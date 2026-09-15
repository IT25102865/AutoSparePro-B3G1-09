package com.lankaautoparts.autosparepro.finance.controller;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import com.lankaautoparts.autosparepro.finance.service.FinanceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return financeService.getAllTransactions();
    }

    @PostMapping
    public Transaction addTransaction(@RequestBody Transaction transaction) {
        return financeService.saveTransaction(transaction);
    }
}
