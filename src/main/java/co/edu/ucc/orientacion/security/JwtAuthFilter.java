package co.edu.ucc.orientacion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que autentica cada solicitud a partir del JWT del encabezado Authorization.
 * Los datos y el rol del usuario se consultan en base de datos en cada solicitud, de modo que
 * las desactivaciones y los cambios de rol tienen efecto inmediato.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Crea el filtro con sus dependencias.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param jwtUtils utilidades de validación de tokens
     * @param userDetailsService servicio de carga de usuarios
     */
    public JwtAuthFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Extrae y valida el token Bearer. Si es válido y la cuenta está activa, registra la
     * autenticación en el contexto de seguridad. Si no, la solicitud continúa sin autenticar
     * y el punto de entrada de seguridad responde HTTP 401.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param request solicitud HTTP entrante
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros
     * @throws ServletException cuando falla el procesamiento de la cadena de filtros
     * @throws IOException cuando falla la escritura o lectura de la solicitud
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtUtils.parseAccessToken(token).ifPresent(claims -> authenticate(claims.getSubject(), request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String userId, HttpServletRequest request) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            if (userDetails.isEnabled()) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
        }
    }
}
