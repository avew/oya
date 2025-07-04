package io.github.avew.oya.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import io.github.avew.oya.constants.ResponseCodes;
import io.github.avew.oya.entity.Document;
import io.github.avew.oya.entity.DocumentChunk;
import io.github.avew.oya.exception.DocumentProcessingException;
import io.github.avew.oya.exception.FileValidationException;
import io.github.avew.oya.repository.DocumentRepository;
import io.github.avew.oya.repository.DocumentChunkRepository;
import io.github.avew.oya.dto.DocumentSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final MessageService messageService;
    private final Tika tika = new Tika();

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final int MAX_CHUNK_SIZE = 1000; // tokens per chunk
    private static final int CHUNK_OVERLAP = 200; // overlap between chunks

    public Document storeFile(MultipartFile file) throws IOException {
        log.info("Processing file: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Save file to disk
        String uploadPath = saveFileToStorage(file);

        // Create document metadata
        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadPath(uploadPath)
                .status(Document.DocumentStatus.PROCESSING)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Document metadata saved with ID: {}", savedDocument.getId());

        // Process document content asynchronously
        CompletableFuture.runAsync(() -> processDocumentContent(savedDocument, file));

        return savedDocument;
    }

    private void processDocumentContent(Document document, MultipartFile file) {
        try {
            // Extract text using Apache Tika
            String content = extractTextFromFile(file);
            log.debug("Processing file: {}", file.getOriginalFilename());
            // Split content into chunks
            List<String> chunks = splitIntoChunks(content);
            log.debug("Split content into {} chunks", chunks.size());
            // Process each chunk
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                log.debug("Processing chunk: {}", chunkContent);
                // Generate embedding as vector string for pgvector
                String embedding = generateVectorEmbedding(chunkContent);

                // Calculate token count (approximate)
                int tokenCount = estimateTokenCount(chunkContent);

                // Create document chunk
                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunkContent)
                        .embedding(embedding)
                        .tokenCount(tokenCount)
                        .build();

                documentChunkRepository.save(chunk);
            }

            // Update document status
            document.setStatus(Document.DocumentStatus.COMPLETED);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);

            log.info("Document processing completed for ID: {}, chunks: {}",
                    document.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Error processing document content for ID: {}", document.getId(), e);
            document.setStatus(Document.DocumentStatus.FAILED);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
        }
    }

    public List<DocumentChunk> searchDocumentChunks(String keyword, int limit) {
        try {
            // Generate embedding for the search query
            String queryEmbedding = generateVectorEmbedding(keyword);

            if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
                // Use vector similarity search
                return documentChunkRepository.findSimilarChunksByCosineDistance(queryEmbedding, limit);
            } else {
                // Fallback to text search if embedding generation fails
                log.warn("Vector embedding failed, falling back to text search");
                return documentChunkRepository.findByContentContaining(keyword, limit);
            }
        } catch (Exception e) {
            log.warn("Vector search failed, falling back to text search", e);
            return documentChunkRepository.findByContentContaining(keyword, limit);
        }
    }

    public List<DocumentChunk> searchDocumentChunksWithHybrid(String query, int limit) {
        try {
            String queryEmbedding = generateVectorEmbedding(query);

            if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
                return documentChunkRepository.findSimilarChunksByHybridSearch(queryEmbedding, query, limit);
            } else {
                return documentChunkRepository.findByContentContaining(query, limit);
            }
        } catch (Exception e) {
            log.warn("Hybrid search failed, falling back to text search", e);
            return documentChunkRepository.findByContentContaining(query, limit);
        }
    }

    public List<DocumentSearchResult> searchDocumentChunksWithScores(String query, int limit) {
        try {
            String queryEmbedding = generateVectorEmbedding(query);
            log.debug("Searching for documents with scores: {}", queryEmbedding);
            if (queryEmbedding != null && !queryEmbedding.isEmpty()) {
                // Get raw results from database
                List<Object[]> rawResults = documentChunkRepository.findSimilarChunksByHybridSearchWithScoresRaw(queryEmbedding, query, limit);
                // Convert raw results to DocumentSearchResult objects
                // Array structure: [0]=id, [1]=document_id, [2]=chunk_index, [3]=content, [4]=token_count, [5]=created_at,
                //                  [6]=vector_similarity, [7]=text_rank, [8]=hybrid_score
                return rawResults.stream().map(row -> {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setId(UUID.fromString(row[0].toString()));
                    chunk.setChunkIndex((Integer) row[2]);
                    chunk.setContent((String) row[3]);
                    chunk.setEmbedding(null); // embedding tidak diperlukan untuk response
                    chunk.setTokenCount((Integer) row[4]);

                    // We need to fetch the document separately since it's not fully loaded
                    UUID documentId = UUID.fromString(row[1].toString());
                    Document document = documentRepository.findById(documentId).orElse(null);
                    chunk.setDocument(document);

                    return DocumentSearchResult.builder()
                            .documentChunk(chunk)
                            .vectorSimilarity(((Number) row[6]).doubleValue())
                            .textRank(((Number) row[7]).doubleValue())
                            .hybridScore(((Number) row[8]).doubleValue())
                            .searchMethod("hybrid_search")
                            .build();
                }).toList();
            } else {
                // Fallback to text search without scores
                List<DocumentChunk> chunks = documentChunkRepository.findByContentContaining(query, limit);
                return chunks.stream()
                        .map(chunk -> DocumentSearchResult.builder()
                                .documentChunk(chunk)
                                .vectorSimilarity(null)
                                .textRank(null)
                                .hybridScore(null)
                                .searchMethod("text_search")
                                .build())
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Hybrid search with scores failed, falling back to text search", e);
            List<DocumentChunk> chunks = documentChunkRepository.findByContentContaining(query, limit);
            return chunks.stream()
                    .map(chunk -> DocumentSearchResult.builder()
                            .documentChunk(chunk)
                            .vectorSimilarity(null)
                            .textRank(null)
                            .hybridScore(null)
                            .searchMethod("text_search_fallback")
                            .build())
                    .toList();
        }
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public List<Document> getCompletedDocuments() {
        return documentRepository.findByStatus(Document.DocumentStatus.COMPLETED);
    }

    public Document getDocumentById(UUID id) {
        return documentRepository.findById(id).orElse(null);
    }

    public List<DocumentChunk> getDocumentChunks(UUID documentId) {
        return documentChunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
    }

    public List<Document> getDocumentsByStatus(Document.DocumentStatus status) {
        return documentRepository.findByStatus(status);
    }

    public List<Document> searchDocuments(String keyword, int limit) {
        return documentRepository.findDocumentsWithContentKeyword(keyword);
    }

    private String saveFileToStorage(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = content.split("\\. ");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokenCount = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokenCount(sentence);

            if (currentTokenCount + sentenceTokens > MAX_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap
                String overlap = getLastNTokens(currentChunk.toString(), CHUNK_OVERLAP);
                currentChunk = new StringBuilder(overlap);
                currentTokenCount = estimateTokenCount(overlap);
            }

            currentChunk.append(sentence).append(". ");
            currentTokenCount += sentenceTokens;
        }

        // Add the last chunk if it has content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private String getLastNTokens(String text, int n) {
        String[] words = text.split("\\s+");
        int start = Math.max(0, words.length - n);
        return String.join(" ", Arrays.copyOfRange(words, start, words.length));
    }

    private int estimateTokenCount(String text) {
        // Simple approximation: ~4 characters per token
        return text.length() / 4;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException(messageService.getMessage(ResponseCodes.FileError.FILE_EMPTY));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileValidationException(messageService.getMessage(ResponseCodes.FileError.FILENAME_NULL));
        }

        // Check file size (50MB limit)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new FileValidationException(
                messageService.getMessage(ResponseCodes.FileError.SIZE_EXCEEDED, new Object[]{"50"})
            );
        }

        // Check file type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
        );

        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new FileValidationException(
                messageService.getMessage(ResponseCodes.FileError.UNSUPPORTED_TYPE, new Object[]{contentType})
            );
        }
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        try {
            String extractedText = tika.parseToString(file.getInputStream());
            log.debug("Extracted text length: {}", extractedText.length());
            return extractedText;
        } catch (Exception e) {
            log.error("Error extracting text from file: {}", file.getOriginalFilename(), e);
            throw new DocumentProcessingException(
                messageService.getMessage(ResponseCodes.DocumentError.PROCESSING_FAILED), e
            );
        }
    }

    private String generateVectorEmbedding(String text) {
        try {
            if (openAiApiKey == null || openAiApiKey.equals("your-api-key-here")) {
                log.warn("OpenAI API key not configured, returning null embedding");
                return null;
            }

            OpenAiService openAiService = new OpenAiService(openAiApiKey);

            // Create embedding request
            EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                    .model(embeddingModel)
                    .input(List.of(text))
                    .build();

            var result = openAiService.createEmbeddings(embeddingRequest);

            if (result.getData() != null && !result.getData().isEmpty()) {
                // Convert embedding to pgvector format [1.0, 2.0, 3.0, ...]
                var embedding = result.getData().get(0).getEmbedding();
                return formatEmbeddingForPgVector(embedding);
            }

            return null;
        } catch (Exception e) {
            log.error("Error generating vector embedding for text", e);
            return null;
        }
    }

    private String formatEmbeddingForPgVector(List<Double> embedding) {
        // Format as pgvector array: [1.0, 2.0, 3.0, ...]
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embedding.size(); i++) {
            sb.append(embedding.get(i));
            if (i < embedding.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
