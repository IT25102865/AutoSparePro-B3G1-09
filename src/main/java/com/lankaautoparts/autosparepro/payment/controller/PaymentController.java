package com.lankaautoparts.autosparepro.payment.controller;

import com.lankaautoparts.autosparepro.payment.model.CardType;
import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.model.PaymentStatus;
import com.lankaautoparts.autosparepro.payment.service.PaymentService;
import com.lankaautoparts.autosparepro.request.model.PartRequest;
import com.lankaautoparts.autosparepro.request.model.RequestStatus;
import com.lankaautoparts.autosparepro.request.service.RequestService;
import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class PaymentController {

    /** Statuses an admin can set by hand. REFUNDED is deliberately absent: refunds go through the Refund button. */
    private static final List<PaymentStatus> EDITABLE_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED, PaymentStatus.FAILED);

    private final PaymentService paymentService;
    private final RequestService requestService;
    private final SalesService salesService;

    public PaymentController(PaymentService paymentService, RequestService requestService, SalesService salesService) {
        this.paymentService = paymentService;
        this.requestService = requestService;
        this.salesService = salesService;
    }

    // ─── Customer: pay for an approved request ─────────────────────────

    @GetMapping("/payments/pay/{requestId}")
    public String payForm(@PathVariable Long requestId, Model model, Authentication authentication) {
        PartRequest request = requestService.getRequestById(requestId);
        if (!isPayable(request, authentication)) {
            return "redirect:/my-requests";
        }

        Invoice invoice = salesService.findByInvoiceNumber("REQ-" + requestId);
        model.addAttribute("request", request);
        model.addAttribute("amount", invoice != null ? invoice.getTotalAmount() : 0.0);
        model.addAttribute("cardTypes", CardType.values());
        return "pay";
    }

    /**
     * Serializes the whole pay-for-a-request flow (payable check, stock
     * commit, payment creation) so that two near-simultaneous submissions
     * for the same request — a double-click, or two open tabs — can't both
     * pass the "not already paid" check before either has actually saved a
     * payment. Without this, both could slip through and double-decrement
     * stock / double-charge the same request.
     */
    private static final Object PAY_LOCK = new Object();

    @PostMapping("/payments/pay/{requestId}")
    public String pay(@PathVariable Long requestId,
                       @RequestParam String cardHolderName,
                       @RequestParam String cardNumber,
                       @RequestParam CardType cardType,
                       @RequestParam String paymentMethod,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        synchronized (PAY_LOCK) {
            PartRequest request = requestService.getRequestById(requestId);
            if (!isPayable(request, authentication)) {
                return "redirect:/my-requests";
            }

            // The amount charged is always recomputed from the request's own
            // invoice, server-side — never taken from a client-submitted form
            // field, which could otherwise be edited to pay any amount at all.
            double amount = requestService.getInvoiceAmount(requestId);

            // Stock is only committed here, at the moment of payment (not back
            // when the request was approved), and re-checked in case it's no
            // longer sufficient — e.g. another approved request was paid first.
            boolean stockOk = requestService.fulfillPayment(requestId);
            if (!stockOk) {
                redirectAttributes.addFlashAttribute("error",
                        "Sorry — stock for this item has changed and there isn't enough left to fulfill your order. Please contact support.");
                return "redirect:/my-requests";
            }

            Payment saved = paymentService.makePayment(requestId, "REQ-" + requestId, cardHolderName,
                    cardNumber, amount, authentication.getName(), cardType, paymentMethod);
            return "redirect:/payments/" + saved.getId() + "?success=true";
        }
    }

    // ─── Receipt (owner or admin) ───────────────────────────────────────

    @GetMapping("/payments/{id}")
    public String receipt(@PathVariable Long id, Model model, Authentication authentication) {
        Payment payment = paymentService.getPaymentById(id).orElse(null);
        if (payment != null && !hasAdminRole(authentication) && !payment.getUsername().equals(authentication.getName())) {
            payment = null;
        }
        model.addAttribute("payment", payment);
        return "payment-receipt";
    }

    // ─── Admin: list, stats, edit, delete ───────────────────────────────

    @GetMapping("/admin/payments")
    public String adminList(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        model.addAttribute("statuses", EDITABLE_STATUSES);
        model.addAttribute("totalRevenue", paymentService.getTotalRevenue());
        model.addAttribute("completedCount", paymentService.countByStatus(PaymentStatus.COMPLETED));
        model.addAttribute("pendingCount", paymentService.countByStatus(PaymentStatus.PENDING));
        model.addAttribute("failedCount", paymentService.countByStatus(PaymentStatus.FAILED));
        model.addAttribute("refundedCount", paymentService.countByStatus(PaymentStatus.REFUNDED));
        return "payments";
    }

    /**
     * The only thing an admin can change on a recorded payment is its status
     * (e.g. COMPLETED -> REFUNDED). Amount, card and cardholder details are a
     * record of what the customer actually paid and are deliberately not
     * editable — this endpoint accepts nothing but the new status.
     */
    @PostMapping("/admin/payments/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam PaymentStatus status,
                               RedirectAttributes redirectAttributes) {
        applyResult(paymentService.updateStatus(id, status), "Payment status updated.", redirectAttributes);
        return "redirect:/admin/payments";
    }

    /**
     * Payments are never deleted — a financial record has to stay on file.
     * The way to reverse one is a refund: the payment is marked REFUNDED,
     * a negative entry is booked in Finance, and the purchased items go
     * back into stock.
     */
    @PostMapping("/admin/payments/refund/{id}")
    public String refund(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applyResult(paymentService.refund(id, true), "Payment refunded and the items were returned to stock.", redirectAttributes);
        return "redirect:/admin/payments";
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private void applyResult(String errorMessage, String successMessage, RedirectAttributes redirectAttributes) {
        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } else {
            redirectAttributes.addFlashAttribute("success", successMessage);
        }
    }

    private boolean isPayable(PartRequest request, Authentication authentication) {
        return request != null
                && request.getUsername() != null
                && request.getUsername().equals(authentication.getName())
                && request.getStatus() == RequestStatus.APPROVED
                && !paymentService.isRequestPaid(request.getId())
                && !paymentService.isRequestRefunded(request.getId());
    }

    private boolean hasAdminRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
