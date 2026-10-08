package com.lankaautoparts.autosparepro.cart;

import com.lankaautoparts.autosparepro.cart.model.CartItem;
import com.lankaautoparts.autosparepro.inventory.model.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One customer's shopping cart, kept in their HTTP session only — nothing
 * here touches the database until checkout. Session-scoped so every logged-in
 * customer gets their own independent cart.
 */
@Component
@SessionScope
public class Cart implements Serializable {

    private final Map<Long, CartItem> items = new LinkedHashMap<>();

    /**
     * Adds a part to the cart at the given per-unit price (the price the
     * customer will actually be charged, after any active promotion). The
     * part's normal catalog price is kept alongside it purely for display
     * (strikethrough price / "you saved" messaging).
     */
    public void addItem(Part part, int quantity, double unitPrice) {
        if (quantity <= 0) {
            quantity = 1;
        }
        CartItem existing = items.get(part.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.put(part.getId(), new CartItem(part.getId(), part.getPartNumber(), part.getPartName(),
                    unitPrice, part.getPrice(), quantity));
        }
    }

    public void updateQuantity(Long partId, int quantity) {
        CartItem item = items.get(partId);
        if (item == null) {
            return;
        }
        if (quantity <= 0) {
            items.remove(partId);
        } else {
            item.setQuantity(quantity);
        }
    }

    public void removeItem(Long partId) {
        items.remove(partId);
    }

    public void clear() {
        items.clear();
    }

    public Map<Long, CartItem> getItems() {
        return items;
    }

    public Collection<CartItem> getItemList() {
        return items.values();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getItemCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getTotal() {
        return items.values().stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    /** Total saved across the whole cart thanks to active promotions. */
    public double getTotalSavings() {
        return items.values().stream().mapToDouble(CartItem::getSavings).sum();
    }
}
