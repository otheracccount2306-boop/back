package co.edu.ucc.orientacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación monolítica de orientación estudiantil de la
 * Universidad Cooperativa de Colombia, campus Santa Marta.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@SpringBootApplication
@EnableScheduling
public class OrientacionApplication {

    /**
     * Inicia el contexto de Spring Boot y el servidor web embebido.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(OrientacionApplication.class, args);
    }
}
