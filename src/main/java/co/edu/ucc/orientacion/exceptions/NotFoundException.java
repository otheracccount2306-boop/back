package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando el recurso solicitado no existe o no está disponible (HTTP 404).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class NotFoundException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción del recurso no encontrado
     */
    public NotFoundException(String message) {
        super(message);
    }
}
