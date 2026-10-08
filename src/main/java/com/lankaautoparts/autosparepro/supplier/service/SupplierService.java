package com.lankaautoparts.autosparepro.supplier.service;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.supplier.model.Supplier;
import com.lankaautoparts.autosparepro.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    /** Suppliers on file who can supply the given part category — used by the Low Stock Alerts panel. */
    public List<Supplier> getSuppliersForCategory(PartCategory category) {
        if (category == null) return List.of();
        return supplierRepository.findBySuppliedCategory(category);
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id).orElse(new Supplier());
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }
}
