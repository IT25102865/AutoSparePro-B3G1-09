package com.lankaautoparts.autosparepro.damagedstock.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class DamagedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Part number is required")
    private String partNumber;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    private LocalDate dateLogged;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String reportedBy;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private DamagedItemStatus status;

    /**
     * The id of the customer's Payment this claim was reported against, or
     * null for a claim-less internal stock log (damage found in the
     * warehouse, not reported by a customer). Used to gate refunds to a
     * real purchase, and to stop a customer reporting damage on something
     * they never bought.
     */
    private Long paymentId;
}
