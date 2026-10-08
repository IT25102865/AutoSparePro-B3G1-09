package com.lankaautoparts.autosparepro.request.repository;

import com.lankaautoparts.autosparepro.request.model.PartRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartRequestRepository extends JpaRepository<PartRequest, Long> {
    List<PartRequest> findByUsernameOrderByRequestedDateDesc(String username);
}
