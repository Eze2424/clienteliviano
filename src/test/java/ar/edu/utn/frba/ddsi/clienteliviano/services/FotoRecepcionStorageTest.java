package ar.edu.utn.frba.ddsi.clienteliviano.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FotoRecepcionStorageTest {

  @TempDir
  Path tempDir;

  private FotoRecepcionStorage storage;

  private static final byte[] JPEG_HEADER = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2};
  private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
  private static final byte[] WEBP_HEADER = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

  @BeforeEach
  void setUp() {
    storage = new FotoRecepcionStorage(tempDir.toString(), "http://localhost:8086");
  }

  @Test
  void guardarFotos_Valida_Exito() {
    MockMultipartFile file = new MockMultipartFile("fotos", "recibo.jpg", "image/jpeg", JPEG_HEADER);

    List<String> urls = storage.guardarFotos(new MultipartFile[]{file});

    assertEquals(1, urls.size());
    assertTrue(urls.get(0).startsWith("http://localhost:8086/uploads/recepciones/"));
    assertTrue(urls.get(0).endsWith(".jpg"));

    // Verificar que existe en disco
    String filename = urls.get(0).substring(urls.get(0).lastIndexOf('/') + 1);
    assertTrue(Files.exists(tempDir.resolve(filename)));

    // Eliminar y verificar limpieza
    storage.eliminarFotos(urls);
    assertFalse(Files.exists(tempDir.resolve(filename)));
  }

  @Test
  void guardarFotos_PngYWebp_Exito() {
    MockMultipartFile png = new MockMultipartFile("fotos", "foto.png", "image/png", PNG_HEADER);
    MockMultipartFile webp = new MockMultipartFile("fotos", "foto.webp", "image/webp", WEBP_HEADER);

    List<String> urls = storage.guardarFotos(new MultipartFile[]{png, webp});

    assertEquals(2, urls.size());
    assertTrue(urls.get(0).endsWith(".png"));
    assertTrue(urls.get(1).endsWith(".webp"));

    storage.eliminarFotos(urls);
  }

  @Test
  void guardarFotos_TipoFalso_LanzaExcepcion() {
    byte[] textoFalso = "esto no es una imagen real".getBytes();
    MockMultipartFile fake = new MockMultipartFile("fotos", "falso.jpg", "image/jpeg", textoFalso);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> storage.guardarFotos(new MultipartFile[]{fake}));
    assertTrue(ex.getMessage().contains("no es un formato de imagen permitido"));
  }

  @Test
  void guardarFotos_MayorA5MB_LanzaExcepcion() {
    byte[] grande = new byte[FotoRecepcionStorage.MAX_FILE_SIZE > Integer.MAX_VALUE ? 1 : (int) FotoRecepcionStorage.MAX_FILE_SIZE + 10];
    System.arraycopy(JPEG_HEADER, 0, grande, 0, JPEG_HEADER.length);

    MockMultipartFile fileGrande = new MockMultipartFile("fotos", "pesada.jpg", "image/jpeg", grande);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> storage.guardarFotos(new MultipartFile[]{fileGrande}));
    assertTrue(ex.getMessage().contains("supera el límite de 5 MB"));
  }

  @Test
  void guardarFotos_MasDe5Archivos_LanzaExcepcion() {
    MultipartFile[] seis = new MultipartFile[6];
    for (int i = 0; i < 6; i++) {
      seis[i] = new MockMultipartFile("fotos", "foto" + i + ".jpg", "image/jpeg", JPEG_HEADER);
    }

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> storage.guardarFotos(seis));
    assertTrue(ex.getMessage().contains("No se pueden subir más de 5 fotos"));
  }

  @Test
  void guardarFotos_NombreMaliciosoPathTraversal_LanzaExcepcion() {
    MockMultipartFile evil = new MockMultipartFile("fotos", "../../etc/shadow.jpg", "image/jpeg", JPEG_HEADER);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> storage.guardarFotos(new MultipartFile[]{evil}));
    assertTrue(ex.getMessage().contains("malicioso o inválido"));
  }

  @Test
  void guardarFotos_SinFotos_LanzaExcepcion() {
    assertThrows(IllegalArgumentException.class, () -> storage.guardarFotos(null));
    assertThrows(IllegalArgumentException.class, () -> storage.guardarFotos(new MultipartFile[0]));

    MockMultipartFile vacio = new MockMultipartFile("fotos", "vacio.jpg", "image/jpeg", new byte[0]);
    assertThrows(IllegalArgumentException.class, () -> storage.guardarFotos(new MultipartFile[]{vacio}));
  }
}
