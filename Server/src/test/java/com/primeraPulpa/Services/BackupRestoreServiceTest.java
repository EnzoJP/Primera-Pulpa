package com.primeraPulpa.Services;

import com.primeraPulpa.exceptions.ErrorServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupRestoreServiceTest {

    private BackupRestoreService backupRestoreService;

    @TempDir
    Path tempFolder;

    @BeforeEach
    void setUp() {
        backupRestoreService = new BackupRestoreService();
        ReflectionTestUtils.setField(backupRestoreService, "backupDir", tempFolder.toString());
        ReflectionTestUtils.setField(backupRestoreService, "dbUrl", "jdbc:postgresql://localhost:5432/primera_pulpa_test");
        ReflectionTestUtils.setField(backupRestoreService, "dbUser", "postgres");
        ReflectionTestUtils.setField(backupRestoreService, "dbPassword", "postgres");
    }

    // --- Tests de listarBackupsDisponibles ---

    @Test
    void listarBackupsDisponibles_filtraPorExtensionYOrdenaAlfabeticamente() throws IOException {
        Files.createFile(tempFolder.resolve("backup_2026_02.sql"));
        Files.createFile(tempFolder.resolve("backup_2026_01.sql.gz"));
        Files.createFile(tempFolder.resolve("archivo_invalido.txt"));
        Files.createFile(tempFolder.resolve("datos.csv"));
        Files.createDirectory(tempFolder.resolve("carpeta.sql"));

        List<String> backups = backupRestoreService.listarBackupsDisponibles();

        assertThat(backups).containsExactly(
                "backup_2026_01.sql.gz",
                "backup_2026_02.sql"
        );
    }

    @Test
    void listarBackupsDisponibles_devuelveListaVaciaSiCarpetaEstaVacia() {
        List<String> backups = backupRestoreService.listarBackupsDisponibles();

        assertThat(backups).isEmpty();
    }

    // --- Tests de Seguridad: validarNombreArchivo ---

    @Test
    void restaurarBackup_fallaSiNombreEsNuloOVacio() {
        assertThatThrownBy(() -> backupRestoreService.restaurarBackup(null))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar un nombre de archivo.");

        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("   "))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Debe indicar un nombre de archivo.");
    }

    @Test
    void restaurarBackup_previeneDirectoryTraversal() {
        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("../etc/passwd.sql"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Nombre de archivo no válido.");

        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("subcarpeta/backup.sql"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Nombre de archivo no válido.");

        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("..\\windows\\backup.sql"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("Nombre de archivo no válido.");
    }

    @Test
    void restaurarBackup_fallaSiExtensionNoEsSqlONoEsSqlGz() {
        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("backup.tar.gz"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El archivo debe ser un backup .sql o .sql.gz válido.");

        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("backup.zip"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El archivo debe ser un backup .sql o .sql.gz válido.");

        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("script_malicioso.sh"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El archivo debe ser un backup .sql o .sql.gz válido.");
    }

    // --- Tests de Existencia del Archivo en Disco ---

    @Test
    void restaurarBackup_fallaSiElArchivoNoExisteEnElVolumen() {
        assertThatThrownBy(() -> backupRestoreService.restaurarBackup("backup_inexistente.sql"))
                .isInstanceOf(ErrorServiceException.class)
                .hasMessageContaining("El archivo de backup no existe en el volumen.");
    }
}