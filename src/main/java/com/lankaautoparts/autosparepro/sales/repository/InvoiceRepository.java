package com.lankaautoparts.autosparepro.sales.repository;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findFirstByInvoiceNumber(String invoiceNumber);
}
