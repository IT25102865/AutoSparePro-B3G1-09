package com.lankaautoparts.autosparepro.finance.repository;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
