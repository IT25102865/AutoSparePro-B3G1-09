package com.lankaautoparts.autosparepro.payment.repository;

import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByRequestId(Long requestId);
    List<Payment> findByUsername(String username);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    double sumAmountByStatus(PaymentStatus status);
}
