package com.esmeraldas.backend.controller;

import com.esmeraldas.backend.webhook.WhatsAppService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * WhatsApp webhook verification endpoint
     * This is called when setting up the webhook in WhatsApp Cloud API
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        log.info("Webhook verification attempt - Mode: {}, Token: {}", mode, token);

        String challengeResponse = whatsAppService.getChallenge(mode, token, challenge);

        if (challengeResponse != null) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challengeResponse);
        } else {
            log.warn("Webhook verification failed - invalid token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * WhatsApp webhook endpoint to receive messages
     * This is called when a user sends a message to the WhatsApp Business number
     */
    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String payload) {
        try {
            log.info("Received webhook payload: {}", payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            String result = whatsAppService.handleMessage(jsonNode);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing webhook message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to verify webhook is working
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "WhatsApp webhook is running",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Esmeraldas WhatsApp Webhook"
        ));
    }
}
