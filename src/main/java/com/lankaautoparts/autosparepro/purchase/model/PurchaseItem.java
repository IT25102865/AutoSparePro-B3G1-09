package com.lankaautoparts.autosparepro.purchase.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * A display-only row for the My Purchases page — not a JPA entity, just a
 * plain view model built by joining a completed Payment with the
 * PartRequest it paid for.
 */
@Data
public class PurchaseItem {
    private Long paymentId;
    private String partNumber;
    private String partName;
    private int quantity;
    private double amount;
    private LocalDate purchaseDate;
    private boolean alreadyReported;
    private String claimStatus;
}
