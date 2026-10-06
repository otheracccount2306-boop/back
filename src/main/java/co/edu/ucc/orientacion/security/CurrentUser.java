package co.edu.ucc.orientacion.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

public final class CurrentUser {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMINISTRADOR";

    private CurrentUser() {
    }

    public static UUID id(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
    }
}
