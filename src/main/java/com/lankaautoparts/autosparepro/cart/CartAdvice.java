package com.lankaautoparts.autosparepro.cart;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the current cart's item count available to every view (used for the
 * "Cart (N)" nav link) without every controller having to add it themselves.
 */
@ControllerAdvice
public class CartAdvice {

    private final Cart cart;

    public CartAdvice(Cart cart) {
        this.cart = cart;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount() {
        try {
            return cart.getItemCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
