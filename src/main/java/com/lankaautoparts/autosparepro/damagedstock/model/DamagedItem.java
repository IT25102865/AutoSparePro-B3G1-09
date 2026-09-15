package com.lankaautoparts.autosparepro.damagedstock.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class DamagedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String partNumber;
    private int quantity;
    private LocalDate dateLogged;
    private String reason;
}
