package com.lankaautoparts.autosparepro.request.controller;

import com.lankaautoparts.autosparepro.payment.service.PaymentService;
import com.lankaautoparts.autosparepro.request.model.PartRequest;
import com.lankaautoparts.autosparepro.request.service.RequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class RequestController {

    private final RequestService requestService;
    private final PaymentService paymentService;

    public RequestController(RequestService requestService, PaymentService paymentService) {
        this.requestService = requestService;
        this.paymentService = paymentService;
    }

    @PostMapping("/catalog/request")
    public String requestPart(@RequestParam String partNumber, Principal principal) {
        requestService.createRequest(partNumber, principal.getName());
        return "redirect:/catalog?requested=true";
    }

    @GetMapping("/my-requests")
    public String myRequests(Model model, Principal principal) {
        List<PartRequest> requests = requestService.getRequestsByUsername(principal.getName());

        // Maps requestId -> the id of its COMPLETED payment (if any), so the
        // page can link straight to that payment's receipt.
        Map<Long, Long> paidPaymentIds = new HashMap<>();
        Set<Long> refundedRequestIds = new HashSet<>();
        for (PartRequest r : requests) {
            paymentService.getCompletedPaymentIdForRequest(r.getId()).ifPresent(pid -> paidPaymentIds.put(r.getId(), pid));
            if (paymentService.isRequestRefunded(r.getId())) {
                refundedRequestIds.add(r.getId());
            }
        }

        model.addAttribute("requests", requests);
        model.addAttribute("paidPaymentIds", paidPaymentIds);
        model.addAttribute("refundedRequestIds", refundedRequestIds);
        return "my-requests";
    }

    @GetMapping("/admin/requests")
    public String list(Model model) {
        model.addAttribute("requests", requestService.getAllRequests());
        return "requests";
    }

    @PostMapping("/admin/requests/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        RequestService.ApprovalResult result = requestService.approveRequest(id);
        switch (result) {
            case APPROVED -> redirectAttributes.addFlashAttribute("success", "Request approved.");
            case ALREADY_PROCESSED -> redirectAttributes.addFlashAttribute("error", "This request was already processed.");
            case PART_NOT_FOUND -> redirectAttributes.addFlashAttribute("error", "Cannot approve — the requested part no longer exists.");
            case INSUFFICIENT_STOCK -> redirectAttributes.addFlashAttribute("error", "Cannot approve — not enough stock is available for this request.");
        }
        return "redirect:/admin/requests";
    }

    @PostMapping("/admin/requests/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean rejected = requestService.rejectRequest(id);
        if (!rejected) {
            redirectAttributes.addFlashAttribute("error", "This request was already processed.");
        }
        return "redirect:/admin/requests";
    }
}
