# Victory Esmeraldas - Backend API

## 📋 Descripción General

Sistema backend para e-commerce de venta de esmeraldas colombianas. Construido con Spring Boot 3.2 y PostgreSQL, ofrece una API REST robusta con integración a WhatsApp Cloud API para gestión automatizada de productos.

## 🎯 Objetivos del Proyecto

1. **Comercio Electrónico Premium**: Plataforma especializada en venta de esmeraldas colombianas de alta calidad
2. **Gestión de Inventario**: Sistema CRUD completo para administración de catálogo de productos
3. **Integración WhatsApp**: Chatbot inteligente para agregar productos mediante conversación natural
4. **Arquitectura Cloud-Native**: Sistema preparado para escalabilidad y producción en plataforma serverless
5. **API RESTful**: Interfaz moderna y optimizada para frontend React
6. **Experiencia de Usuario**: Respuesta rápida y confiable para excelente UX

## 🏗️ Arquitectura Tecnológica

### Stack Tecnológico

| Componente | Tecnología | Versión | Propósito |
|------------|------------|---------|-----------|
| Backend Framework | Spring Boot | 3.2.0 | Marco principal |
| Lenguaje | Java | 17 LTS | Desarrollo |
| Build Tool | Maven | - | Gestión de dependencias |
| Database | PostgreSQL | 14+ | Persistencia de datos |
| ORM | Hibernate/JPA | - | Mapeo objeto-relacional |
| API Integration | WhatsApp Cloud API | v18.0 | Mensajería |
| Deployment | Docker | - | Contenerización |
| Cloud Platform | Render | - | Hosting producción |

### Patrón Arquitectónico

- **Arquitectura en Capas**: Controller → Service → Repository → Entity
- **Inyección de Dependencias**: Constructor-based con Spring
- **Configuración Externa**: Environment variables para seguridad
- **API REST**: Recursos RESTful con HTTP semántico

## 📁 Estructura del Proyecto

```
src/main/java/com/esmeraldas/backend/
├── EsmeraldasBackendApplication.java  # Clase principal
├── config/                             # Configuraciones Spring
│   └── CorsConfig.java                 # Configuración CORS
├── controller/                         # Controladores REST
│   ├── ProductController.java          # API Productos
│   └── WhatsAppWebhookController.java  # Webhook WhatsApp
├── dto/                                # Data Transfer Objects
│   ├── WhatsAppMessageDto.java         # Mensajes WhatsApp
│   └── WhatsAppResponse.java           # Respuestas API
├── entity/                             # Entidades JPA
│   └── Product.java                    # Modelo Producto
├── repository/                         # Repositorios Spring Data
│   └── ProductRepository.java          # Datos Productos
├── service/                            # Lógica de Negocio
│   └── ProductService.java             # Servicios Productos
└── webhook/                            # Servicios WhatsApp
    └── WhatsAppService.java            # Lógica Chatbot

src/main/resources/
├── application.properties              # Configuración app
└── logback.xml                        # Logging (opcional)
```

## 🔌 Endpoints API

### Productos

| Método | Endpoint | Descripción | Response |
|--------|----------|-------------|----------|
| GET | `/api/products` | Listar productos disponibles | 200 OK |
| GET | `/api/products/{id}` | Obtener producto por ID | 200 OK / 404 |
| POST | `/api/products` | Crear nuevo producto | 201 Created |
| PUT | `/api/products/{id}` | Actualizar producto | 200 OK / 404 |
| DELETE | `/api/products/{id}` | Eliminar producto | 204 No Content |
| GET | `/api/products/search?keyword=` | Buscar productos | 200 OK |
| GET | `/api/products/category/{category}` | Filtrar por categoría | 200 OK |

### WhatsApp Webhook

| Método | Endpoint | Descripción | Uso |
|--------|----------|-------------|-----|
| GET | `/webhook` | Verificación de webhook | Meta verify |
| POST | `/webhook` | Recepción de mensajes | Chatbot |
| GET | `/webhook/test` | Test de conectividad | Diagnóstico |
| GET | `/webhook/health` | Health check | Monitoreo |

### Sistema

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Información del sistema | Status |

## 📊 Modelo de Datos

### Entity: Product

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;                    // Nombre del producto
    private String description;             // Descripción detallada
    private BigDecimal price;               // Precio en USD
    private String imageUrl;                // URL de imagen
    private String category;                // Categoría (Anillo, Collar, etc.)
    private Integer stock;                  // Inventario
    private Boolean available;              // Disponibilidad
    private String whatsappNumber;          // Contacto WhatsApp
    private LocalDateTime createdAt;         // Fecha creación
    private LocalDateTime updatedAt;         // Última actualización
}
```

### Relaciones

- **Sin relaciones complejas** (Sistema simple actual)
- **Escalable** para agregar: Users, Orders, Invoices (futuro)

## 🔐 Configuración de Seguridad

### Variables de Entorno Requeridas

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://host:port/database
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password

# WhatsApp Cloud API Configuration
WHATSAPP_ACCESS_TOKEN=token_de_acceso
WHATSAPP_PHONE_NUMBER_ID=phone_number_id
WHATSAPP_VERIFY_TOKEN=verify_token_seguro

# CORS Configuration
FRONTEND_URL=https://frontend-url.com

# Server Configuration
PORT=8080
```

### ⚠️ Seguridad - IMPORTANTE

**Nunca commits información sensible:**
- ❌ Tokens de acceso reales
- ❌ Contraseñas de base de datos
- ❌ API Keys
- ❌ Secrets de producción
- ❌ Credenciales de WhatsApp

**Usa siempre:**
- ✅ Variables de entorno (`.env` files)
- ✅ Secrets de plataforma (Render, GitHub)
- ✅ Archivos `.gitignore` apropiados
- ✅ Tokens temporales para desarrollo

## 🚀 Despliegue

### Desarrollo Local

**Prerequisitos:**
- Java 17+
- Maven 3.9+
- PostgreSQL 14+

**Pasos:**
```bash
# Clonar repositorio
git clone [repo-url]
cd backend

# Configurar base de datos (ver sección Database Setup)

# Ejecutar
mvn spring-boot:run
```

**Acceso:** `http://localhost:8080`

### Producción - Render

**Preparación:**
1. Código en GitHub (rama `main`)
2. Variables de entorno configuradas
3. Base de datos PostgreSQL creada

**Pasos:**
1. Crear "Web Service" en Render
2. Conectar repositorio GitHub
3. Configurar:
   - Runtime: Docker
   - Dockerfile Path: `./Dockerfile`
4. Configurar variables de entorno
5. Deploy automático

**URL de producción:** `https://[service-name].onrender.com`

## 📱 Integración WhatsApp Cloud API

### Flujo del Chatbot

```
Usuario WhatsApp → "subir producto"
       ↓
Bot solicita: Nombre
       ↓
Usuario envía: "Esmeralda Colombiana 2ct"
       ↓
Bot solicita: Descripción
       ↓
Usuario envía: "Color verde intenso, 2 quilates..."
       ↓
Bot solicita: Precio (USD)
       ↓
Usuario envía: "2500"
       ↓
Bot solicita: Categoría
       ↓
Usuario envía: "Anillo"
       ↓
Bot solicita: Número WhatsApp contacto
       ↓
Usuario envía: "573001234567"
       ↓
Bot solicita: Imagen del producto
       ↓
Usuario envía foto 📷
       ↓
✅ Producto creado automáticamente
       ↓
Producto visible en frontend
```

### Comandos Disponibles

- `subir producto` - Inicia creación de producto
- `productos` / `catálogo` - Lista productos disponibles
- `ayuda` - Muestra ayuda
- `inicio` - Reinicia conversación

### Configuración Meta

**Pasos:**
1. Crear cuenta en [Meta for Developers](https://developers.facebook.com)
2. Crear nueva App (tipo Business)
3. Agregar producto WhatsApp
4. Configurar Webhook:
   - URL: `https://[backend-url]/webhook`
   - Verify Token: (generar token seguro)
5. Suscribir a eventos: `messages`, `messaging_postbacks`
6. Copiar credenciales:
   - Phone Number ID
   - Access Token (permanent o expirable)

## 🧪 Testing

### Health Check

```bash
curl https://[backend-url]/webhook/health
```

**Response esperado:**
```json
{
  "status": "UP",
  "service": "Esmeraldas WhatsApp Webhook"
}
```

### Productos API

```bash
# Listar productos
curl https://[backend-url]/api/products

# Producto por ID
curl https://[backend-url]/api/products/1

# Crear producto
curl -X POST https://[backend-url]/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Esmeralda","description":"Verde","price":2500,...}'
```

## 🔧 Desarrollo

### Build

```bash
# Compilar
mvn clean compile

# Empaquetar
mvn clean package

# Ejecutar tests
mvn test

# Instalar dependencias
mvn clean install
```

### Estructura de Paquetes

```
com.esmeraldas.backend
├── config          # Configuraciones globales
├── controller      # Controladores REST (@RestController)
├── dto            # Data Transfer Objects
├── entity         # Entidades JPA (@Entity)
├── repository     # Repositorios Spring Data
├── service        # Servicios (@Service)
└── webhook        # Servicios WhatsApp
```

## 📝 Notas de Implementación

### Características Implementadas

✅ API REST completa de productos
✅ Integración WhatsApp Cloud API
✅ Webhook funcional con chatbot
✅ CRUD de productos
✅ Búsqueda y filtrado
✅ CORS configurado
✅ Docker multi-stage build
✅ Deployment en Render
✅ Logging configurado

### Próximas Mejoras (Roadmap)

🔮 Fase 2:
- [ ] Autenticación JWT
- [ ] Panel de administración
- [ ] Subida de imágenes desde WhatsApp
- [ ] Categorías dinámicas

🔮 Fase 3:
- [ ] Carrito de compras
- [ ] Pasarela de pagos
- [ ] Sistema de pedidos
- [ ] Notificaciones

## 🐛 Troubleshooting

### Problemas Comunes

**Error: Connection refused**
- Verificar que PostgreSQL esté corriendo
- Confirmar URL de base de datos

**Error: 404 en endpoints**
- Verificar que el backend esté corriendo
- Confirmar CORS configurado

**WhatsApp no responde**
- Verificar Access Token vigente
- Confirmar Webhook URL correcta

### Logs

```bash
# Ver logs en Render
# Dashboard → Service → Logs

# Logs locales
tail -f backend.log
```

## 📄 Licencia

Proprietary - Todos los derechos reservados
© 2026 Victory Esmeraldas - David Montejo

## 🔗 Recursos

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [WhatsApp Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Render Documentation](https://render.com/docs)

---

**Versión**: 1.0.0
**Última actualización**: Febrero 2026
**Autor**: David Montejo
**Estado**: Production ✅
