package com.lankaautoparts.autosparepro.sales.controller;

import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {
    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public List<Invoice> getAllInvoices() {
        return salesService.getAllInvoices();
    }

    @PostMapping
    public Invoice addInvoice(@RequestBody Invoice invoice) {
        return salesService.saveInvoice(invoice);
    }
}
