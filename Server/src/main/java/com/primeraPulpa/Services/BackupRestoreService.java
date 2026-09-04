package com.primeraPulpa.Services;

import com.primeraPulpa.exceptions.ErrorServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BackupRestoreService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.dir:/backups}")
    private String backupDir;

    private static final Pattern URL_PATTERN =
            Pattern.compile("^jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");

    /**
     * Lista los archivos de backup disponibles en el volumen compartido.
     */
    public List<String> listarBackupsDisponibles() {
        File folder = new File(backupDir);
        File[] files = folder.listFiles((dir, name) ->
                name.endsWith(".sql.gz") || name.endsWith(".sql"));
        List<String> nombres = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    nombres.add(file.getName());
                }
            }
            nombres.sort(String::compareToIgnoreCase);
        }
        return nombres;
    }

    /**
     * Restaura un backup. El dump generado por pg_dump no es destructivo
     * (trae CREATE + COPY), por lo que se vacían las tablas de la DB antes
     * de volcar el archivo, evitando conflictos de claves duplicadas.
     */
    public void restaurarBackup(String fileName) {
        validarNombreArchivo(fileName);

        File backupFile = new File(backupDir, fileName);
        if (!backupFile.isFile() || !backupFile.exists()) {
            throw new ErrorServiceException("El archivo de backup no existe en el volumen.");
        }

        DatosConexion dc = obtenerDatosConexion();

        // 1) Vaciar la base antes de restaurar (operación destructiva, confirmada por el admin).
        ejecutarVaciado(dc);

        // 2) Volcar el backup (gunzip -c archivo | psql ...).
        ejecutarRestauracion(backupFile, dc);
    }

    // ---- Destrucción previa ----------------------------------------------

    private void ejecutarVaciado(DatosConexion dc) throws ErrorServiceException {
        // Un dump de pg_dump (sin --clean) asume una base VACÍA: trae CREATE TABLE,
        // CREATE FUNCTION y CREATE TRIGGER. Por eso hay que dropear TODO el schema
        // public (tablas, secuencias, funciones y triggers) antes de restaurar.
        // Así se evitan errores de "X already exists" que abortarían la restauración.
        String sql = "DROP SCHEMA public CASCADE; CREATE SCHEMA public;";
        String[] args = {
                "psql", "-h", dc.host, "-p", String.valueOf(dc.port),
                "-U", dc.usuario, "-d", dc.base, "-v", "ON_ERROR_STOP=1",
                "-c", sql
        };
        int code = ejecutarProceso(crearProceso(args));
        if (code != 0) {
            throw new ErrorServiceException("No se pudo vaciar la base de datos (código " + code + ").");
        }
    }

    // ---- Restauración -----------------------------------------------------

    private void ejecutarRestauracion(File backupFile, DatosConexion dc) {
        String lector = backupFile.getName().endsWith(".gz")
                ? "gunzip -c \"" + backupFile.getAbsolutePath() + "\""
                : "cat \"" + backupFile.getAbsolutePath() + "\"";
        String psql = "psql -h " + dc.host + " -p " + dc.port + " -U " + dc.usuario
                + " -d " + dc.base + " -v ON_ERROR_STOP=1";
        String comando = lector + " | " + psql;

        ProcessBuilder pb = crearProcesoDeShell(comando);
        pb.environment().put("PGPASSWORD", dc.clave);
        int code = ejecutarProceso(pb);
        if (code != 0) {
            throw new ErrorServiceException("La restauración del backup finalizó con errores (código " + code + ").");
        }
    }

    // ---- Datos de conexión -----------------------------------------------

    private DatosConexion obtenerDatosConexion() {
        DatosConexion dc = new DatosConexion();
        dc.usuario = dbUser == null ? "postgres" : dbUser;
        dc.clave = dbPassword == null ? "" : dbPassword;
        dc.host = "localhost";
        dc.port = 5432;
        dc.base = dbUrl == null ? "" : dbUrl;
        Matcher m = URL_PATTERN.matcher(dbUrl == null ? "" : dbUrl);
        if (m.find()) {
            dc.host = m.group(1);
            if (m.group(2) != null && !m.group(2).isEmpty()) {
                dc.port = Integer.parseInt(m.group(2));
            }
            dc.base = m.group(3);
        }
        return dc;
    }

    private static class DatosConexion {
        String host;
        int port;
        String usuario;
        String clave;
        String base;
    }

    // ---- Validación de seguridad -----------------------------------------

    private void validarNombreArchivo(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new ErrorServiceException("Debe indicar un nombre de archivo.");
        }
        // Prevenir directory traversal / path absoluto.
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new ErrorServiceException("Nombre de archivo no válido.");
        }
        if (!fileName.matches("^[A-Za-z0-9._-]+\\.(sql|sql\\.gz)$")) {
            throw new ErrorServiceException("El archivo debe ser un backup .sql o .sql.gz válido.");
        }
    }

    // ---- Procesos ---------------------------------------------------------

    private ProcessBuilder crearProceso(String[] args) {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().put("PGPASSWORD", dbPassword == null ? "" : dbPassword);
        return pb;
    }

    private ProcessBuilder crearProcesoDeShell(String comando) {
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", comando);
        pb.environment().put("PGPASSWORD", dbPassword == null ? "" : dbPassword);
        return pb;
    }

    private int ejecutarProceso(ProcessBuilder pb) throws ErrorServiceException {
        try {
            Process process = pb.start();
            StringBuilder err = new StringBuilder();
            Thread lector = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        err.append(linea).append("\n");
                    }
                } catch (IOException ignored) {
                }
            });
            lector.start();
            int exitCode = process.waitFor();
            lector.join();
            if (exitCode != 0) {
                System.err.println("----- ERROR RESTAURACIÓN -----");
                System.err.println(err);
                System.err.println("-------------------------------");
            }
            return exitCode;
        } catch (IOException | InterruptedException e) {
            throw new ErrorServiceException("Error al ejecutar la restauración: " + e.getMessage());
        }
    }
}
