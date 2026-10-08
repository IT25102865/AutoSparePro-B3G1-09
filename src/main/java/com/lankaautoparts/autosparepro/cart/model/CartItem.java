package com.lankaautoparts.autosparepro.cart.model;

import lombok.Data;

import java.io.Serializable;

/**
 * A single line in a customer's shopping cart. Not a JPA entity — the cart
 * itself only lives in the HTTP session (see {@link com.lankaautoparts.autosparepro.cart.Cart}),
 * so nothing here is persisted to the database until checkout, when it becomes
 * a Payment + Invoice.
 *
 * {@code price} is the price actually charged per unit (after any active
 * promotion discount is applied at add-to-cart time). {@code originalPrice}
 * is the part's normal catalog price, kept only so the cart/checkout pages
 * can show a strikethrough price and the amount saved.
 */
@Data
public class CartItem implements Serializable {
    private Long partId;
    private String partNumber;
    private String partName;
    private double price;
    private double originalPrice;
    private int quantity;

    public CartItem() {
    }

    public CartItem(Long partId, String partNumber, String partName, double price, double originalPrice, int quantity) {
        this.partId = partId;
        this.partNumber = partNumber;
        this.partName = partName;
        this.price = price;
        this.originalPrice = originalPrice;
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return price * quantity;
    }

    /** How much this line saved thanks to an active promotion, if any. */
    public double getSavings() {
        return originalPrice > price ? (originalPrice - price) * quantity : 0.0;
    }

    public boolean isDiscounted() {
        return originalPrice > price;
    }
}
