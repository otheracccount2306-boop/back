package co.edu.ucc.orientacion.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Utilidades para obtener los datos del usuario autenticado en la solicitud actual.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
public final class CurrentUser {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMINISTRADOR";

    private CurrentUser() {
    }

    /**
     * Obtiene el identificador del usuario autenticado.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param authentication autenticación de la solicitud
     * @return identificador UUID del usuario
     */
    public static UUID id(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    /**
     * Indica si el usuario autenticado tiene el rol ADMINISTRADOR.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param authentication autenticación de la solicitud, puede ser nula
     * @return true si el usuario es administrador
     */
    public static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}
