package co.edu.ucc.orientacion.exceptions;

/**
 * Se lanza cuando la solicitud es válida sintácticamente pero incumple una regla de negocio (HTTP 422).
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public class UnprocessableEntityException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param message descripción de la regla de negocio incumplida
     */
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
