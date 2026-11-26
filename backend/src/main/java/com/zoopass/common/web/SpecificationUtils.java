package com.zoopass.common.web;

import java.util.Locale;

/** Helpers for building JPA {@code Specification} search filters. */
public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /**
     * Turns free text into a lowercase LIKE pattern {@code %text%}, escaping {@code %}, {@code _} and
     * {@code \} so user input is matched literally. Use with {@code cb.like(expr, pattern, '\\')}.
     */
    public static String containsPattern(String text) {
        String escaped = text.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
