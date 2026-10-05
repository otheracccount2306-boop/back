package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando la operación entra en conflicto con el estado actual de los datos (HTTP 409).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class ConflictException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción del conflicto
     */
    public ConflictException(String message) {
        super(message);
    }
}
