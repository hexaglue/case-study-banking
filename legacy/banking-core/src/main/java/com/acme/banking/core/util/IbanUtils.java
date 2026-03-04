package com.acme.banking.core.util;

/**
 * Utility class for IBAN operations.
 * Anti-pattern: Utility class in domain model - business logic should be in value object.
 */
public final class IbanUtils {

    private IbanUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates IBAN format (basic check).
     * Anti-pattern: Very simplistic validation, missing proper checksum validation.
     *
     * @param iban the IBAN to validate
     * @return true if format is valid
     */
    public static boolean isValid(String iban) {
        if (iban == null || iban.isEmpty()) {
            return false;
        }

        String cleaned = iban.replaceAll("\\s", "");

        // Basic format check: 15-34 characters, starts with 2 letters
        if (cleaned.length() < 15 || cleaned.length() > 34) {
            return false;
        }

        if (!Character.isLetter(cleaned.charAt(0)) || !Character.isLetter(cleaned.charAt(1))) {
            return false;
        }

        // Anti-pattern: Missing proper mod-97 checksum validation
        return cleaned.substring(2).chars().allMatch(Character::isLetterOrDigit);
    }

    /**
     * Formats IBAN in groups of 4 characters.
     *
     * @param iban the IBAN to format
     * @return formatted IBAN (e.g., "FR76 1234 5678 9012 3456 7890 123")
     */
    public static String format(String iban) {
        if (iban == null || iban.isEmpty()) {
            return iban;
        }

        String cleaned = iban.replaceAll("\\s", "");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < cleaned.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(' ');
            }
            formatted.append(cleaned.charAt(i));
        }

        return formatted.toString();
    }
}
