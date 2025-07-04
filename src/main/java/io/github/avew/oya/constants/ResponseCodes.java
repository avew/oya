package io.github.avew.oya.constants;

/**
 * Constants class for API response codes
 * This class contains all success and error codes used throughout the application
 */
public final class ResponseCodes {

    private ResponseCodes() {
        // Private constructor to prevent instantiation
    }

    // File Error Codes
    public static final class FileError {
        public static final String FILE_EMPTY = "FILE_ERROR_0001";
        public static final String FILENAME_NULL = "FILE_ERROR_0002";
        public static final String SIZE_EXCEEDED = "FILE_ERROR_0003";
        public static final String UNSUPPORTED_TYPE = "FILE_ERROR_0004";
    }

    // Document Error Codes
    public static final class DocumentError {
        public static final String NOT_FOUND = "DOCUMENT_ERROR_0001";
        public static final String PROCESSING_FAILED = "DOCUMENT_ERROR_0002";
        public static final String UPLOAD_FAILED = "DOCUMENT_ERROR_0003";
        public static final String CHUNKS_RETRIEVAL_FAILED = "DOCUMENT_ERROR_0004";
        public static final String SEARCH_FAILED = "DOCUMENT_ERROR_0005";
    }

    // Document Success Codes
    public static final class DocumentSuccess {
        public static final String FILE_UPLOADED = "DOCUMENT_SUCCESS_0001";
        public static final String DOCUMENTS_RETRIEVED = "DOCUMENT_SUCCESS_0002";
        public static final String DOCUMENT_FOUND = "DOCUMENT_SUCCESS_0003";
        public static final String SEARCH_COMPLETED = "DOCUMENT_SUCCESS_0004";
        public static final String CHUNKS_RETRIEVED = "DOCUMENT_SUCCESS_0005";
        public static final String DOCUMENTS_FILTERED = "DOCUMENT_SUCCESS_0006";
    }

    // Chat Error Codes
    public static final class ChatError {
        public static final String PROCESSING_FAILED = "CHAT_ERROR_0001";
        public static final String OPENAI_NOT_CONFIGURED = "CHAT_ERROR_0002";
    }

    // Chat Success Codes
    public static final class ChatSuccess {
        public static final String RESPONSE_GENERATED = "CHAT_SUCCESS_0001";
        public static final String SERVICE_HEALTHY = "CHAT_SUCCESS_0002";
    }

    // Validation Error Codes
    public static final class ValidationError {
        public static final String VALIDATION_FAILED = "VALIDATION_ERROR_0001";
        public static final String INVALID_STATUS = "VALIDATION_ERROR_0002";
    }

    // System Error Codes
    public static final class SystemError {
        public static final String INTERNAL_SERVER_ERROR = "SYSTEM_ERROR_0001";
        public static final String UNAUTHORIZED = "SYSTEM_ERROR_0002";
        public static final String FORBIDDEN = "SYSTEM_ERROR_0003";
        public static final String METHOD_NOT_ALLOWED = "SYSTEM_ERROR_0004";
        public static final String UNSUPPORTED_MEDIA_TYPE = "SYSTEM_ERROR_0005";
    }
}
