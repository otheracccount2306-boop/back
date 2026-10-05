package co.edu.ucc.orientacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Configuración de acceso a datos: plantilla JDBC con parámetros nombrados y gestor de
 * transacciones. No se utiliza JPA ni Hibernate.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * Crea la plantilla JDBC con parámetros nombrados usada por todos los repositorios.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param dataSource origen de datos configurado en application.properties
     * @return plantilla JDBC con parámetros nombrados
     */
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Crea el gestor de transacciones asociado al origen de datos.
     *
     * @author Doris Arzuaga
     * @author Diego Luna
     * @author Gabriela Zabaleta
     * @param dataSource origen de datos configurado en application.properties
     * @return gestor de transacciones JDBC
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
