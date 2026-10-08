package com.lankaautoparts.autosparepro.config;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * The single place that decides how money is displayed. Templates call it as
 * {@code ${@money.format(amount)}} -> "LKR 25,000.00".
 *
 * Amounts are stored as plain numbers with no currency attached, so changing
 * the currency here only changes how they're shown; it never converts a
 * stored value.
 */
@Component("money")
public class MoneyFormatter {

    public static final String CURRENCY_CODE = "LKR";

    /** DecimalFormat isn't thread-safe, so a fresh one is created per call (cheap, and called from request threads). */
    public String format(double amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return CURRENCY_CODE + " " + df.format(amount);
    }
}
