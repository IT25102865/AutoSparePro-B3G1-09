package com.lankaautoparts.autosparepro.damagedstock.service;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import com.lankaautoparts.autosparepro.damagedstock.repository.DamagedItemRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DamagedStockService {
    private final DamagedItemRepository damagedItemRepository;

    public DamagedStockService(DamagedItemRepository damagedItemRepository) {
        this.damagedItemRepository = damagedItemRepository;
    }

    public List<DamagedItem> getAllDamagedItems() {
        return damagedItemRepository.findAll();
    }

    public DamagedItem saveDamagedItem(DamagedItem item) {
        return damagedItemRepository.save(item);
    }
}
