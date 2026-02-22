# Esmeraldas Backend

Backend del e-commerce de Esmeraldas con integración de WhatsApp Cloud API.

## Tecnologías

- Spring Boot 3.2.0
- PostgreSQL
- JPA/Hibernate
- WhatsApp Cloud API
- Maven

## Configuración de Base de Datos

### Opción 1: PostgreSQL Local

1. Instala PostgreSQL: `brew install postgresql` (Mac)
2. Inicia PostgreSQL: `brew services start postgresql`
3. Crea la base de datos:
```bash
psql postgres
CREATE DATABASE esmeraldas_db;
CREATE USER esmeraldas_user WITH PASSWORD 'esmeraldas_pass';
GRANT ALL PRIVILEGES ON DATABASE esmeraldas_db TO esmeraldas_user;
\q
```

### Opción 2: Supabase (Recomendado - Gratis)

1. Ve a [supabase.com](https://supabase.com)
2. Crea un proyecto nuevo
3. Ve a Settings > Database
4. Copia la **Connection String** JDBC
5. Actualiza `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://YOUR_PROJECT.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### Opción 3: ElephantSQL (Gratis)

1. Ve a [elephantsql.com](https://www.elephantsql.com)
2. Crea una instancia gratuita
3. Copia la URL de conexión
4. Actualiza `application.properties`

## Ejecutar el Proyecto

```bash
cd backend

# Con Maven
mvn clean install
mvn spring-boot:run
```

El backend estará disponible en: `http://localhost:8080`

## API Endpoints

### Productos

- `GET /api/products` - Listar todos los productos
- `GET /api/products/{id}` - Obtener producto por ID
- `POST /api/products` - Crear producto
- `PUT /api/products/{id}` - Actualizar producto
- `DELETE /api/products/{id}` - Eliminar producto
- `GET /api/products/search?keyword=xxx` - Buscar productos
- `GET /api/products/category/{category}` - Filtrar por categoría

### Webhook de WhatsApp

- `GET /webhook` - Verificación de webhook
- `POST /webhook` - Recepción de mensajes de WhatsApp
- `GET /webhook/test` - Test endpoint
- `GET /webhook/health` - Health check

## Configuración de WhatsApp Cloud API

### 1. Crear App en Meta for Developers

1. Ve a [developers.facebook.com](https://developers.facebook.com)
2. Crea una nueva app > **Business** type
3. Agrega el producto **WhatsApp**

### 2. Configurar Webhook

1. En WhatsApp > Configuration, haz clic en "Edit" en Webhooks
2. Callback URL: `https://tu-backend-url.com/webhook`
3. Verify Token: Genera uno seguro y guárdalo
4. Suscríbete a los eventos:
   - `messages`
   - `messaging_postbacks`

### 3. Obtener Credenciales

1. Ve a WhatsApp > API Setup
2. Copia:
   - **Phone Number ID**
   - **Access Token** (con permisos de lectura y envío)

### 4. Actualizar application.properties

```properties
whatsapp.phone.number.id=YOUR_PHONE_NUMBER_ID
whatsapp.access.token=YOUR_ACCESS_TOKEN
whatsapp.verify.token=YOUR_VERIFY_TOKEN
```

### 5. Probar el Webhook

1. Inicia el backend localmente
2. Usa ngrok para exponer tu localhost: `ngrok http 8080`
3. Configura el webhook con la URL de ngrok
4. Envía un mensaje desde WhatsApp al número de prueba

## Estructura del Proyecto

```
backend/
├── src/main/java/com/esmeraldas/backend/
│   ├── entity/           # Entidades JPA
│   ├── repository/       # Repositorios JPA
│   ├── service/          # Lógica de negocio
│   ├── controller/       # Controladores REST
│   ├── webhook/          # Servicio de WhatsApp
│   ├── dto/              # DTOs para transferencia de datos
│   └── config/           # Configuraciones
└── src/main/resources/
    └── application.properties
```

## Comandos Útiles

```bash
# Compilar proyecto
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar para producción
mvn clean package

# Ejecutar JAR
java -jar target/backend-1.0.0.jar
```

## Deploy en Render (Gratis)

1. Crea un archivo `render.yaml` en la raíz del proyecto
2. Conecta tu repo de GitHub
3. Render detectará automáticamente Spring Boot
4. Configura las variables de entorno
5. Deploy automático en cada push

Variables de entorno necesarias:
- `DATABASE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `WHATSAPP_PHONE_NUMBER_ID`
- `WHATSAPP_ACCESS_TOKEN`
- `WHATSAPP_VERIFY_TOKEN`
