package com.esmeraldas.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EsmeraldasBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsmeraldasBackendApplication.class, args);
        System.out.println("🟢 Esmeraldas Backend is running!");
        System.out.println("📱 WhatsApp Webhook endpoint: /webhook");
        System.out.println("🛒 API endpoint: /api/products");
    }
}
