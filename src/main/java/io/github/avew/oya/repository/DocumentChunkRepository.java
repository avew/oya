package io.github.avew.oya.repository;

import io.github.avew.oya.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    List<DocumentChunk> findChunksByDocumentId(@Param("documentId") UUID documentId);

    // Vector similarity search using pgvector extension
    @Query(value = """
        SELECT dc.* FROM document_chunk dc
        JOIN document d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <-> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksByVector(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);

    // Vector similarity search with cosine distance
    @Query(value = """
        SELECT dc.*, (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) as similarity
        FROM document_chunk dc
        JOIN document d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksByCosineDistance(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);

    // Vector similarity search with L2 distance
    @Query(value = """
        SELECT dc.* FROM document_chunk dc
        JOIN document d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <-> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksByL2Distance(@Param("queryEmbedding") String queryEmbedding, @Param("limit") int limit);

    // Hybrid search: combine vector similarity with keyword matching
    @Query(value = """
        SELECT dc.*,
               (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) as vector_similarity,
               ts_rank(to_tsvector('english', dc.content), plainto_tsquery('english', :keyword)) as text_rank
        FROM document_chunk dc
        JOIN document d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        AND (dc.content ILIKE CONCAT('%', :keyword, '%') OR to_tsvector('english', dc.content) @@ plainto_tsquery('english', :keyword))
        ORDER BY
            (0.7 * (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)))) +
            (0.3 * ts_rank(to_tsvector('english', dc.content), plainto_tsquery('english', :keyword))) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksByHybridSearch(@Param("queryEmbedding") String queryEmbedding, @Param("keyword") String keyword, @Param("limit") int limit);

    // Hybrid search with scores for detailed logging
    @Query(value = """
        SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, dc.token_count, dc.created_at,
               CASE 
                   WHEN dc.embedding IS NOT NULL THEN (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)))
                   ELSE 0.0
               END as vector_similarity,
               ts_rank(to_tsvector('english', dc.content), plainto_tsquery('english', :keyword)) as text_rank,
               CASE 
                   WHEN dc.embedding IS NOT NULL THEN
                       (0.7 * (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)))) +
                       (0.3 * ts_rank(to_tsvector('english', dc.content), plainto_tsquery('english', :keyword)))
                   ELSE
                       ts_rank(to_tsvector('english', dc.content), plainto_tsquery('english', :keyword))
               END as hybrid_score
        FROM document_chunk dc
        JOIN document d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND (
            dc.embedding IS NOT NULL 
            OR dc.content ILIKE CONCAT('%', :keyword, '%') 
            OR to_tsvector('english', dc.content) @@ plainto_tsquery('english', :keyword)
        )
        ORDER BY hybrid_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksByHybridSearchWithScoresRaw(@Param("queryEmbedding") String queryEmbedding, @Param("keyword") String keyword, @Param("limit") int limit);

    // Fallback text search when vector search is not available
    @Query(value = "SELECT * FROM document_chunk WHERE content ILIKE CONCAT('%', :keyword, '%') ORDER BY chunk_index LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findByContentContaining(@Param("keyword") String keyword, @Param("limit") int limit);

    @Query("SELECT dc FROM DocumentChunk dc JOIN dc.document d WHERE d.status = 'COMPLETED' AND dc.content ILIKE CONCAT('%', :keyword, '%')")
    List<DocumentChunk> findCompletedChunksWithKeyword(@Param("keyword") String keyword);

    void deleteByDocumentId(UUID documentId);
}
