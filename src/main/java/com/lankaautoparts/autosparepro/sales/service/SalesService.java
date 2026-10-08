package com.lankaautoparts.autosparepro.sales.service;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.model.InvoiceItem;
import com.lankaautoparts.autosparepro.sales.repository.InvoiceItemRepository;
import com.lankaautoparts.autosparepro.sales.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalesService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    public SalesService(InvoiceRepository invoiceRepository, InvoiceItemRepository invoiceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id).orElse(new Invoice());
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    /**
     * Finds an invoice by its invoiceNumber (e.g. "REQ-42"). Used by the
     * payment flow to link a payment back to the invoice that was created
     * when the request was approved (or when a cart checkout completed).
     */
    public Invoice findByInvoiceNumber(String invoiceNumber) {
        return invoiceNumber == null ? null : invoiceRepository.findFirstByInvoiceNumber(invoiceNumber).orElse(null);
    }

    public InvoiceItem saveInvoiceItem(InvoiceItem item) {
        return invoiceItemRepository.save(item);
    }

    /** The itemized lines for one invoice/quotation, in the order they were added. */
    public List<InvoiceItem> getItemsForInvoice(Long invoiceId) {
        return invoiceItemRepository.findByInvoiceIdOrderByIdAsc(invoiceId);
    }

    /** Every part a given customer has ever actually purchased, newest first. */
    public List<InvoiceItem> getPurchasedItemsForCustomer(String username) {
        return invoiceItemRepository.findByInvoice_CustomerNameOrderByIdDesc(username);
    }
}
