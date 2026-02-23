package com.esmeraldas.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "https://esmeraldas-frontend.onrender.com", "https://ecomerce-frontend-crm.onrender.com"})
public class ImageController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path imagePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = org.springframework.core.io.FileSystemResource.createResource(imagePath.toFile());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving image: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Image Service is Running 📷");
    }
}
