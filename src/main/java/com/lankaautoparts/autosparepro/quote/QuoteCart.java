package com.lankaautoparts.autosparepro.quote;

import com.lankaautoparts.autosparepro.quote.model.QuoteLine;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer's session-only price quotation, built while browsing the
 * catalog. This is NOT a shopping cart and NOT tied to the database in any
 * way — it exists only in the HTTP session, is never persisted, and has no
 * connection whatsoever to the real Request -> Admin Approves -> Pay
 * purchase flow. It lets a customer put together a list of parts and print
 * it as a reference quotation (e.g. to show a mechanic or compare prices)
 * before deciding what to actually request for purchase.
 */
@Component
@SessionScope
public class QuoteCart {
    private final List<QuoteLine> lines = new ArrayList<>();

    public List<QuoteLine> getLines() {
        return lines;
    }

    /**
     * Adds a line for the given part, merging quantity into an existing
     * line for the same part number rather than creating a duplicate row.
     */
    public void addItem(String partNumber, String partName, double price, int quantity) {
        int qty = Math.max(quantity, 1);
        for (QuoteLine line : lines) {
            if (line.getPartNumber().equalsIgnoreCase(partNumber)) {
                line.setQuantity(line.getQuantity() + qty);
                return;
            }
        }
        lines.add(new QuoteLine(partNumber, partName, price, qty));
    }

    public void removeItem(String partNumber) {
        lines.removeIf(line -> line.getPartNumber().equalsIgnoreCase(partNumber));
    }

    public void clear() {
        lines.clear();
    }

    public double getTotal() {
        return lines.stream().mapToDouble(QuoteLine::getLineTotal).sum();
    }

    public int getItemCount() {
        return lines.size();
    }
}
