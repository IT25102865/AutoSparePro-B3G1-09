package com.lankaautoparts.autosparepro.sales.controller;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/sales")
public class SalesViewController {
    private final SalesService salesService;

    public SalesViewController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", salesService.getAllInvoices());
        model.addAttribute("invoice", new Invoice());
        return "sales";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("invoices", salesService.getAllInvoices());
        model.addAttribute("invoice", salesService.getInvoiceById(id));
        return "sales";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("invoice") Invoice invoice, BindingResult bindingResult, Model model) {
        // Checked here (not as an entity constraint) so that legacy rows can still be re-saved elsewhere.
        if (invoice.getDate() != null && invoice.getDate().isAfter(LocalDate.now())) {
            bindingResult.rejectValue("date", "date.future", "Invoice date can't be in the future.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("invoices", salesService.getAllInvoices());
            return "sales";
        }
        salesService.saveInvoice(invoice);
        return "redirect:/sales";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        salesService.deleteInvoice(id);
        return "redirect:/sales";
    }
}
