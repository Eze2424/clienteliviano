package ar.edu.utn.frba.ddsi.clienteliviano.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FotoRecepcionStorage {

  private static final Logger log = LoggerFactory.getLogger(FotoRecepcionStorage.class);

  public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
  public static final int MAX_FILES = 5;

  private final Path storageDir;
  private final String baseUrl;

  public FotoRecepcionStorage(
      @Value("${storage.recepciones.dir:./uploads/recepciones}") String storageDir,
      @Value("${app.base-url:http://localhost:8086}") String baseUrl) {
    this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    try {
      Files.createDirectories(this.storageDir);
    } catch (IOException e) {
      log.error("No se pudo crear el directorio de almacenamiento de fotos: {}", this.storageDir, e);
    }
  }

  public List<String> guardarFotos(MultipartFile[] files) {
    if (files == null || files.length == 0) {
      throw new IllegalArgumentException("Debe subir al menos una foto.");
    }

    List<MultipartFile> validFiles = Arrays.stream(files)
        .filter(f -> f != null && !f.isEmpty())
        .toList();

    if (validFiles.isEmpty()) {
      throw new IllegalArgumentException("Debe subir al menos una foto.");
    }

    if (validFiles.size() > MAX_FILES) {
      throw new IllegalArgumentException("No se pueden subir más de " + MAX_FILES + " fotos.");
    }

    List<String> urls = new ArrayList<>();
    List<Path> guardados = new ArrayList<>();

    try {
      for (MultipartFile file : validFiles) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\"))) {
          throw new IllegalArgumentException("Nombre de archivo malicioso o inválido: " + originalFilename);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
          throw new IllegalArgumentException("El archivo '" + originalFilename + "' supera el límite de 5 MB.");
        }

        String extension = detectarExtensionPorContenido(file);
        if (extension == null) {
          throw new IllegalArgumentException("El archivo '" + originalFilename + "' no es un formato de imagen permitido (se permite JPG, PNG, WebP).");
        }

        String nuevoNombre = UUID.randomUUID().toString() + extension;
        Path destino = storageDir.resolve(nuevoNombre).normalize();

        if (!destino.startsWith(storageDir)) {
          throw new IllegalArgumentException("Ruta de destino inválida para el archivo.");
        }

        try (InputStream in = file.getInputStream()) {
          Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        guardados.add(destino);
        urls.add(baseUrl + "/uploads/recepciones/" + nuevoNombre);
      }
      return urls;
    } catch (Exception e) {
      for (Path p : guardados) {
        try {
          Files.deleteIfExists(p);
        } catch (IOException ex) {
          log.warn("No se pudo eliminar archivo parcial {}: {}", p, ex.getMessage());
        }
      }
      if (e instanceof IllegalArgumentException iae) {
        throw iae;
      }
      throw new RuntimeException("Error al almacenar las fotos: " + e.getMessage(), e);
    }
  }

  public void eliminarFotos(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return;
    }

    for (String url : urls) {
      try {
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
          continue;
        }
        Path archivo = storageDir.resolve(filename).normalize();
        if (archivo.startsWith(storageDir)) {
          Files.deleteIfExists(archivo);
        }
      } catch (Exception e) {
        log.warn("No se pudo eliminar foto {}: {}", url, e.getMessage());
      }
    }
  }

  private String detectarExtensionPorContenido(MultipartFile file) {
    try (InputStream in = file.getInputStream()) {
      byte[] header = new byte[12];
      int read = in.read(header);
      if (read < 3) {
        return null;
      }

      // JPEG: FF D8 FF
      if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
        return ".jpg";
      }

      // PNG: 89 50 4E 47 0D 0A 1A 0A
      if (read >= 8 &&
          (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 &&
          (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47 &&
          (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A &&
          (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
        return ".png";
      }

      // WebP: RIFF .... WEBP
      if (read >= 12 &&
          header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F' &&
          header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
        return ".webp";
      }

      return null;
    } catch (IOException e) {
      log.warn("Error al leer contenido del archivo para validar tipo: {}", e.getMessage());
      return null;
    }
  }

  public Path getStorageDir() {
    return storageDir;
  }
}
