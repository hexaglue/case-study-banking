package com.acme.banking.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for money operations.
 * Anti-pattern: Utility class in domain model - business logic should be in Money value object.
 */
public final class MoneyUtils {

    private MoneyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Rounds amount to 2 decimal places using HALF_UP rounding.
     * Anti-pattern: Money operations scattered in utility class instead of value object.
     *
     * @param amount the amount to round
     * @return rounded amount
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Formats amount with currency symbol.
     * Anti-pattern: Presentation logic in domain utility.
     *
     * @param amount   the amount to format
     * @param currency the currency code (e.g., "EUR", "USD")
     * @return formatted string (e.g., "1,234.56 EUR")
     */
    public static String format(BigDecimal amount, String currency) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "EUR";
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return formatter.format(amount) + " " + currency;
    }
}
