package com.lankaautoparts.autosparepro.promotion.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
}
