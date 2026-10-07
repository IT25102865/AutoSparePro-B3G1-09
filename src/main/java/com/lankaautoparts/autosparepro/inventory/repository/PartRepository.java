package com.lankaautoparts.autosparepro.inventory.repository;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findFirstByPartNumber(String partNumber);

    /** Parts at or below their reorder level. */
    @Query("select p from Part p where p.quantity <= p.reorderLevel")
    List<Part> findLowStock();
}
