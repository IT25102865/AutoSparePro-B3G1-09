package com.lankaautoparts.autosparepro.damagedstock.repository;

import com.lankaautoparts.autosparepro.damagedstock.model.DamagedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DamagedItemRepository extends JpaRepository<DamagedItem, Long> {
    Optional<DamagedItem> findFirstByPaymentIdOrderByIdDesc(Long paymentId);

    Optional<DamagedItem> findFirstByPaymentIdAndPartNumberOrderByIdDesc(Long paymentId, String partNumber);
}
