package com.lankaautoparts.autosparepro.finance.service;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import com.lankaautoparts.autosparepro.finance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FinanceService {
    private final TransactionRepository transactionRepository;

    public FinanceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}
