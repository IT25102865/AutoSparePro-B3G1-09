package com.lankaautoparts.autosparepro.damagedstock.controller;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItemStatus;
import com.lankaautoparts.autosparepro.damagedstock.service.DamagedStockService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/damaged-stock")
public class DamagedStockController {
    private final DamagedStockService damagedStockService;

    public DamagedStockController(DamagedStockService damagedStockService) {
        this.damagedStockService = damagedStockService;
    }

    @GetMapping
    public List<DamagedItem> getAllDamagedItems() {
        return damagedStockService.getAllDamagedItems();
    }

    @PostMapping
    public DamagedItem addDamagedItem(@RequestBody DamagedItem item) {
        if (item.getStatus() == null) {
            item.setStatus(DamagedItemStatus.REPORTED);
        }
        return damagedStockService.saveDamagedItem(item);
    }
}
