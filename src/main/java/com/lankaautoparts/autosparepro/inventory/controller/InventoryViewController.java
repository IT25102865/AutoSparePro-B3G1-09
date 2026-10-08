package com.lankaautoparts.autosparepro.inventory.controller;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.inventory.service.ImageUrlService;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.supplier.model.Supplier;
import com.lankaautoparts.autosparepro.supplier.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
public class InventoryViewController {
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final ImageUrlService imageUrlService;

    public InventoryViewController(InventoryService inventoryService, SupplierService supplierService,
                                   ImageUrlService imageUrlService) {
        this.inventoryService = inventoryService;
        this.supplierService = supplierService;
        this.imageUrlService = imageUrlService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("parts", inventoryService.getAllParts());
        model.addAttribute("part", new Part());
        model.addAttribute("categories", PartCategory.values());
        addLowStockAttributes(model);
        return "inventory";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("parts", inventoryService.getAllParts());
        model.addAttribute("part", inventoryService.getPartById(id));
        model.addAttribute("categories", PartCategory.values());
        addLowStockAttributes(model);
        return "inventory";
    }

    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("part") Part part, BindingResult bindingResult, Model model) {
        // Resolve the pasted image link (share links, page links, etc.) to a direct image URL.
        ImageUrlService.Result image = imageUrlService.resolve(part.getImageUrl());
        if (image.ok()) {
            part.setImageUrl(image.url());
        } else {
            bindingResult.rejectValue("imageUrl", "imageUrl.invalid", image.error());
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("parts", inventoryService.getAllParts());
            model.addAttribute("categories", PartCategory.values());
            addLowStockAttributes(model);
            return "inventory";
        }
        inventoryService.savePart(part);
        return "redirect:/inventory";
    }

    /** Powers the "Fetch" button on the Image URL field: resolves a link and reports the direct image URL or why it can't be used. */
    @GetMapping("/image-preview")
    @ResponseBody
    public Map<String, Object> imagePreview(@RequestParam String url) {
        ImageUrlService.Result result = imageUrlService.resolve(url);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", result.ok());
        body.put("url", result.url());
        body.put("error", result.error());
        return body;
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        inventoryService.deletePart(id);
        return "redirect:/inventory";
    }

    /**
     * Cross-references parts at or below their reorder level with suppliers
     * on file for that part's category, so low stock isn't just a badge
     * nobody acts on — it surfaces who to actually call to reorder.
     */
    private void addLowStockAttributes(Model model) {
        List<Part> lowStock = inventoryService.getLowStockParts();

        Map<PartCategory, List<Supplier>> suppliersByCategory = new HashMap<>();
        Map<Long, List<Supplier>> lowStockSuppliers = new LinkedHashMap<>();
        for (Part p : lowStock) {
            List<Supplier> suppliers = p.getCategory() == null
                    ? List.of()
                    : suppliersByCategory.computeIfAbsent(p.getCategory(), supplierService::getSuppliersForCategory);
            lowStockSuppliers.put(p.getId(), suppliers);
        }

        model.addAttribute("lowStockParts", lowStock);
        model.addAttribute("lowStockSuppliers", lowStockSuppliers);
    }
}
