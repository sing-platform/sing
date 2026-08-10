package br.cadi.config;

import java.text.Normalizer;
import java.util.Locale;

final class SeedEmailUtils {

    private SeedEmailUtils() {
    }

    static String emailFromPersonName(String firstName, String lastName) {
        String local = normalizeToken(firstName) + "." + normalizeToken(lastName);
        return local + "@gmail.com";
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "user";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }
}
