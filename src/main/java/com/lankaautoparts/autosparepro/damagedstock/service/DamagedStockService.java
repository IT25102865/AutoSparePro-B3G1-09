package com.lankaautoparts.autosparepro.damagedstock.service;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItemStatus;
import com.lankaautoparts.autosparepro.damagedstock.repository.DamagedItemRepository;
import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.payment.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Previously, admins could freely rewrite any field of a damaged-item
 * record — including a customer's claim — with no workflow at all. Now a
 * customer-filed claim moves through REPORTED -> VERIFIED -> (REFUNDED or
 * REPLACED or REJECTED), and only status-transition actions are exposed;
 * the original free-edit path is gone.
 */
@Service
public class DamagedStockService {
    private final DamagedItemRepository damagedItemRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    public DamagedStockService(DamagedItemRepository damagedItemRepository,
                                PaymentService paymentService,
                                InventoryService inventoryService) {
        this.damagedItemRepository = damagedItemRepository;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }

    public List<DamagedItem> getAllDamagedItems() {
        return damagedItemRepository.findAll();
    }

    public DamagedItem getDamagedItemById(Long id) {
        return damagedItemRepository.findById(id).orElse(new DamagedItem());
    }

    public DamagedItem saveDamagedItem(DamagedItem item) {
        return damagedItemRepository.save(item);
    }

    public void deleteDamagedItem(Long id) {
        damagedItemRepository.deleteById(id);
    }

    /** Newest claim (if any) filed against a given payment, used to gate reporting and duplicate claims. */
    public DamagedItem findLatestClaimForPayment(Long paymentId) {
        if (paymentId == null) return null;
        return damagedItemRepository.findFirstByPaymentIdOrderByIdDesc(paymentId).orElse(null);
    }

    /**
     * Newest claim (if any) filed against a specific part within a given
     * payment. A cart checkout can pay for several different parts in one
     * payment, so {@link #findLatestClaimForPayment} alone can't tell which
     * part a claim was actually about — this scopes the lookup to a single
     * line of that purchase instead.
     */
    public DamagedItem findLatestClaimForPurchase(Long paymentId, String partNumber) {
        if (paymentId == null || partNumber == null) return null;
        return damagedItemRepository.findFirstByPaymentIdAndPartNumberOrderByIdDesc(paymentId, partNumber).orElse(null);
    }

    /** Filed by a customer from the My Purchases page, against a specific payment. */
    public DamagedItem reportFromPurchase(String partNumber, int quantity, String reason, String reportedBy, Long paymentId) {
        DamagedItem item = new DamagedItem();
        item.setPartNumber(partNumber);
        item.setQuantity(quantity <= 0 ? 1 : quantity);
        item.setDateLogged(LocalDate.now());
        item.setReason((reason == null || reason.isBlank()) ? "Reported by customer" : reason);
        item.setReportedBy(reportedBy);
        item.setPaymentId(paymentId);
        item.setStatus(DamagedItemStatus.REPORTED);
        return damagedItemRepository.save(item);
    }

    /** Staff logging damage found in the warehouse — not tied to any customer purchase, so it's pre-verified. */
    public DamagedItem logInternal(DamagedItem item) {
        item.setId(null);
        item.setDateLogged(item.getDateLogged() != null ? item.getDateLogged() : LocalDate.now());
        item.setStatus(DamagedItemStatus.VERIFIED);
        item.setPaymentId(null);
        if (item.getReportedBy() == null || item.getReportedBy().isBlank()) {
            item.setReportedBy("Internal");
        }
        return damagedItemRepository.save(item);
    }

    /** Returns null on success, or a human-readable reason the action couldn't be done. */
    public String verify(Long id) {
        DamagedItem item = damagedItemRepository.findById(id).orElse(null);
        if (item == null) return "Item not found.";
        if (item.getStatus() != DamagedItemStatus.REPORTED) return "Only newly reported claims can be verified.";
        item.setStatus(DamagedItemStatus.VERIFIED);
        damagedItemRepository.save(item);
        return null;
    }

    public String reject(Long id) {
        DamagedItem item = damagedItemRepository.findById(id).orElse(null);
        if (item == null) return "Item not found.";
        if (item.getStatus() != DamagedItemStatus.REPORTED && item.getStatus() != DamagedItemStatus.VERIFIED) {
            return "This claim has already been finalized.";
        }
        item.setStatus(DamagedItemStatus.REJECTED);
        damagedItemRepository.save(item);
        return null;
    }

    @Transactional
    public String refund(Long id) {
        DamagedItem item = damagedItemRepository.findById(id).orElse(null);
        if (item == null) return "Item not found.";
        if (item.getStatus() != DamagedItemStatus.VERIFIED) return "Only verified claims can be refunded.";
        if (item.getPaymentId() == null) return "This is an internal stock log with no linked payment to refund.";

        // Same refund as the admin Payments page (status, invoice label and
        // Finance entry), except the goods are damaged so they are NOT
        // returned to sellable stock.
        String error = paymentService.refund(item.getPaymentId(), false);
        if (error != null) return error;

        item.setStatus(DamagedItemStatus.REFUNDED);
        damagedItemRepository.save(item);
        return null;
    }

    @Transactional
    public String replace(Long id) {
        DamagedItem item = damagedItemRepository.findById(id).orElse(null);
        if (item == null) return "Item not found.";
        if (item.getStatus() != DamagedItemStatus.VERIFIED) return "Only verified claims can be issued a replacement.";

        Optional<Part> partOpt = inventoryService.findByPartNumber(item.getPartNumber());
        if (partOpt.isEmpty()) return "The part for this claim no longer exists in inventory.";

        Part part = partOpt.get();
        if (part.getQuantity() < item.getQuantity()) return "Not enough stock on hand to issue a replacement.";

        part.setQuantity(part.getQuantity() - item.getQuantity());
        inventoryService.savePart(part);

        item.setStatus(DamagedItemStatus.REPLACED);
        damagedItemRepository.save(item);
        return null;
    }
}
