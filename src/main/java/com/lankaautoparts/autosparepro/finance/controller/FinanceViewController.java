package com.lankaautoparts.autosparepro.finance.controller;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import com.lankaautoparts.autosparepro.finance.service.FinanceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/finance")
public class FinanceViewController {
    private final FinanceService financeService;

    public FinanceViewController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("transactions", financeService.getAllTransactions());
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("netTotal", financeService.getNetTotal());
        model.addAttribute("autoCount", financeService.countBySource("AUTO"));
        model.addAttribute("manualCount", financeService.countBySource("MANUAL"));
        return "finance";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("transactions", financeService.getAllTransactions());
        model.addAttribute("transaction", financeService.getTransactionById(id));
        model.addAttribute("netTotal", financeService.getNetTotal());
        model.addAttribute("autoCount", financeService.countBySource("AUTO"));
        model.addAttribute("manualCount", financeService.countBySource("MANUAL"));
        return "finance";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("transaction") Transaction transaction, BindingResult bindingResult, Model model) {
        // Checked here (not as an entity constraint) so that legacy rows can still be re-saved elsewhere.
        if (transaction.getDate() != null && transaction.getDate().isAfter(LocalDate.now())) {
            bindingResult.rejectValue("date", "date.future", "Transaction date can't be in the future.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("transactions", financeService.getAllTransactions());
            model.addAttribute("netTotal", financeService.getNetTotal());
            model.addAttribute("autoCount", financeService.countBySource("AUTO"));
            model.addAttribute("manualCount", financeService.countBySource("MANUAL"));
            return "finance";
        }
        // A transaction created/edited through this admin form is always
        // treated as MANUAL — its "source" is never taken from client input,
        // and editing an existing AUTO-recorded transaction keeps its
        // original source rather than letting the edit silently reclassify it.
        if (transaction.getId() != null) {
            Transaction existing = financeService.getTransactionById(transaction.getId());
            transaction.setSource(existing.getSource() != null ? existing.getSource() : "MANUAL");
        } else {
            transaction.setSource("MANUAL");
        }
        financeService.saveTransaction(transaction);
        return "redirect:/finance";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        financeService.deleteTransaction(id);
        return "redirect:/finance";
    }
}
