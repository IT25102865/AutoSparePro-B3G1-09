package com.lankaautoparts.autosparepro.sales.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * One line item on an Invoice/Quotation — a specific part, quantity and the
 * price actually charged per unit (after any promotion discount). Every
 * invoice created by the cart checkout, an approved special-part request, or
 * the admin's "Build a Quotation" tool gets one of these per part, so the
 * invoice can be viewed and printed as an itemized quotation instead of just
 * a single total figure.
 */
@Entity
@Data
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    private String partNumber;
    private String partName;
    private int quantity;

    /** Price actually charged per unit (after any active promotion). */
    private double unitPrice;

    /** The part's normal catalog price per unit, kept for "you saved" display. */
    private double originalPrice;

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    public double getSavings() {
        return originalPrice > unitPrice ? (originalPrice - unitPrice) * quantity : 0.0;
    }

    public boolean isDiscounted() {
        return originalPrice > unitPrice;
    }
}
