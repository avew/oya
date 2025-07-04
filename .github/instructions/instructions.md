// COPILOT: Buatlah projek Spring Boot (Java 17+) dengan dependencies:
//   - spring-boot-starter-web
//   - spring-boot-starter-data-jpa
//   - spring-boot-starter-data-redis
//   - postgresql
//   - commons-fileupload (untuk parsing PDF/Excel)
//   - openai-java-client (atau okhttp + jackson untuk panggilan REST ke OpenAI)
//   - Tika-core & Tika-parsers (untuk ekstraksi teks dari PDF/Excel)
//   - spring-boot-starter-validation

// COPILOT: Buat file application.yml dengan konfigurasi:
// spring:
//   datasource:
//     url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
//     username: ${DB_USER}
//     password: ${DB_PASS}
//   jpa:
//     hibernate:
//       ddl-auto: update
//     properties:
//       hibernate.jdbc.lob.non_contextual_creation: true
//   redis:
//     host: ${REDIS_HOST:localhost}
//     port: ${REDIS_PORT:6379}
// openai:
//   api-key: ${OPENAI_API_KEY}
//   model: gpt-3.5-turbo

// COPILOT: Tambahkan dependency Flyway di pom.xml:
//   <dependency>
//     <groupId>org.flywaydb</groupId>
//     <artifactId>flyway-core</artifactId>
//   </dependency>

// COPILOT: Di application.yml, konfigurasi Flyway:
// spring:
//   flyway:
//     enabled: true
//     locations: classpath:db/migration
//     baseline-on-migrate: true
//     # optional: setujuan schema history table, misal:
//     # table: flyway_schema_history

// COPILOT: Buat folder resources/db/migration dan file migration V1__init_schema.sql:
//   -- V1__init_schema.sql
//   CREATE TABLE document (
//       id UUID PRIMARY KEY,
//       filename VARCHAR(255) NOT NULL,
//       content TEXT NOT NULL,
//       embedding BYTEA,
//       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
//   );
//   CREATE INDEX idx_document_embedding ON document USING ivfflat (embedding) WITH (lists = 100);
//   -- buat tabel lain sesuai kebutuhan

// COPILOT: Setiap perubahan skema selanjutnya buat file V2__xxx.sql di folder yang sama.

// COPILOT: Flyway otomatis dijalankan saat aplikasi Spring Boot start, melakukan migrasi dan baseline.


// COPILOT: Buat Entity `Document`:
//   - id (UUID)
//   - filename (String)
//   - content (TEXT)
//   - embedding (vector<float> atau bytea, tergantung extension Postgres)
//   - createdAt (Timestamp)

// COPILOT: Buat Repository `DocumentRepository extends JpaRepository<Document, UUID>`
// COPILOT: Tambahkan method custom untuk vector similarity search (native query dengan <-> operator)

// COPILOT: Buat service `DocumentService` dengan method:
//   - `storeFile(MultipartFile file)`:
//       • detect jenis file (extension / content-type)  
//       • ekstrak teks via Apache Tika  
//       • panggil OpenAI Embedding API untuk setiap chunk teks  
//       • simpan Document + embedding ke PostgreSQL

// COPILOT: Buat Controller `UploadController` dengan endpoint POST `/api/v1/documents`:
//   - consumes: multipart/form-data
//   - parameter: MultipartFile file
//   - validasi ukuran & jenis file
//   - panggil `DocumentService.storeFile(...)`
//   - kembalikan status dan metadata document

// COPILOT: Buat DTO `ChatRequest`:
//   - userId (String/UUID)
//   - message (String)

// COPILOT: Buat DTO `ChatResponse`:
//   - reply (String)

// COPILOT: Buat service `ChatService` dengan method:
//   - `chat(userId, message)`:
//       • ambil history percakapan dari Redis (List<String> atau List<ChatEntry>)  
//       • simpan message user ke history  
//       • lakukan semantic search ke `DocumentRepository` untuk konteks  
//       • bangun payload ke OpenAI ChatCompletion:
//           – system prompt + top-k dokumen + conversation history + current message  
//       • panggil ChatCompletion API  
//       • simpan response assistant ke Redis history  
//       • kembalikan text response

// COPILOT: Buat Controller `ChatController` dengan endpoint POST `/api/v1/chat`:
//   - consumes: application/json
//   - body: ChatRequest
//   - validasi payload (@Valid)
//   - panggil `ChatService.chat(...)`
//   - kembalikan ChatResponse

// COPILOT: Konfigurasikan RedisTemplate<String, Object> untuk menyimpan List<ChatEntry> per user:
//   - gunakan key pattern: "chat:history:{userId}"
//   - TTL optional (misal history 1 hari)

// COPILOT: Tambahkan Swagger/OpenAPI untuk mendokumentasikan endpoint `/documents` dan `/chat`

// COPILOT: Buat kelas util untuk memecah teks besar menjadi chunks (max ~1000 token),
// sehingga embedding tidak melebihi batas OpenAI.

// COPILOT: Tambahkan exception handling global (@ControllerAdvice) untuk menangani:
//   - FileSizeLimitExceededException
//   - HttpMessageNotReadableException
//   - OpenAI API errors

// COPILOT: Tuliskan unit test untuk:
//   - DocumentService.storeFile → memastikan content tersimpan & embedding tidak null
//   - ChatService.chat → mocking Redis & OpenAI client

// COPILOT: Pada CI/CD (misal GitHub Actions):
//   - siapkan secret DB & OPENAI_API_KEY
//   - jalankan build & test
//   - deploy ke Heroku / AWS Elastic Beanstalk / Docker Registry

// COPILOT: Buat Dockerfile multi-stage:
//   - stage build: mvn package -DskipTests
//   - stage runtime: copy jar, expose port 8080, ENTRYPOINT java -jar app.jar

// COPILOT: Buat docker-compose.yml untuk pengembangan lokal:
//   services:
//     app:
//       build: .
//       ports: ["8080:8080"]
//       environment:
//         - SPRING_PROFILES_ACTIVE=dev
//     postgres:
//       image: postgres:15
//       environment:
//         - POSTGRES_DB=app
//         - POSTGRES_USER=app
//         - POSTGRES_PASSWORD=secret
//     redis:
//       image: redis:7

// COPILOT: Dokumentasikan semua langkah di README.md:
//   1. Prasyarat (Java 17+, Maven, Docker, Docker Compose)  
//   2. Cara build & run lokal  
//   3. Endpoints & contoh request/response  
//   4. Cara deploy & environment variables  

