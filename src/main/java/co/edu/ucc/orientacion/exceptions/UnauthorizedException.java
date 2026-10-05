package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando la autenticación falla o las credenciales no son válidas (HTTP 401).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción del fallo de autenticación
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
