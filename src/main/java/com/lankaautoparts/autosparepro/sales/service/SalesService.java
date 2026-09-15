package com.lankaautoparts.autosparepro.sales.service;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SalesService {
    private final InvoiceRepository invoiceRepository;

    public SalesService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
}
