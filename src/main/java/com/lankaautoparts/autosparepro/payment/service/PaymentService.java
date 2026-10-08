package com.lankaautoparts.autosparepro.payment.service;

import com.lankaautoparts.autosparepro.finance.service.FinanceService;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.payment.model.CardType;
import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.model.PaymentStatus;
import com.lankaautoparts.autosparepro.payment.repository.PaymentRepository;
import com.lankaautoparts.autosparepro.request.model.PartRequest;
import com.lankaautoparts.autosparepro.request.service.RequestService;
import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.model.InvoiceItem;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Handles payment processing for approved requests and cart checkouts —
 * records the card's last 4 digits and cardholder name against the
 * request/invoice. Full card numbers and CVV codes are read from the form
 * but are NEVER persisted.
 *
 * Payments are a financial record, so they are never deleted and their
 * details are never edited. The only changes after the fact are a status
 * correction ({@link #updateStatus}) and a refund ({@link #refund}).
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesService salesService;
    private final FinanceService financeService;
    private final InventoryService inventoryService;
    private final RequestService requestService;

    public PaymentService(PaymentRepository paymentRepository, SalesService salesService, FinanceService financeService,
                          InventoryService inventoryService, RequestService requestService) {
        this.paymentRepository = paymentRepository;
        this.salesService = salesService;
        this.financeService = financeService;
        this.inventoryService = inventoryService;
        this.requestService = requestService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /** True only if this request has at least one COMPLETED payment against it. */
    public boolean isRequestPaid(Long requestId) {
        return hasPaymentWithStatus(requestId, PaymentStatus.COMPLETED);
    }

    /** True if this request's payment was refunded — it must not be payable a second time. */
    public boolean isRequestRefunded(Long requestId) {
        return hasPaymentWithStatus(requestId, PaymentStatus.REFUNDED);
    }

    private boolean hasPaymentWithStatus(Long requestId, PaymentStatus status) {
        if (requestId == null) return false;
        return paymentRepository.findByRequestId(requestId).stream()
                .anyMatch(p -> p.getStatus() == status);
    }

    /** The id of the (first) COMPLETED payment for this request, if any — used to link to the receipt. */
    public Optional<Long> getCompletedPaymentIdForRequest(Long requestId) {
        if (requestId == null) return Optional.empty();
        return paymentRepository.findByRequestId(requestId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getId)
                .findFirst();
    }

    public Payment makePayment(Long requestId, String invoiceNumber, String cardHolderName,
                                String cardNumber, double amount, String username,
                                CardType cardType, String paymentMethod) {
        String digitsOnly = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        String last4 = digitsOnly.length() >= 4
                ? digitsOnly.substring(digitsOnly.length() - 4)
                : digitsOnly;

        Payment payment = new Payment();
        payment.setRequestId(requestId);
        payment.setInvoiceNumber(invoiceNumber);
        payment.setCardHolderName(cardHolderName);
        payment.setCardLast4(last4);
        payment.setAmount(amount);
        payment.setUsername(username);
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCardType(cardType);
        payment.setPaymentMethod(paymentMethod);
        Payment saved = paymentRepository.save(payment);

        applyToInvoice(saved);
        financeService.recordFromPayment(saved);
        return saved;
    }

    /**
     * Admin status correction (PENDING / COMPLETED / FAILED). Refunds are not
     * a plain status change — they also write a Finance entry and return
     * stock — so they only happen through {@link #refund}, and a refunded
     * payment is final.
     *
     * @return null on success, or a human-readable reason it couldn't be done
     */
    @Transactional
    public String updateStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id).orElse(null);
        if (payment == null) return "Payment not found.";
        if (payment.getStatus() == PaymentStatus.REFUNDED) return "Refunded payments are final and can't be changed.";
        if (status == PaymentStatus.REFUNDED) return "Use the Refund button to refund a payment — it also records the refund in Finance.";
        if (payment.getStatus() == status) return null;

        payment.setStatus(status);
        Payment saved = paymentRepository.save(payment);
        applyToInvoice(saved);
        financeService.recordFromPayment(saved);
        return null;
    }

    /**
     * Refunds a COMPLETED payment in one transaction: marks it REFUNDED,
     * updates the linked invoice, writes the negative Finance entry and —
     * if {@code restock} — returns the purchased quantities to inventory
     * (not wanted when the goods are being refunded because they arrived
     * damaged).
     *
     * @return null on success, or a human-readable reason it couldn't be done
     */
    @Transactional
    public String refund(Long id, boolean restock) {
        Payment payment = paymentRepository.findById(id).orElse(null);
        if (payment == null) return "Payment not found.";
        if (payment.getStatus() == PaymentStatus.REFUNDED) return "This payment has already been refunded.";
        if (payment.getStatus() != PaymentStatus.COMPLETED) return "Only completed payments can be refunded.";

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);
        applyToInvoice(saved);
        financeService.recordRefund(saved);
        if (restock) {
            restockPurchasedItems(saved);
        }
        return null;
    }

    public double getTotalRevenue() {
        return paymentRepository.sumAmountByStatus(PaymentStatus.COMPLETED);
    }

    public long countByStatus(PaymentStatus status) {
        return paymentRepository.countByStatus(status);
    }

    /** Puts what this payment bought back on the shelf. */
    private void restockPurchasedItems(Payment payment) {
        if (payment.getRequestId() != null) {
            PartRequest request = requestService.getRequestById(payment.getRequestId());
            if (request != null) {
                addStock(request.getPartNumber(), request.getQuantity());
            }
            return;
        }
        Invoice invoice = salesService.findByInvoiceNumber(payment.getInvoiceNumber());
        if (invoice == null) return;
        for (InvoiceItem item : salesService.getItemsForInvoice(invoice.getId())) {
            addStock(item.getPartNumber(), item.getQuantity());
        }
    }

    private void addStock(String partNumber, int quantity) {
        inventoryService.findByPartNumber(partNumber).ifPresent(part -> {
            part.setQuantity(part.getQuantity() + quantity);
            inventoryService.savePart(part);
        });
    }

    /** Reflects this payment's current status back onto the linked invoice's paymentMethod label. */
    private void applyToInvoice(Payment payment) {
        Invoice invoice = salesService.findByInvoiceNumber(payment.getInvoiceNumber());
        if (invoice == null) return;

        switch (payment.getStatus()) {
            case COMPLETED -> invoice.setPaymentMethod(
                    "Paid (" + payment.getCardType() + " **** " + payment.getCardLast4() + ")");
            case FAILED -> invoice.setPaymentMethod("Payment failed");
            case PENDING -> invoice.setPaymentMethod("Pending");
            case REFUNDED -> invoice.setPaymentMethod("Refunded");
        }
        salesService.saveInvoice(invoice);
    }
}
