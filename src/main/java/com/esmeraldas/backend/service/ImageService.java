package com.esmeraldas.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.api.base-url:https://ecomerce-backend-crm.onrender.com}")
    private String apiBaseUrl;

    /**
     * Descarga una imagen desde una URL y la guarda localmente
     * @param imageUrl URL de la imagen (URL temporal de WhatsApp)
     * @param productId ID del producto
     * @return URL pública de la imagen guardada
     */
    public String downloadAndSaveImage(String imageUrl, Long productId) {
        try {
            log.info("Descargando imagen desde WhatsApp: {}", imageUrl);

            // Crear directorio de uploads si no existe
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Directorio de uploads creado: {}", uploadPath.toAbsolutePath());
            }

            // Generar nombre único para el archivo
            String extension = getImageExtension(imageUrl);
            String filename = "product_" + productId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetPath = uploadPath.resolve(filename);

            // Descargar imagen
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("No se pudo descargar la imagen desde WhatsApp");
            }

            // Guardar imagen
            Files.write(targetPath, imageBytes);
            log.info("Imagen guardada exitosamente: {}", filename);

            // Retornar URL pública
            String publicUrl = apiBaseUrl + "/api/images/" + filename;
            log.info("URL pública de la imagen: {}", publicUrl);

            return publicUrl;

        } catch (IOException e) {
            log.error("Error guardando imagen", e);
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error descargando imagen de WhatsApp", e);
            throw new RuntimeException("Error al descargar la imagen: " + e.getMessage());
        }
    }

    /**
     * Obtiene la extensión del archivo basado en la URL
     */
    private String getImageExtension(String url) {
        if (url.contains(".jpg") || url.contains(".jpeg")) {
            return ".jpg";
        } else if (url.contains(".png")) {
            return ".png";
        } else if (url.contains(".webp")) {
            return ".webp";
        }
        // Por defecto, WhatsApp usa JPG
        return ".jpg";
    }

    /**
     * Limpia imágenes antiguas (opcional para mantenimiento)
     */
    public void cleanupOldImages() {
        // Implementación futura para limpiar imágenes no usadas
    }
}
