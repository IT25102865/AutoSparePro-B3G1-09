package com.lankaautoparts.autosparepro.sales.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceNumber;
    private String customerName;
    private double totalAmount;
    private double discount;
    private String paymentMethod;
    private LocalDate date;
}
