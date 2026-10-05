package co.edu.ucc.orientacion.dto.response;

/**
 * Envoltorio estándar de todas las respuestas HTTP de la API.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 * @param success indica si la operación fue exitosa
 * @param data carga útil de la respuesta, nula en caso de error
 * @param message mensaje descriptivo de la operación
 */
public record ApiResponse(boolean success, Object data, String message) {

    /**
     * Crea una respuesta exitosa.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param data carga útil de la respuesta
     * @param message mensaje descriptivo de la operación
     * @return respuesta con success en true
     */
    public static ApiResponse ok(Object data, String message) {
        return new ApiResponse(true, data, message);
    }

    /**
     * Crea una respuesta de error.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message mensaje descriptivo del error
     * @return respuesta con success en false y sin datos
     */
    public static ApiResponse error(String message) {
        return new ApiResponse(false, null, message);
    }
}
