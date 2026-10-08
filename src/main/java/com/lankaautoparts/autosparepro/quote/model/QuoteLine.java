package com.lankaautoparts.autosparepro.quote.model;

/**
 * One line of a customer's session-only price quotation. Deliberately NOT a
 * JPA @Entity — nothing about the quotation is persisted to the database,
 * it exists purely in the customer's HTTP session while they browse the
 * catalog and decide what to actually request.
 */
public class QuoteLine {
    private final String partNumber;
    private final String partName;
    private final double price;
    private int quantity;

    public QuoteLine(String partNumber, String partName, double price, int quantity) {
        this.partNumber = partNumber;
        this.partName = partName;
        this.price = price;
        this.quantity = quantity;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public String getPartName() {
        return partName;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return price * quantity;
    }
}
