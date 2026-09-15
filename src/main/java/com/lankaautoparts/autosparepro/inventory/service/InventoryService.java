package com.lankaautoparts.autosparepro.inventory.service;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.repository.PartRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class InventoryService {
    private final PartRepository partRepository;

    public InventoryService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public Part savePart(Part part) {
        return partRepository.save(part);
    }
}
