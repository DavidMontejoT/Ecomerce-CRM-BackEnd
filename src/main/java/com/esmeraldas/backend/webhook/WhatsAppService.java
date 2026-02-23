package com.esmeraldas.backend.webhook;

import com.esmeraldas.backend.entity.Product;
import com.esmeraldas.backend.repository.ProductRepository;
import com.esmeraldas.backend.service.ImageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final ProductRepository productRepository;
    private final ImageService imageService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${whatsapp.api.url:https://graph.facebook.com}")
    private String whatsappApiUrl;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.api.version:v18.0}")
    private String apiVersion;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    // Store conversation state for each user
    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();

    public String handleMessage(JsonNode payload) {
        try {
            log.info("Received WhatsApp payload: {}", payload.toString());

            // Extract message from payload
            JsonNode entry = payload.path("entry");
            if (entry.isEmpty()) {
                return "No entries found";
            }

            JsonNode changes = entry.get(0).path("changes");
            if (changes.isEmpty()) {
                return "No changes found";
            }

            JsonNode value = changes.get(0).path("value");
            JsonNode messages = value.path("messages");

            if (messages.isEmpty()) {
                return "No messages found";
            }

            JsonNode message = messages.get(0);
            String from = message.path("from").asText();
            String messageId = message.path("id").asText();
            String text = message.path("text").path("body").asText().toLowerCase().trim();
            JsonNode image = message.path("image");

            log.info("Message from {}: {}", from, text);

            // Process message based on conversation state
            String response = processMessage(from, text, image);

            // Send response back to WhatsApp
            if (response != null && !response.isEmpty()) {
                sendMessage(from, response);
            }

            return "Message processed successfully";

        } catch (Exception e) {
            log.error("Error processing WhatsApp message", e);
            return "Error processing message: " + e.getMessage();
        }
    }

    private String processMessage(String from, String text, JsonNode image) {
        ConversationState state = conversationStates.get(from);

        // Check if this is a new conversation or command
        if (text.contains("inicio") || text.contains("empezar") || text.contains("ayuda")) {
            conversationStates.put(from, new ConversationState());
            return getWelcomeMessage();
        }

        if (text.contains("producto") || text.contains("subir") || text.contains("agregar")) {
            ConversationState newState = new ConversationState();
            newState.setStep(1);
            conversationStates.put(from, newState);
            return "📱 *Subir nuevo producto*\n\n" +
                   "Por favor, envíame la siguiente información:\n\n" +
                   "1️⃣ **Nombre del producto**\n" +
                   "Ejemplo: Esmeralda Colombiana 2ct\n\n" +
                   "Responde con el nombre del producto.";
        }

        // Process based on conversation state
        if (state == null) {
            return getWelcomeMessage();
        }

        switch (state.getStep()) {
            case 1: // Product name
                state.setName(text);
                state.setStep(2);
                return "✅ Nombre guardado: " + text + "\n\n" +
                       "2️⃣ **Descripción del producto**\n" +
                       "Ejemplo: Esmeralda natural de origen colombiano, color verde intenso, 2 quilates\n\n" +
                       "Responde con la descripción.";

            case 2: // Product description
                state.setDescription(text);
                state.setStep(3);
                return "✅ Descripción guardada\n\n" +
                       "3️⃣ **Precio del producto** (en USD)\n" +
                       "Ejemplo: 2500\n\n" +
                       "Responde con el precio (solo números).";

            case 3: // Product price
                try {
                    BigDecimal price = new BigDecimal(text.replaceAll("[^0-9.]", ""));
                    state.setPrice(price);
                    state.setStep(4);
                    return "✅ Precio guardado: $" + price + "\n\n" +
                           "4️⃣ **Categoría** (opcional)\n" +
                           "Ejemplo: Anillo, Collar, Pendientes, Sin categoría\n\n" +
                           "Responde con la categoría o escribe 'omitir'.";
                } catch (NumberFormatException e) {
                    return "❌ Precio inválido. Por favor, ingresa solo números.\n" +
                           "Ejemplo: 2500";
                }

            case 4: // Product category
                if (!text.equalsIgnoreCase("omitir")) {
                    state.setCategory(text);
                } else {
                    state.setCategory("Sin categoría");
                }
                state.setStep(5);
                return "✅ Categoría guardada\n\n" +
                       "5️⃣ **Número de WhatsApp para contacto**\n" +
                       "Ejemplo: +573001234567\n\n" +
                       "Responde con el número de WhatsApp.";

            case 5: // WhatsApp number
                state.setWhatsappNumber(text);
                state.setStep(6);
                return "✅ Número guardado\n\n" +
                       "6️⃣ **Imagen del producto**\n" +
                       "Por favor, envía la imagen del esmeralda.";

            case 6: // Image - download and save from WhatsApp
                if (image != null && !image.isEmpty()) {
                    String tempImageUrl = image.path("url").asText();
                    log.info("Imagen recibida de WhatsApp, URL temporal: {}", tempImageUrl);

                    try {
                        // First, save the product to get an ID
                        Product product = new Product();
                        product.setName(state.getName());
                        product.setDescription(state.getDescription());
                        product.setPrice(state.getPrice());
                        product.setCategory(state.getCategory());
                        product.setWhatsappNumber(state.getWhatsappNumber());
                        product.setAvailable(true);
                        product.setStock(1);

                        // Save to get the ID
                        Product savedProduct = productRepository.save(product);
                        log.info("Producto guardado con ID: {}", savedProduct.getId());

                        // Download and save image from WhatsApp
                        String permanentImageUrl = imageService.downloadAndSaveImage(tempImageUrl, savedProduct.getId());
                        log.info("Imagen descargada y guardada: {}", permanentImageUrl);

                        // Update product with permanent image URL
                        savedProduct.setImageUrl(permanentImageUrl);
                        productRepository.save(savedProduct);
                        log.info("Producto actualizado con imagen permanente");

                        // Clear conversation state
                        conversationStates.remove(from);

                        return "✅ *¡Producto agregado exitosamente!*\n\n" +
                               "📦 **" + state.getName() + "**\n" +
                               "💰 Precio: $" + state.getPrice() + "\n" +
                               "📝 " + state.getDescription() + "\n" +
                               "📷 Imagen descargada y guardada\n\n" +
                               "Tu producto ya está visible en el catálogo.\n\n" +
                               "👉 Para agregar otro producto, escribe 'subir producto'";

                    } catch (Exception e) {
                        log.error("Error procesando imagen", e);
                        conversationStates.remove(from);
                        return "❌ Hubo un error al procesar la imagen. Por favor, intenta nuevamente escribiendo 'subir producto'.\n\n" +
                               "Error: " + e.getMessage();
                    }
                } else {
                    return "❌ Por favor, envía una imagen.\n\n" +
                           "Si no tienes imagen, escribe 'omitir' para usar una imagen por defecto.";
                }

            default:
                return getWelcomeMessage();
        }
    }

    private String getWelcomeMessage() {
        return "👋 *Bienvenido a Esmeraldas Victory*\n\n" +
               "Comandos disponibles:\n\n" +
               "📦 *Subir producto* - Agregar un nuevo producto al catálogo\n" +
               "📋 *Ver productos* - Listar todos los productos\n" +
               "❓ *Ayuda* - Ver esta ayuda\n\n" +
               "Escribe un comando para comenzar.";
    }

    public void sendMessage(String to, String text) {
        try {
            String url = whatsappApiUrl + "/" + apiVersion + "/" + phoneNumberId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            // Build JSON using ObjectMapper to avoid escaping issues
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", to);
            requestBody.put("type", "text");

            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("body", text);
            requestBody.set("text", textNode);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            log.info("Sending WhatsApp message to {}: {}", to, jsonBody);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            String response = restTemplate.postForObject(url, request, String.class);

            log.info("Message sent successfully to {}: {}", to, response);

        } catch (Exception e) {
            log.error("Error sending WhatsApp message to {}", to, e);
        }
    }

    public boolean verifyToken(String mode, String token, String challenge) {
        // Verify webhook from WhatsApp - uses token from application.properties
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return true;
        }
        return false;
    }

    public String getChallenge(String mode, String token, String challenge) {
        if (verifyToken(mode, token, challenge)) {
            return challenge;
        }
        return null;
    }

    // Inner class to track conversation state
    private static class ConversationState {
        private int step = 0;
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String whatsappNumber;
        private String imageUrl;

        public int getStep() { return step; }
        public void setStep(int step) { this.step = step; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getWhatsappNumber() { return whatsappNumber; }
        public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}
