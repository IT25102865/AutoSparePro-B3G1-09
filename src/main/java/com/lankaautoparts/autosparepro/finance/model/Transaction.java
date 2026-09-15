package com.lankaautoparts.autosparepro.finance.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceReference;
    private double amount;
    private String paymentMethod;
    private LocalDate date;
}
