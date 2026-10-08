package com.lankaautoparts.autosparepro.supplier.controller;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.supplier.model.Supplier;
import com.lankaautoparts.autosparepro.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierViewController {
    private final SupplierService supplierService;

    public SupplierViewController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("categories", PartCategory.values());
        return "supplier";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        model.addAttribute("categories", PartCategory.values());
        return "supplier";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("supplier") Supplier supplier, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getAllSuppliers());
            model.addAttribute("categories", PartCategory.values());
            return "supplier";
        }
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }
}
