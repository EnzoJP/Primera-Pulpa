package com.primeraPulpa.configs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DbConstraintsInitializer {

    @Bean
    public CommandLineRunner instalarSafecheckDesactivacion(JdbcTemplate jdbcTemplate) {
        return args -> {
            // Safecheck a nivel de base de datos: nunca se puede dejar al sistema
            // sin al menos un administrador activo (eliminado = false).
            // La DB no conoce la sesión, por eso esto cubre el invariante de datos;
            // la regla "no desactivar la propia cuenta" vive en Spring.
            jdbcTemplate.execute("""
                    CREATE OR REPLACE FUNCTION fn_previene_desactivar_ultimo_admin() RETURNS trigger AS $fn$
                    BEGIN
                        IF NEW.eliminado = true AND (OLD.eliminado IS DISTINCT FROM true) THEN
                            IF COALESCE((SELECT (r.descripcion = 'ADMIN')
                                         FROM rol r WHERE r.id = NEW.rol_id), false) THEN
                                IF (SELECT count(*)
                                    FROM usuario u
                                    JOIN rol r ON r.id = u.rol_id
                                    WHERE u.eliminado = false AND r.descripcion = 'ADMIN') <= 1 THEN
                                    RAISE EXCEPTION 'No se puede desactivar al ultimo administrador activo';
                                END IF;
                            END IF;
                        END IF;
                        RETURN NEW;
                    END;
                    $fn$ LANGUAGE plpgsql
                    """);
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_no_desactivar_ultimo_admin ON usuario");
            jdbcTemplate.execute("""
                    CREATE TRIGGER trg_no_desactivar_ultimo_admin
                    BEFORE UPDATE OF eliminado ON usuario
                    FOR EACH ROW EXECUTE FUNCTION fn_previene_desactivar_ultimo_admin()
                    """);
        };
    }
}