package com.lankaautoparts.autosparepro.inventory.service;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.repository.PartRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {
    private final PartRepository partRepository;

    public InventoryService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public Optional<Part> findByPartNumber(String partNumber) {
        return partNumber == null ? Optional.empty() : partRepository.findFirstByPartNumber(partNumber);
    }

    /** Parts at or below their reorder level. */
    public List<Part> getLowStockParts() {
        return partRepository.findLowStock();
    }

    public Part getPartById(Long id) {
        return partRepository.findById(id).orElse(new Part());
    }

    public Part savePart(Part part) {
        return partRepository.save(part);
    }

    public void deletePart(Long id) {
        partRepository.deleteById(id);
    }
}
