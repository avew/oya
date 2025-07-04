package io.github.avew.oya.repository;

import io.github.avew.oya.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByFilenameContainingIgnoreCase(String filename);

    List<Document> findByStatus(Document.DocumentStatus status);

    List<Document> findByContentTypeContainingIgnoreCase(String contentType);

    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt DESC")
    List<Document> findByStatusOrderByCreatedAtDesc(@Param("status") Document.DocumentStatus status);

    @Query("SELECT d FROM Document d JOIN d.chunks dc WHERE dc.content ILIKE %:keyword% GROUP BY d.id")
    List<Document> findDocumentsWithContentKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(dc) FROM Document d JOIN d.chunks dc WHERE d.id = :documentId")
    Long countChunksByDocumentId(@Param("documentId") UUID documentId);
}

