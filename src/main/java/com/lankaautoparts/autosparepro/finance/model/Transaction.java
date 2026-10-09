package com.lankaautoparts.autosparepro.finance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Invoice reference is required")
    private String invoiceReference;

    // Left unconstrained: AUTO refund entries are legitimately negative.
    private double amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private LocalDate date;

    /**
     * "AUTO" for transactions Finance records itself off a completed (or
     * refunded) payment, "MANUAL" for ones an admin typed in by hand. Before
     * this field existed, Finance was a disconnected manual-only table with
     * no link at all to what Payments/Sales were actually recording.
     */
    private String source;
}
