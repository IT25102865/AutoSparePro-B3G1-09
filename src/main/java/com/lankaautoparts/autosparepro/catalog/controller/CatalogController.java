package com.lankaautoparts.autosparepro.catalog.controller;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.promotion.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final InventoryService inventoryService;
    private final PromotionService promotionService;

    public CatalogController(InventoryService inventoryService, PromotionService promotionService) {
        this.inventoryService = inventoryService;
        this.promotionService = promotionService;
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) String category, Model model) {
        List<Part> allParts = inventoryService.getAllParts();
        List<Part> parts = allParts;

        String selected = (category == null || category.isBlank()) ? "ALL" : category.toUpperCase();
        if (!selected.equals("ALL")) {
            parts = parts.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().name().equalsIgnoreCase(selected))
                    .collect(Collectors.toList());
        }

        List<String> brands = allParts.stream()
                .map(Part::getBrand)
                .filter(b -> b != null && !b.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        // Best active discount % for each part, keyed by part id — parts with
        // no matching active promotion (storewide or category-scoped) are
        // simply absent from the map, so the template can check with
        // #maps.containsKey() rather than needing a 0.0 default everywhere.
        Map<Long, Double> discounts = promotionService.getBestDiscountsForParts(parts);

        model.addAttribute("parts", parts);
        model.addAttribute("categories", PartCategory.values());
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("brands", brands);
        model.addAttribute("discounts", discounts);
        return "catalog";
    }
}
