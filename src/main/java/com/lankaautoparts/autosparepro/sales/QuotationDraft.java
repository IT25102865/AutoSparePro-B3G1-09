package com.lankaautoparts.autosparepro.sales;

import com.lankaautoparts.autosparepro.inventory.model.Part;
import com.lankaautoparts.autosparepro.sales.model.QuotationLine;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The admin's in-progress "Build a Quotation" list — kept in their HTTP
 * session only, exactly like the customer {@link com.lankaautoparts.autosparepro.cart.Cart}.
 * Lets an admin add parts one at a time (with the total calculated
 * automatically) instead of typing a single lump total, then save the whole
 * thing as one Invoice with itemized InvoiceItem rows.
 */
@Component
@SessionScope
public class QuotationDraft implements Serializable {

    private final Map<Long, QuotationLine> lines = new LinkedHashMap<>();

    public void addLine(Part part, int quantity, double unitPrice) {
        if (quantity <= 0) {
            quantity = 1;
        }
        QuotationLine existing = lines.get(part.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            lines.put(part.getId(), new QuotationLine(part.getId(), part.getPartNumber(), part.getPartName(),
                    unitPrice, part.getPrice(), quantity));
        }
    }

    public void removeLine(Long partId) {
        lines.remove(partId);
    }

    public void clear() {
        lines.clear();
    }

    public Collection<QuotationLine> getLineList() {
        return lines.values();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public double getTotal() {
        return lines.values().stream().mapToDouble(QuotationLine::getSubtotal).sum();
    }

    public double getTotalSavings() {
        return lines.values().stream().mapToDouble(QuotationLine::getSavings).sum();
    }
}
