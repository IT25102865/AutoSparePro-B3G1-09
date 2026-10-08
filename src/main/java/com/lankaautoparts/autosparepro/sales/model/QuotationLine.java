package com.lankaautoparts.autosparepro.sales.model;

import lombok.Data;

import java.io.Serializable;

/**
 * One line in an admin's in-progress "Build a Quotation" draft (see
 * {@link com.lankaautoparts.autosparepro.sales.QuotationDraft}). Not a JPA
 * entity — it only lives in the admin's HTTP session until they save the
 * quotation, at which point each line becomes a persisted {@link InvoiceItem}.
 */
@Data
public class QuotationLine implements Serializable {
    private Long partId;
    private String partNumber;
    private String partName;
    private double unitPrice;
    private double originalPrice;
    private int quantity;

    public QuotationLine() {
    }

    public QuotationLine(Long partId, String partNumber, String partName, double unitPrice, double originalPrice, int quantity) {
        this.partId = partId;
        this.partNumber = partNumber;
        this.partName = partName;
        this.unitPrice = unitPrice;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
    }

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
