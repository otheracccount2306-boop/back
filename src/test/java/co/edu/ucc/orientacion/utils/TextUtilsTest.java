package co.edu.ucc.orientacion.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pruebas de las utilidades de texto y de hashing.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
class TextUtilsTest {

    @Test
    @DisplayName("normalizeKey quita tildes, pasa a mayúsculas y reemplaza espacios por guion bajo")
    void normalizeKeyNormalizes() {
        assertEquals("AREA_COMUN", TextUtils.normalizeKey("  Área común "));
        assertEquals("MIERCOLES", TextUtils.normalizeKey("miércoles"));
        assertEquals("PSICOLOGIA", TextUtils.normalizeKey("Psicología"));
    }

    @Test
    @DisplayName("normalizeKey devuelve null para texto nulo o en blanco")
    void normalizeKeyHandlesBlank() {
        assertNull(TextUtils.normalizeKey(null));
        assertNull(TextUtils.normalizeKey("   "));
    }

    @Test
    @DisplayName("likePattern envuelve el texto y escapa los comodines de SQL")
    void likePatternEscapesWildcards() {
        assertEquals("%lab%", TextUtils.likePattern(" lab "));
        assertEquals("%100\\%%", TextUtils.likePattern("100%"));
        assertEquals("%a\\_b%", TextUtils.likePattern("a_b"));
        assertEquals("%a\\\\b%", TextUtils.likePattern("a\\b"));
    }

    @Test
    @DisplayName("likePattern devuelve null para texto nulo o en blanco")
    void likePatternHandlesBlank() {
        assertNull(TextUtils.likePattern(null));
        assertNull(TextUtils.likePattern(""));
    }

    @Test
    @DisplayName("sha256Hex produce el hash conocido y generateToken produce valores distintos")
    void hashUtilsWork() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HashUtils.sha256Hex("abc"));
        assertNotEquals(HashUtils.generateToken(), HashUtils.generateToken());
    }
}
