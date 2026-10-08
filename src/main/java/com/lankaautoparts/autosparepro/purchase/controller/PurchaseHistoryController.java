package com.lankaautoparts.autosparepro.purchase.controller;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import com.lankaautoparts.autosparepro.damagedstock.service.DamagedStockService;
import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.model.PaymentStatus;
import com.lankaautoparts.autosparepro.payment.service.PaymentService;
import com.lankaautoparts.autosparepro.purchase.model.PurchaseItem;
import com.lankaautoparts.autosparepro.request.model.PartRequest;
import com.lankaautoparts.autosparepro.request.service.RequestService;
import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.model.InvoiceItem;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lets a customer see what they've actually bought, and report damage only
 * against a real purchase — replacing the old catalog "Report Damage"
 * button, which could be clicked for any part in the catalog whether or not
 * the customer had ever bought it.
 *
 * A purchase can come from either of two independent flows: the admin-approval
 * Request -> Approve -> Pay flow (one part per payment, tracked via
 * {@code Payment.requestId} -> {@link PartRequest}), or a cart checkout (any
 * number of parts in one payment, {@code Payment.requestId} is null, and the
 * parts are recorded as {@link InvoiceItem} rows on the Invoice the payment
 * paid for instead). Both are shown here as one combined purchase history.
 */
@Controller
public class PurchaseHistoryController {

    private final PaymentService paymentService;
    private final RequestService requestService;
    private final DamagedStockService damagedStockService;
    private final SalesService salesService;

    public PurchaseHistoryController(PaymentService paymentService, RequestService requestService,
                                      DamagedStockService damagedStockService, SalesService salesService) {
        this.paymentService = paymentService;
        this.requestService = requestService;
        this.damagedStockService = damagedStockService;
        this.salesService = salesService;
    }

    @GetMapping("/my-purchases")
    public String myPurchases(Model model, Principal principal) {
        List<Payment> payments = paymentService.getAllPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED || p.getStatus() == PaymentStatus.REFUNDED)
                .filter(p -> p.getUsername() != null && p.getUsername().equals(principal.getName()))
                .sorted(Comparator.comparing(Payment::getPaymentDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        List<PurchaseItem> purchases = new ArrayList<>();
        for (Payment p : payments) {
            if (p.getRequestId() != null) {
                // Request-approval flow: exactly one part per payment.
                PartRequest request = requestService.getRequestById(p.getRequestId());
                if (request == null) continue;
                purchases.add(toPurchaseItem(p, request.getPartNumber(), request.getPartName(),
                        request.getQuantity(), p.getAmount()));
            } else {
                // Cart checkout: any number of parts per payment, recorded as InvoiceItems.
                Invoice invoice = salesService.findByInvoiceNumber(p.getInvoiceNumber());
                if (invoice == null || invoice.getId() == null) continue;
                for (InvoiceItem line : salesService.getItemsForInvoice(invoice.getId())) {
                    purchases.add(toPurchaseItem(p, line.getPartNumber(), line.getPartName(),
                            line.getQuantity(), line.getSubtotal()));
                }
            }
        }

        model.addAttribute("purchases", purchases);
        return "my-purchases";
    }

    private PurchaseItem toPurchaseItem(Payment payment, String partNumber, String partName, int quantity, double amount) {
        PurchaseItem item = new PurchaseItem();
        item.setPaymentId(payment.getId());
        item.setPartNumber(partNumber);
        item.setPartName(partName);
        item.setQuantity(quantity);
        item.setAmount(amount);
        item.setPurchaseDate(payment.getPaymentDate());

        DamagedItem claim = damagedStockService.findLatestClaimForPurchase(payment.getId(), partNumber);
        item.setAlreadyReported(claim != null);
        item.setClaimStatus(claim != null && claim.getStatus() != null ? claim.getStatus().name() : null);
        return item;
    }

    @PostMapping("/my-purchases/report-damage")
    public String reportDamage(@RequestParam Long paymentId,
                                @RequestParam String partNumber,
                                @RequestParam(defaultValue = "1") int quantity,
                                @RequestParam(required = false) String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        Payment payment = paymentService.getPaymentById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED
                || payment.getUsername() == null || !payment.getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "That purchase could not be found.");
            return "redirect:/my-purchases";
        }

        // Confirm partNumber genuinely belongs to this payment (and find how
        // many units of it were actually bought, to cap the claim quantity),
        // whichever flow this payment came from.
        Integer purchasedQuantity = null;
        if (payment.getRequestId() != null) {
            PartRequest request = requestService.getRequestById(payment.getRequestId());
            if (request != null && partNumber.equals(request.getPartNumber())) {
                purchasedQuantity = request.getQuantity();
            }
        } else {
            Invoice invoice = salesService.findByInvoiceNumber(payment.getInvoiceNumber());
            if (invoice != null && invoice.getId() != null) {
                purchasedQuantity = salesService.getItemsForInvoice(invoice.getId()).stream()
                        .filter(line -> partNumber.equals(line.getPartNumber()))
                        .map(InvoiceItem::getQuantity)
                        .findFirst()
                        .orElse(null);
            }
        }

        if (purchasedQuantity == null) {
            redirectAttributes.addFlashAttribute("error", "That purchase could not be found.");
            return "redirect:/my-purchases";
        }

        if (damagedStockService.findLatestClaimForPurchase(paymentId, partNumber) != null) {
            redirectAttributes.addFlashAttribute("error", "A damage report has already been filed for this item.");
            return "redirect:/my-purchases";
        }

        int qty = Math.max(1, Math.min(quantity, purchasedQuantity));
        damagedStockService.reportFromPurchase(partNumber, qty,
                reason, principal.getName(), paymentId);

        redirectAttributes.addFlashAttribute("success", "Thanks — your damage report has been submitted for review.");
        return "redirect:/my-purchases?reported=true";
    }
}
