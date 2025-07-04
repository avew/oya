# Oya - AI-Powered Document Chat System

A Spring Boot application that allows users to upload documents (PDF, Excel, Word) and chat with an AI assistant that can answer questions based on the uploaded content using vector search technology.

## Features

- **Document Upload**: Support for PDF, Excel, Word, and text files (up to 50MB)
- **Text Extraction**: Uses Apache Tika to extract text from various document formats
- **Vector Search**: pgvector integration for semantic document search
- **AI Integration**: OpenAI GPT integration for intelligent responses with embeddings
- **Chat History**: Redis-based conversation history management
- **Internationalization**: Support for English and Indonesian languages
- **RESTful API**: Clean REST endpoints with standardized response format
- **Error Handling**: Comprehensive error handling with specific error codes

## Technology Stack

- **Backend**: Spring Boot 3.5.3, Java 21
- **Database**: PostgreSQL with pgvector extension, Flyway migrations
- **Cache**: Redis for chat history
- **AI**: OpenAI GPT-3.5-turbo and text-embedding-ada-002
- **Document Processing**: Apache Tika for text extraction
- **Error Handling**: Zalando Problem Spring Web
- **Build Tool**: Maven

## Prerequisites

- Java 21+
- PostgreSQL database with pgvector extension
- Redis server
- OpenAI API key

## Configuration

Create environment variables or update `application.yml`:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=oya
DB_USER=postgres
DB_PASS=your_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here

# File Upload Directory
UPLOAD_DIR=./uploads
```

## Running the Application

1. **Start PostgreSQL with pgvector extension**
   ```bash
   # Using Docker
   docker run --name postgres \
     -e POSTGRES_DB=oya \
     -e POSTGRES_PASSWORD=password \
     -p 5432:5432 \
     -d pgvector/pgvector:pg16
   ```

2. **Start Redis**
   ```bash
   docker run --name redis -p 6379:6379 -d redis
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080`

## API Documentation

### Standard Response Format

All API responses follow a consistent format:

```json
{
  "status": 0,
  "code": "DOCUMENT_SUCCESS_0001",
  "message": "File uploaded successfully and is being processed",
  "data": {
    // Response payload here
  }
}
```

**Fields:**
- `status`: Integer (0 = success, 1 = error)
- `code`: Specific response code for identification
- `message`: Localized human-readable message
- `data`: Response payload (present for success, null for errors)

### Success Response Example

```json
{
  "status": 0,
  "code": "DOCUMENT_SUCCESS_0001",
  "message": "File uploaded successfully and is being processed",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "filename": "document.pdf",
    "contentType": "application/pdf",
    "fileSize": 1024000,
    "status": "PROCESSING",
    "createdAt": "2025-07-05T00:15:06"
  }
}
```

### Error Response Example

```json
{
  "status": 1,
  "code": "FILE_ERROR_0001",
  "message": "File cannot be empty",
  "data": null
}
```

## API Endpoints

### Document Management

#### Upload Document
```http
POST /api/v1/documents
Content-Type: multipart/form-data

curl -X POST -F "file=@document.pdf" \
  -H "Accept-Language: en" \
  http://localhost:8080/api/v1/documents
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0001`
- Message: "File uploaded successfully and is being processed"

**Possible Errors:**
- `FILE_ERROR_0001`: File cannot be empty
- `FILE_ERROR_0002`: Filename cannot be null
- `FILE_ERROR_0003`: File size exceeds 50MB limit
- `FILE_ERROR_0004`: Unsupported file type

#### Get All Documents
```http
GET /api/v1/documents
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0002`
- Message: "Documents retrieved successfully"

#### Get Document by ID
```http
GET /api/v1/documents/{id}
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0003`
- Message: "Document found successfully"

**Possible Errors:**
- `DOCUMENT_ERROR_0001`: Document not found with ID

#### Search Documents
```http
GET /api/v1/documents/search?keyword=your_keyword&limit=10
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0004`
- Message: "Search completed successfully"

#### Get Document Chunks
```http
GET /api/v1/documents/{id}/chunks
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0005`
- Message: "Document chunks retrieved successfully"

#### Filter Documents by Status
```http
GET /api/v1/documents/status/{status}
```

**Success Response:**
- Code: `DOCUMENT_SUCCESS_0006`
- Message: "Documents filtered by status successfully"

**Possible Errors:**
- `VALIDATION_ERROR_0002`: Invalid status value

### Chat Management

#### Send Chat Message
```http
POST /api/v1/chat
Content-Type: application/json

{
  "userId": "user123",
  "message": "What does the document say about..."
}
```

**Success Response:**
- Code: `CHAT_SUCCESS_0001`
- Message: "Chat response generated successfully"

**Possible Errors:**
- `CHAT_ERROR_0001`: Failed to process chat request
- `CHAT_ERROR_0002`: OpenAI API key not configured
- `VALIDATION_ERROR_0001`: Validation failed

#### Health Check
```http
GET /api/v1/chat/health
```

**Success Response:**
- Code: `CHAT_SUCCESS_0002`
- Message: "Chat service is running"

## Response Codes Reference

### File Error Codes
| Code | Description |
|------|-------------|
| `FILE_ERROR_0001` | File cannot be empty |
| `FILE_ERROR_0002` | Filename cannot be null |
| `FILE_ERROR_0003` | File size exceeds 50MB limit |
| `FILE_ERROR_0004` | Unsupported file type |

### Document Error Codes
| Code | Description |
|------|-------------|
| `DOCUMENT_ERROR_0001` | Document not found with ID |
| `DOCUMENT_ERROR_0002` | Failed to process document |
| `DOCUMENT_ERROR_0003` | Failed to upload document |
| `DOCUMENT_ERROR_0004` | Failed to retrieve document chunks |
| `DOCUMENT_ERROR_0005` | Document search failed |

### Document Success Codes
| Code | Description |
|------|-------------|
| `DOCUMENT_SUCCESS_0001` | File uploaded successfully and is being processed |
| `DOCUMENT_SUCCESS_0002` | Documents retrieved successfully |
| `DOCUMENT_SUCCESS_0003` | Document found successfully |
| `DOCUMENT_SUCCESS_0004` | Search completed successfully |
| `DOCUMENT_SUCCESS_0005` | Document chunks retrieved successfully |
| `DOCUMENT_SUCCESS_0006` | Documents filtered by status successfully |

### Chat Error Codes
| Code | Description |
|------|-------------|
| `CHAT_ERROR_0001` | Failed to process chat request |
| `CHAT_ERROR_0002` | OpenAI API key not configured |

### Chat Success Codes
| Code | Description |
|------|-------------|
| `CHAT_SUCCESS_0001` | Chat response generated successfully |
| `CHAT_SUCCESS_0002` | Chat service is running |

### Validation Error Codes
| Code | Description |
|------|-------------|
| `VALIDATION_ERROR_0001` | Validation failed |
| `VALIDATION_ERROR_0002` | Invalid status value |

### System Error Codes
| Code | Description |
|------|-------------|
| `SYSTEM_ERROR_0001` | Internal server error occurred |
| `SYSTEM_ERROR_0002` | Unauthorized access |
| `SYSTEM_ERROR_0003` | Access forbidden |
| `SYSTEM_ERROR_0004` | Method not allowed |
| `SYSTEM_ERROR_0005` | Unsupported media type |

## Internationalization

The API supports multiple languages through the `Accept-Language` header:

**English (default):**
```bash
curl -H "Accept-Language: en" http://localhost:8080/api/v1/documents
```

**Indonesian:**
```bash
curl -H "Accept-Language: id" http://localhost:8080/api/v1/documents
```

Response messages will automatically be localized based on the requested language.

## Database Schema

The application uses separate tables for better performance:

### Document Metadata Table
- `id` (UUID) - Primary key
- `filename` (VARCHAR) - Original filename
- `content_type` (VARCHAR) - MIME type
- `file_size` (BIGINT) - File size in bytes
- `upload_path` (VARCHAR) - File storage path
- `status` (VARCHAR) - Processing status (PROCESSING/COMPLETED/FAILED)
- `created_at`, `updated_at` (TIMESTAMP) - Audit fields

### Document Chunks Table
- `id` (UUID) - Primary key
- `document_id` (UUID) - Foreign key to document
- `chunk_index` (INTEGER) - Chunk sequence number
- `content` (TEXT) - Extracted text content
- `embedding` (vector(1536)) - OpenAI embedding vector
- `token_count` (INTEGER) - Approximate token count
- `created_at` (TIMESTAMP) - Creation timestamp

### Chat History Table
- `id` (UUID) - Primary key
- `user_id` (VARCHAR) - User identifier
- `message` (TEXT) - User message
- `response` (TEXT) - AI response
- `created_at` (TIMESTAMP) - Creation timestamp

## Vector Search

The application uses pgvector for semantic document search:

1. **Document Processing**: Text is extracted and split into chunks
2. **Embedding Generation**: Each chunk is converted to 1536-dimensional vectors using OpenAI
3. **Vector Storage**: Embeddings are stored in PostgreSQL with pgvector
4. **Semantic Search**: User queries are converted to vectors and matched using cosine similarity
5. **Hybrid Search**: Combines vector similarity with traditional text search for better results

## Error Handling

The application uses Zalando Problem Spring Web for standardized error handling:

- **Consistent Format**: All errors follow RFC 7807 Problem Details standard
- **Specific Codes**: Each error type has a unique identifier
- **Localized Messages**: Error messages are internationalized
- **Proper HTTP Status**: Appropriate HTTP status codes are returned
- **Logging**: All errors are properly logged with context

## Development Notes

- **File Size Limit**: 50MB maximum per upload
- **Supported Formats**: PDF, Excel (.xls, .xlsx), Word (.doc, .docx), Plain text
- **Chat History**: Stored in Redis with 24-hour expiration
- **Vector Dimensions**: 1536 (OpenAI ada-002 standard)
- **Chunk Size**: ~1000 tokens with 200 token overlap
- **CORS**: Configured for frontend development

## Contributing

1. Follow the established response code patterns in `ResponseCodes.java`
2. Add appropriate internationalization messages for new features
3. Ensure all new endpoints follow the standard response format
4. Write comprehensive tests for new functionality
5. Update this documentation for any API changes

## License

This project is licensed under the MIT License.
