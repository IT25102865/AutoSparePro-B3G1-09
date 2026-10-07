package com.lankaautoparts.autosparepro.damagedstock.controller;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import com.lankaautoparts.autosparepro.damagedstock.service.DamagedStockService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/damaged-stock")
public class DamagedStockViewController {
    private final DamagedStockService damagedStockService;

    public DamagedStockViewController(DamagedStockService damagedStockService) {
        this.damagedStockService = damagedStockService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", damagedStockService.getAllDamagedItems());
        model.addAttribute("item", new DamagedItem());
        return "damagedstock";
    }

    /** Logs damage found in the warehouse, not tied to any customer claim. */
    @PostMapping("/log-internal")
    public String logInternal(@Valid @ModelAttribute("item") DamagedItem item, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", damagedStockService.getAllDamagedItems());
            return "damagedstock";
        }
        damagedStockService.logInternal(item);
        return "redirect:/damaged-stock";
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applyResult(damagedStockService.verify(id), redirectAttributes);
        return "redirect:/damaged-stock";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applyResult(damagedStockService.reject(id), redirectAttributes);
        return "redirect:/damaged-stock";
    }

    @PostMapping("/{id}/refund")
    public String refund(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applyResult(damagedStockService.refund(id), redirectAttributes);
        return "redirect:/damaged-stock";
    }

    @PostMapping("/{id}/replace")
    public String replace(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applyResult(damagedStockService.replace(id), redirectAttributes);
        return "redirect:/damaged-stock";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        damagedStockService.deleteDamagedItem(id);
        return "redirect:/damaged-stock";
    }

    private void applyResult(String errorMessage, RedirectAttributes redirectAttributes) {
        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } else {
            redirectAttributes.addFlashAttribute("success", "Updated.");
        }
    }
}
