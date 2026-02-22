package com.esmeraldas.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class EsmeraldasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsmeraldasBackendApplication.class, args);
        System.out.println("🟢 Esmeraldas Backend is running!");
        System.out.println("📱 WhatsApp Webhook endpoint: /webhook");
        System.out.println("🛒 API endpoint: /api/products");
    }

    @GetMapping("/")
    public String home() {
        return "🟢 Esmeraldas Backend is Running! 🎉\n\nEndpoints:\n- GET /api/products\n- GET /webhook/health\n- GET /webhook/test";
    }
}
