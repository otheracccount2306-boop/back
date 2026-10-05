package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud administrativa para activar o desactivar una cuenta.
 *
 * @author Doris Arzuaga
 * @param activo true para activar la cuenta, false para desactivarla
 */
public record UpdateStatusRequest(@NotNull Boolean activo) {
}
