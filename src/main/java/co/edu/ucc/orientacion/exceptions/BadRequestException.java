package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando la solicitud contiene parámetros o datos inválidos (HTTP 400).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class BadRequestException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción del dato inválido
     */
    public BadRequestException(String message) {
        super(message);
    }
}
