package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando el usuario autenticado no tiene permitida la acción solicitada (HTTP 403).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción de la acción prohibida
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
