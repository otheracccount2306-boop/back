package co.edu.ucc.orientacion.security;

import co.edu.ucc.orientacion.models.Usuario;
import co.edu.ucc.orientacion.repositories.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Carga los datos de seguridad de un usuario a partir de su identificador UUID, que actúa
 * como nombre de usuario dentro del contexto de seguridad.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Crea el servicio con el repositorio de usuarios.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param userRepository repositorio de usuarios
     */
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga el usuario por su identificador UUID. Las cuentas desactivadas se devuelven
     * como deshabilitadas.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param username identificador UUID del usuario en formato texto
     * @return detalles del usuario con su rol como autoridad
     * @throws UsernameNotFoundException cuando el identificador no es válido o no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UUID id;
        try {
            id = UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Identificador de usuario inválido");
        }
        Usuario usuario = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(usuario.id().toString())
                .password(usuario.contrasenaHash())
                .roles(usuario.rol())
                .disabled(!usuario.activo())
                .build();
    }
}
