package com.lankaautoparts.autosparepro.inventory.controller;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Part> getAllParts() {
        return inventoryService.getAllParts();
    }

    @PostMapping
    public Part addPart(@RequestBody Part part) {
        return inventoryService.savePart(part);
    }
}
