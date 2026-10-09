package com.lankaautoparts.autosparepro.finance.repository;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    long countBySource(String source);

    boolean existsBySourceAndInvoiceReference(String source, String invoiceReference);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t")
    double sumAmount();
}
