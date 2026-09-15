package com.lankaautoparts.autosparepro.inventory.repository;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long> {
}
