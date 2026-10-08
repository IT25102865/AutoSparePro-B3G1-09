package com.lankaautoparts.autosparepro.supplier.repository;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import com.lankaautoparts.autosparepro.supplier.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("select s from Supplier s where :category member of s.suppliedCategories")
    List<Supplier> findBySuppliedCategory(PartCategory category);
}
