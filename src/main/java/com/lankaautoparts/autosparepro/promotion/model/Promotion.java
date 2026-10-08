package com.lankaautoparts.autosparepro.promotion.model;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Promotion name is required")
    private String name;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
    private double discountPercentage;

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * The single Part category this promotion applies to, or null for a
     * storewide promotion that applies to every category. Left
     * unconstrained (no @NotNull) precisely so "storewide" can be
     * expressed as a blank selection.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PartCategory category;
}
