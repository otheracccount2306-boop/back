package co.edu.ucc.orientacion.utils;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades de normalización de texto para categorías, códigos y patrones de búsqueda.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Normaliza un valor de catálogo: elimina tildes, recorta espacios, convierte a mayúsculas
     * y reemplaza los espacios internos por guion bajo. Por ejemplo, "Área común" se convierte
     * en AREA_COMUN.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param value texto original, puede ser nulo
     * @return texto normalizado, o null si el texto es nulo o está en blanco
     */
    public static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    /**
     * Construye un patrón para ILIKE que contiene el texto buscado, escapando los comodines
     * de SQL para que se traten como texto literal.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param search texto buscado, puede ser nulo
     * @return patrón con comodines en los extremos, o null si el texto es nulo o está en blanco
     */
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
