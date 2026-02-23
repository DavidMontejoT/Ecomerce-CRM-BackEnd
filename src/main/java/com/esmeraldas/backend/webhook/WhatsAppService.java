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

        // Upload product command
        if (text.contains("subir") || text.contains("agregar")) {
            ConversationState newState = new ConversationState();
            newState.setStep(1);
            newState.setAction("upload");
            conversationStates.put(from, newState);
            return "📱 *Subir nuevo producto*\n\n" +
                   "Por favor, envíame la siguiente información:\n\n" +
                   "1️⃣ **Nombre del producto**\n" +
                   "Ejemplo: Esmeralda Colombiana 2ct\n\n" +
                   "Responde con el nombre del producto.";
        }

        // Edit product command
        if (text.contains("editar") || text.contains("modificar")) {
            return listProductsForEdit(from);
        }

        // Delete product command
        if (text.contains("borrar") || text.contains("eliminar")) {
            return listProductsForDelete(from);
        }

        // View products command
        if (text.contains("ver") && text.contains("producto")) {
            return listAllProducts();
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
                // Handle edit and delete flows
                if ("edit".equals(state.getAction())) {
                    return handleEditFlow(from, text, state);
                } else if ("delete".equals(state.getAction())) {
                    return handleDeleteFlow(from, text, state);
                }
                return getWelcomeMessage();
        }
    }

    private String getWelcomeMessage() {
        return "👋 *Bienvenido a Esmeraldas Victory*\n\n" +
               "Comandos disponibles:\n\n" +
               "📦 *Subir producto* - Agregar un nuevo producto al catálogo\n" +
               "✏️ *Editar producto* - Modificar un producto existente\n" +
               "🗑️ *Borrar producto* - Eliminar un producto del catálogo\n" +
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
            log.info("Sending WhatsApp message to {}", to);
            log.info("Request URL: {}", url);
            log.info("Request JSON: {}", jsonBody);

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

    // Helper methods for edit and delete operations

    private String listProductsForEdit(String from) {
        try {
            StringBuilder productList = new StringBuilder("✏️ *Editar Producto*\n\n");
            productList.append("Productos disponibles:\n\n");

            java.util.List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                return "📭 No hay productos disponibles. Primero agrega un producto con 'subir producto'.";
            }

            for (Product p : products) {
                productList.append("*ID ").append(p.getId()).append("*: ").append(p.getName())
                           .append("\n💰 Precio: $").append(p.getPrice())
                           .append("\n\n");
            }

            productList.append("Responde con el **ID** del producto que quieres editar.");

            // Set state for editing
            ConversationState state = new ConversationState();
            state.setStep(10); // Step 10 = waiting for product ID to edit
            state.setAction("edit");
            conversationStates.put(from, state);

            return productList.toString();
        } catch (Exception e) {
            log.error("Error listing products for edit", e);
            return "❌ Error al listar productos. Intenta nuevamente.";
        }
    }

    private String listProductsForDelete(String from) {
        try {
            StringBuilder productList = new StringBuilder("🗑️ *Borrar Producto*\n\n");
            productList.append("Productos disponibles:\n\n");

            java.util.List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                return "📭 No hay productos disponibles.";
            }

            for (Product p : products) {
                productList.append("*ID ").append(p.getId()).append("*: ").append(p.getName())
                           .append("\n💰 Precio: $").append(p.getPrice())
                           .append("\n\n");
            }

            productList.append("Responde con el **ID** del producto que quieres borrar.");

            // Set state for deleting
            ConversationState state = new ConversationState();
            state.setStep(20); // Step 20 = waiting for product ID to delete
            state.setAction("delete");
            conversationStates.put(from, state);

            return productList.toString();
        } catch (Exception e) {
            log.error("Error listing products for delete", e);
            return "❌ Error al listar productos. Intenta nuevamente.";
        }
    }

    private String listAllProducts() {
        try {
            StringBuilder productList = new StringBuilder("📋 *Catálogo de Productos*\n\n");

            java.util.List<Product> products = productRepository.findAll();
            if (products.isEmpty()) {
                return "📭 No hay productos disponibles en el catálogo.";
            }

            for (Product p : products) {
                productList.append("━━━━━━━━━━━━━━━━\n");
                productList.append("*").append(p.getName()).append("*\n");
                productList.append("💰 Precio: $").append(p.getPrice()).append("\n");
                if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                    productList.append("📝 ").append(p.getDescription()).append("\n");
                }
                if (p.getCategory() != null && !p.getCategory().isEmpty()) {
                    productList.append("🏷️ Categoría: ").append(p.getCategory()).append("\n");
                }
                productList.append("🆔 ID: ").append(p.getId()).append("\n");
                productList.append("━━━━━━━━━━━━━━━━\n\n");
            }

            productList.append("💡 Para editar o borrar, usa los comandos:");
            productList.append("\n✏️ 'editar producto'\n🗑️ 'borrar producto'");

            return productList.toString();
        } catch (Exception e) {
            log.error("Error listing products", e);
            return "❌ Error al listar productos. Intenta nuevamente.";
        }
    }

    private String handleEditFlow(String from, String text, ConversationState state) {
        try {
            switch (state.getStep()) {
                case 10: // Waiting for product ID
                    try {
                        Long productId = Long.parseLong(text.trim());
                        Product product = productRepository.findById(productId).orElse(null);

                        if (product == null) {
                            return "❌ Producto no encontrado. Responde con un ID válido o escribe 'ayuda'.";
                        }

                        state.setProductId(productId);
                        state.setStep(11);

                        return "✅ Producto seleccionado: *" + product.getName() + "*\n\n" +
                               "¿Qué campo quieres editar?\n\n" +
                               "1️⃣ Nombre\n" +
                               "2️⃣ Descripción\n" +
                               "3️⃣ Precio\n" +
                               "4️⃣ Categoría\n" +
                               "5️⃣ Número de WhatsApp\n\n" +
                               "Responde con el número de la opción (1-5).";

                    } catch (NumberFormatException e) {
                        return "❌ ID inválido. Responde con un número (ejemplo: 1)";
                    }

                case 11: // Waiting for field selection
                    String field = null;
                    switch (text.trim()) {
                        case "1": field = "name"; break;
                        case "2": field = "description"; break;
                        case "3": field = "price"; break;
                        case "4": field = "category"; break;
                        case "5": field = "whatsappNumber"; break;
                        default:
                            return "❌ Opción inválida. Responde con un número del 1 al 5.";
                    }

                    state.setFieldToEdit(field);
                    state.setStep(12);

                    String prompt = "";
                    switch (field) {
                        case "name":
                            prompt = "Responde con el nuevo **nombre** del producto:";
                            break;
                        case "description":
                            prompt = "Responde con la nueva **descripción** del producto:";
                            break;
                        case "price":
                            prompt = "Responde con el nuevo **precio** (solo números, ejemplo: 2500):";
                            break;
                        case "category":
                            prompt = "Responde con la nueva **categoría** (o escribe 'omitir'):";
                            break;
                        case "whatsappNumber":
                            prompt = "Responde con el nuevo **número de WhatsApp** (ejemplo: +573001234567):";
                            break;
                    }

                    return "✅ Campo seleccionado\n\n" + prompt;

                case 12: // Waiting for new value
                    Product product = productRepository.findById(state.getProductId()).orElse(null);
                    if (product == null) {
                        conversationStates.remove(from);
                        return "❌ Producto no encontrado. El flujo se ha cancelado.";
                    }

                    try {
                        switch (state.getFieldToEdit()) {
                            case "name":
                                product.setName(text);
                                break;
                            case "description":
                                product.setDescription(text);
                                break;
                            case "price":
                                BigDecimal newPrice = new BigDecimal(text.replaceAll("[^0-9.]", ""));
                                product.setPrice(newPrice);
                                break;
                            case "category":
                                product.setCategory(text.equalsIgnoreCase("omitir") ? "Sin categoría" : text);
                                break;
                            case "whatsappNumber":
                                product.setWhatsappNumber(text);
                                break;
                        }

                        productRepository.save(product);
                        conversationStates.remove(from);

                        return "✅ *Producto actualizado exitosamente!*\n\n" +
                               "📦 **" + product.getName() + "**\n" +
                               "💰 Precio: $" + product.getPrice() + "\n\n" +
                               "Para continuar, puedes:\n" +
                               "• Editar otro producto: 'editar producto'\n" +
                               "• Ver catálogo: 'ver productos'\n" +
                               "• Subir nuevo producto: 'subir producto'";

                    } catch (Exception e) {
                        conversationStates.remove(from);
                        return "❌ Error al actualizar el producto: " + e.getMessage() + "\n\n" +
                               "Para intentar de nuevo, escribe 'editar producto'.";
                    }

                default:
                    return getWelcomeMessage();
            }
        } catch (Exception e) {
            log.error("Error in edit flow", e);
            conversationStates.remove(from);
            return "❌ Error en el flujo de edición. Intenta nuevamente con 'editar producto'.";
        }
    }

    private String handleDeleteFlow(String from, String text, ConversationState state) {
        try {
            switch (state.getStep()) {
                case 20: // Waiting for product ID
                    try {
                        Long productId = Long.parseLong(text.trim());
                        Product product = productRepository.findById(productId).orElse(null);

                        if (product == null) {
                            return "❌ Producto no encontrado. Responde con un ID válido o escribe 'ayuda'.";
                        }

                        state.setProductId(productId);
                        state.setStep(21);

                        return "⚠️ *Confirmar eliminación*\n\n" +
                               "¿Estás seguro de que quieres borrar este producto?\n\n" +
                               "📦 *" + product.getName() + "*\n" +
                               "💰 Precio: $" + product.getPrice() + "\n\n" +
                               "Responde:\n" +
                               "✅ **'sí'** para confirmar\n" +
                               "❌ **'no'** para cancelar";

                    } catch (NumberFormatException e) {
                        return "❌ ID inválido. Responde con un número (ejemplo: 1)";
                    }

                case 21: // Waiting for confirmation
                    if (text.equalsIgnoreCase("si") || text.equalsIgnoreCase("sí") || text.equalsIgnoreCase("yes")) {
                        Product product = productRepository.findById(state.getProductId()).orElse(null);
                        if (product != null) {
                            productRepository.delete(product);
                            conversationStates.remove(from);

                            return "✅ *Producto borrado exitosamente!*\n\n" +
                                   "El producto ha sido eliminado del catálogo.\n\n" +
                                   "Para continuar:\n" +
                                   "• Ver catálogo: 'ver productos'\n" +
                                   "• Subir nuevo producto: 'subir producto'";
                        } else {
                            conversationStates.remove(from);
                            return "❌ Producto no encontrado. El flujo se ha cancelado.";
                        }
                    } else if (text.equalsIgnoreCase("no") || text.equalsIgnoreCase("cancelar")) {
                        conversationStates.remove(from);
                        return "❌ *Eliminación cancelada*\n\n" +
                               "El producto no ha sido borrado.\n\n" +
                               "Para volver al inicio, escribe 'ayuda'.";
                    } else {
                        return "❌ Respuesta no reconocida.\n\n" +
                               "Responde:\n" +
                               "✅ **'sí'** para confirmar la eliminación\n" +
                               "❌ **'no'** para cancelar";
                    }

                default:
                    return getWelcomeMessage();
            }
        } catch (Exception e) {
            log.error("Error in delete flow", e);
            conversationStates.remove(from);
            return "❌ Error en el flujo de eliminación. Intenta nuevamente con 'borrar producto'.";
        }
    }

    // Inner class to track conversation state
    private static class ConversationState {
        private int step = 0;
        private String action; // upload, edit, delete
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private String whatsappNumber;
        private String imageUrl;
        private Long productId; // For edit/delete operations
        private String fieldToEdit; // Which field to edit

        public int getStep() { return step; }
        public void setStep(int step) { this.step = step; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
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
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getFieldToEdit() { return fieldToEdit; }
        public void setFieldToEdit(String fieldToEdit) { this.fieldToEdit = fieldToEdit; }
    }
}
