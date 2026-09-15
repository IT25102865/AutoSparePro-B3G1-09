package com.lankaautoparts.autosparepro.damagedstock.repository;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DamagedItemRepository extends JpaRepository<DamagedItem, Long> {
}
