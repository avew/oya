package io.github.avew.oya.exception;

import io.github.avew.oya.constants.ResponseCodes;
import io.github.avew.oya.dto.ApiResponse;
import io.github.avew.oya.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleDocumentNotFound(DocumentNotFoundException ex) {
        log.error("Document not found: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.DocumentError.NOT_FOUND,
            messageService.getMessage(ResponseCodes.DocumentError.NOT_FOUND, new Object[]{ex.getDetail()})
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileValidation(FileValidationException ex) {
        log.error("File validation error: {}", ex.getMessage());

        // Determine specific file error code based on message content
        String errorCode = determineFileErrorCode(ex.getDetail() != null ? ex.getDetail() : "");

        ApiResponse<Object> response = ApiResponse.error(
            errorCode,
            ex.getDetail()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiResponse<Object>> handleDocumentProcessing(DocumentProcessingException ex) {
        log.error("Document processing error: {}", ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.DocumentError.PROCESSING_FAILED,
            messageService.getMessage(ResponseCodes.DocumentError.PROCESSING_FAILED)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ChatProcessingException.class)
    public ResponseEntity<ApiResponse<Object>> handleChatProcessing(ChatProcessingException ex) {
        log.error("Chat processing error: {}", ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.ChatError.PROCESSING_FAILED,
            messageService.getMessage(ResponseCodes.ChatError.PROCESSING_FAILED)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());

        String errorCode = determineErrorCodeFromMessage(ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
            errorCode,
            ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.ValidationError.VALIDATION_FAILED,
            messageService.getMessage(ResponseCodes.ValidationError.VALIDATION_FAILED)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.FileError.SIZE_EXCEEDED,
            messageService.getMessage(ResponseCodes.FileError.SIZE_EXCEEDED, new Object[]{"50"})
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
            ResponseCodes.SystemError.INTERNAL_SERVER_ERROR,
            messageService.getMessage(ResponseCodes.SystemError.INTERNAL_SERVER_ERROR)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String determineFileErrorCode(String message) {
        if (message.contains("empty")) {
            return ResponseCodes.FileError.FILE_EMPTY;
        } else if (message.contains("null")) {
            return ResponseCodes.FileError.FILENAME_NULL;
        } else if (message.contains("size")) {
            return ResponseCodes.FileError.SIZE_EXCEEDED;
        } else if (message.contains("type")) {
            return ResponseCodes.FileError.UNSUPPORTED_TYPE;
        }
        return ResponseCodes.FileError.FILE_EMPTY; // default
    }

    private String determineErrorCodeFromMessage(String message) {
        if (message.contains("status")) {
            return ResponseCodes.ValidationError.INVALID_STATUS;
        }
        return ResponseCodes.ValidationError.VALIDATION_FAILED; // default
    }
}
