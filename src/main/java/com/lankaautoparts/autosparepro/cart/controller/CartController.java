package com.lankaautoparts.autosparepro.cart.controller;

import com.lankaautoparts.autosparepro.cart.Cart;
import com.lankaautoparts.autosparepro.cart.model.CartItem;
import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.inventory.service.InventoryService;
import com.lankaautoparts.autosparepro.payment.model.CardType;
import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.service.PaymentService;
import com.lankaautoparts.autosparepro.promotion.service.PromotionService;
import com.lankaautoparts.autosparepro.sales.model.Invoice;
import com.lankaautoparts.autosparepro.sales.model.InvoiceItem;
import com.lankaautoparts.autosparepro.sales.service.SalesService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Direct "browse catalog -> add to cart -> pay" checkout for customers buying
 * parts that ARE in the catalog. This does not use the admin-approval
 * PartRequest flow at all (that flow is now reserved for customers asking
 * for a part that isn't listed) — a cart checkout is instant, like a normal
 * online store: stock is checked and decremented, an Invoice + Payment are
 * created, and the customer lands on the same receipt page used elsewhere.
 *
 * Any active Promotion is applied automatically: the discounted per-unit
 * price is locked in the moment a part is added to the cart, and the total
 * amount saved flows through to the Invoice's discount field at checkout.
 * Each cart line also becomes a persisted InvoiceItem, so the resulting
 * invoice can be viewed as an itemized quotation and so the part shows up on
 * the customer's "My Purchases" page (where damage reporting now lives).
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final Cart cart;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final SalesService salesService;
    private final PromotionService promotionService;

    public CartController(Cart cart, InventoryService inventoryService, PaymentService paymentService,
                           SalesService salesService, PromotionService promotionService) {
        this.cart = cart;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.salesService = salesService;
        this.promotionService = promotionService;
    }

    @PostMapping("/add")
    public String add(@RequestParam Long partId, @RequestParam(defaultValue = "1") int quantity) {
        Part part = inventoryService.getPartById(partId);
        if (part.getId() != null && part.getQuantity() > 0) {
            int qtyToAdd = Math.min(quantity <= 0 ? 1 : quantity, part.getQuantity());
            double discountPercent = promotionService.getBestDiscountForPart(part);
            double unitPrice = part.getPrice() * (1 - (discountPercent / 100.0));
            cart.addItem(part, qtyToAdd, unitPrice);
        }
        return "redirect:/catalog?added=true";
    }

    @GetMapping
    public String view(Model model) {
        model.addAttribute("items", cart.getItemList());
        model.addAttribute("total", cart.getTotal());
        model.addAttribute("totalSavings", cart.getTotalSavings());
        return "cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long partId, @RequestParam int quantity) {
        cart.updateQuantity(partId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove/{partId}")
    public String remove(@PathVariable Long partId) {
        cart.removeItem(partId);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutForm(Model model) {
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("items", cart.getItemList());
        model.addAttribute("total", cart.getTotal());
        model.addAttribute("totalSavings", cart.getTotalSavings());
        model.addAttribute("cardTypes", CardType.values());
        return "cart-checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String cardHolderName,
                            @RequestParam String cardNumber,
                            @RequestParam CardType cardType,
                            @RequestParam String paymentMethod,
                            Model model,
                            Authentication authentication) {
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        // Re-check stock right before charging — it may have moved since the cart page loaded.
        for (Map.Entry<Long, CartItem> entry : cart.getItems().entrySet()) {
            Part part = inventoryService.getPartById(entry.getKey());
            if (part.getId() == null || part.getQuantity() < entry.getValue().getQuantity()) {
                model.addAttribute("items", cart.getItemList());
                model.addAttribute("total", cart.getTotal());
                model.addAttribute("totalSavings", cart.getTotalSavings());
                model.addAttribute("cardTypes", CardType.values());
                model.addAttribute("stockError", "Not enough stock for " + entry.getValue().getPartName()
                        + ". Please update the quantity in your cart and try again.");
                return "cart-checkout";
            }
        }

        double total = cart.getTotal();
        double totalSavings = cart.getTotalSavings();
        String invoiceNumber = "CART-" + System.currentTimeMillis();

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomerName(authentication.getName());
        invoice.setTotalAmount(total);
        invoice.setDiscount(totalSavings);
        invoice.setPaymentMethod("Pending");
        invoice.setDate(LocalDate.now());
        Invoice savedInvoice = salesService.saveInvoice(invoice);

        for (CartItem item : cart.getItemList()) {
            Part part = inventoryService.getPartById(item.getPartId());
            if (part.getId() != null) {
                part.setQuantity(part.getQuantity() - item.getQuantity());
                inventoryService.savePart(part);
            }

            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(savedInvoice);
            invoiceItem.setPartNumber(item.getPartNumber());
            invoiceItem.setPartName(item.getPartName());
            invoiceItem.setQuantity(item.getQuantity());
            invoiceItem.setUnitPrice(item.getPrice());
            invoiceItem.setOriginalPrice(item.getOriginalPrice());
            salesService.saveInvoiceItem(invoiceItem);
        }

        Payment saved = paymentService.makePayment(null, invoiceNumber, cardHolderName,
                cardNumber, total, authentication.getName(), cardType, paymentMethod);

        cart.clear();
        return "redirect:/payments/" + saved.getId() + "?success=true";
    }
}
