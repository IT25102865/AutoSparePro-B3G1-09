package com.lankaautoparts.autosparepro.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Part {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String partNumber;
    private String partName;
    private String brand;
    private String vehicleModel;
    private int quantity;
    private int reorderLevel;
    private double price;
}
