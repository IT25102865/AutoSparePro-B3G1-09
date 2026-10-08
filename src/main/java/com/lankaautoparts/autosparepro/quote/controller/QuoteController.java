package com.lankaautoparts.autosparepro.quote.controller;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.quote.QuoteCart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Print-only quotation builder for customers browsing the catalog. Entirely
 * separate from the real purchase flow (Request -> Admin Approves -> Pay):
 * nothing added here is submitted, saved to the database, or seen by an
 * admin. It's just a running list the customer can print for their own
 * reference.
 */
@Controller
@RequestMapping("/quote")
public class QuoteController {

    private final QuoteCart quoteCart;
    private final InventoryService inventoryService;

    public QuoteController(QuoteCart quoteCart, InventoryService inventoryService) {
        this.quoteCart = quoteCart;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String view(Model model) {
        model.addAttribute("lines", quoteCart.getLines());
        model.addAttribute("total", quoteCart.getTotal());
        model.addAttribute("today", LocalDate.now());
        return "quote";
    }

    @PostMapping("/add")
    public String add(@RequestParam String partNumber,
                       @RequestParam(defaultValue = "1") int quantity,
                       RedirectAttributes redirectAttributes) {
        Optional<Part> match = findPart(partNumber);
        if (match.isEmpty()) {
            redirectAttributes.addFlashAttribute("quoteError", "That part could not be found.");
            return "redirect:/catalog";
        }
        Part part = match.get();
        quoteCart.addItem(part.getPartNumber(), part.getPartName(), part.getPrice(), quantity);
        redirectAttributes.addFlashAttribute("quoteSuccess", "Added \"" + part.getPartName() + "\" to your quotation.");
        return "redirect:/catalog";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam String partNumber) {
        quoteCart.removeItem(partNumber);
        return "redirect:/quote";
    }

    @PostMapping("/clear")
    public String clear() {
        quoteCart.clear();
        return "redirect:/quote";
    }

    private Optional<Part> findPart(String partNumber) {
        List<Part> parts = inventoryService.getAllParts();
        return parts.stream()
                .filter(p -> p.getPartNumber() != null && p.getPartNumber().equalsIgnoreCase(partNumber))
                .findFirst();
    }
}
