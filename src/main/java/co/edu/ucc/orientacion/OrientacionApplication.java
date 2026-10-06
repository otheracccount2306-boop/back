package co.edu.ucc.orientacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrientacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrientacionApplication.class, args);
    }
}
