package co.edu.ucc.orientacion.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class TextUtils {

    private TextUtils() {
    }

    public static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    public static String normalizeRoom(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String key = withoutAccents.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim();
        return key.isEmpty() ? null : key;
    }

    public static String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
