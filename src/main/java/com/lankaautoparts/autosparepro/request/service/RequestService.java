package com.lankaautoparts.autosparepro.request.service;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.request.model.PartRequest;
import com.lankaautoparts.autosparepro.request.model.RequestStatus;
import com.lankaautoparts.autosparepro.request.repository.PartRequestRepository;
import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RequestService {

    /**
     * Outcome of an approval attempt, so the controller can show the admin
     * exactly why an approval did or didn't go through instead of silently
     * changing state.
     */
    public enum ApprovalResult {
        APPROVED,
        ALREADY_PROCESSED,
        PART_NOT_FOUND,
        INSUFFICIENT_STOCK
    }

    private final PartRequestRepository requestRepository;
    private final InventoryService inventoryService;
    private final SalesService salesService;

    public RequestService(PartRequestRepository requestRepository, InventoryService inventoryService, SalesService salesService) {
        this.requestRepository = requestRepository;
        this.inventoryService = inventoryService;
        this.salesService = salesService;
    }

    public List<PartRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public PartRequest getRequestById(Long id) {
        return requestRepository.findById(id).orElse(null);
    }

    public List<PartRequest> getRequestsByUsername(String username) {
        return requestRepository.findByUsernameOrderByRequestedDateDesc(username);
    }

    public void createRequest(String partNumber, String username) {
        Optional<Part> match = findPart(partNumber);

        PartRequest request = new PartRequest();
        request.setPartNumber(partNumber);
        request.setPartName(match.map(Part::getPartName).orElse(partNumber));
        request.setUsername(username);
        request.setQuantity(1);
        request.setStatus(RequestStatus.PENDING);
        request.setRequestedDate(LocalDate.now());
        requestRepository.save(request);
    }

    /**
     * Approves a request and creates its (unpaid) invoice, but does NOT touch
     * stock — stock is only committed once the customer actually pays (see
     * {@link #fulfillPayment(Long)}). This also guards against re-processing
     * a request that isn't PENDING anymore (previously calling this twice
     * on the same id would decrement stock and create a duplicate invoice
     * each time), and against approving a request for more units than are
     * currently in stock.
     */
    public ApprovalResult approveRequest(Long id) {
        PartRequest request = requestRepository.findById(id).orElse(null);
        if (request == null) {
            return ApprovalResult.PART_NOT_FOUND;
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            return ApprovalResult.ALREADY_PROCESSED;
        }

        Optional<Part> partOpt = findPart(request.getPartNumber());
        if (partOpt.isEmpty()) {
            return ApprovalResult.PART_NOT_FOUND;
        }
        Part part = partOpt.get();
        if (part.getQuantity() < request.getQuantity()) {
            return ApprovalResult.INSUFFICIENT_STOCK;
        }

        request.setStatus(RequestStatus.APPROVED);
        requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("REQ-" + request.getId());
        invoice.setCustomerName(request.getUsername());
        invoice.setTotalAmount(part.getPrice() * request.getQuantity());
        invoice.setDiscount(0);
        invoice.setPaymentMethod("Pending");
        invoice.setDate(LocalDate.now());
        salesService.saveInvoice(invoice);

        return ApprovalResult.APPROVED;
    }

    /** Returns false (no-op) if the request was already approved/rejected. */
    public boolean rejectRequest(Long id) {
        PartRequest request = requestRepository.findById(id).orElse(null);
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
            return false;
        }
        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);
        return true;
    }

    /**
     * The authoritative amount owed for a request, taken from its invoice —
     * never from a value the client submits.
     */
    public double getInvoiceAmount(Long requestId) {
        Invoice invoice = salesService.findByInvoiceNumber("REQ-" + requestId);
        return invoice != null ? invoice.getTotalAmount() : 0.0;
    }

    /**
     * Commits stock for an approved request at the moment of payment. Stock
     * is re-checked here (not just at approval time) because other requests
     * may have been approved and paid in the meantime; if there isn't enough
     * left, this returns false and the caller must not charge the customer.
     */
    @Transactional
    public boolean fulfillPayment(Long requestId) {
        PartRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null || request.getStatus() != RequestStatus.APPROVED) {
            return false;
        }
        Optional<Part> partOpt = findPart(request.getPartNumber());
        if (partOpt.isEmpty()) {
            return false;
        }
        Part part = partOpt.get();
        if (part.getQuantity() < request.getQuantity()) {
            return false;
        }
        part.setQuantity(part.getQuantity() - request.getQuantity());
        inventoryService.savePart(part);
        return true;
    }

    private Optional<Part> findPart(String partNumber) {
        return inventoryService.findByPartNumber(partNumber);
    }
}
