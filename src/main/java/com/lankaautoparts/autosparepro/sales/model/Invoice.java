package com.lankaautoparts.autosparepro.sales.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @DecimalMin(value = "0.0", message = "Total amount cannot be negative")
    private double totalAmount;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private double discount;

    private String paymentMethod;
    private LocalDate date;
}
