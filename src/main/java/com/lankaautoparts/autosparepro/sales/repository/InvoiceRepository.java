package com.lankaautoparts.autosparepro.sales.repository;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
