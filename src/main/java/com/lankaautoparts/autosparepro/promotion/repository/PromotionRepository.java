package com.lankaautoparts.autosparepro.promotion.repository;

import com.lankaautoparts.autosparepro.promotion.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    /** Promotions whose date window contains {@code date}; a missing start/end means open-ended. */
    @Query("select p from Promotion p where (p.startDate is null or p.startDate <= :date) "
            + "and (p.endDate is null or p.endDate >= :date)")
    List<Promotion> findActiveOn(LocalDate date);
}
