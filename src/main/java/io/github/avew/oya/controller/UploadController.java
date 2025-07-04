package io.github.avew.oya.controller;

import io.github.avew.oya.constants.ResponseCodes;
import io.github.avew.oya.dto.ApiResponse;
import io.github.avew.oya.entity.Document;
import io.github.avew.oya.entity.DocumentChunk;
import io.github.avew.oya.exception.DocumentNotFoundException;
import io.github.avew.oya.service.DocumentService;
import io.github.avew.oya.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final DocumentService documentService;
    private final MessageService messageService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(@RequestParam("file") MultipartFile file) {
        Document document = documentService.storeFile(file);

        Map<String, Object> documentData = Map.of(
            "id", document.getId(),
            "filename", document.getFilename(),
            "contentType", document.getContentType(),
            "fileSize", document.getFileSize(),
            "status", document.getStatus().name(),
            "createdAt", document.getCreatedAt()
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
            ResponseCodes.DocumentSuccess.FILE_UPLOADED,
            messageService.getMessage(ResponseCodes.DocumentSuccess.FILE_UPLOADED),
            documentData
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Document>>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        ApiResponse<List<Document>> response = ApiResponse.success(
            ResponseCodes.DocumentSuccess.DOCUMENTS_RETRIEVED,
            messageService.getMessage(ResponseCodes.DocumentSuccess.DOCUMENTS_RETRIEVED),
            documents
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Document>> getDocumentById(@PathVariable UUID id) {
        Document document = documentService.getDocumentById(id);
        if (document != null) {
            ApiResponse<Document> response = ApiResponse.success(
                ResponseCodes.DocumentSuccess.DOCUMENT_FOUND,
                messageService.getMessage(ResponseCodes.DocumentSuccess.DOCUMENT_FOUND),
                document
            );
            return ResponseEntity.ok(response);
        } else {
            throw new DocumentNotFoundException(id.toString());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Document>>> searchDocuments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        List<Document> documents = documentService.searchDocuments(keyword, limit);
        ApiResponse<List<Document>> response = ApiResponse.success(
            ResponseCodes.DocumentSuccess.SEARCH_COMPLETED,
            messageService.getMessage(ResponseCodes.DocumentSuccess.SEARCH_COMPLETED),
            documents
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<ApiResponse<List<DocumentChunk>>> getDocumentChunks(@PathVariable UUID id) {
        List<DocumentChunk> chunks = documentService.getDocumentChunks(id);
        ApiResponse<List<DocumentChunk>> response = ApiResponse.success(
            ResponseCodes.DocumentSuccess.CHUNKS_RETRIEVED,
            messageService.getMessage(ResponseCodes.DocumentSuccess.CHUNKS_RETRIEVED),
            chunks
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Document>>> getDocumentsByStatus(@PathVariable String status) {
        try {
            Document.DocumentStatus documentStatus = Document.DocumentStatus.valueOf(status.toUpperCase());
            List<Document> documents = documentService.getDocumentsByStatus(documentStatus);
            ApiResponse<List<Document>> response = ApiResponse.success(
                ResponseCodes.DocumentSuccess.DOCUMENTS_FILTERED,
                messageService.getMessage(ResponseCodes.DocumentSuccess.DOCUMENTS_FILTERED),
                documents
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                messageService.getMessage(ResponseCodes.ValidationError.INVALID_STATUS, new Object[]{status})
            );
        }
    }
}
