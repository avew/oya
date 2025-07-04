package io.github.avew.oya.repository;

import io.github.avew.oya.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {

    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT ch FROM ChatHistory ch WHERE ch.userId = :userId ORDER BY ch.createdAt DESC")
    List<ChatHistory> findRecentChatHistory(@Param("userId") String userId);

    @Query(value = "SELECT * FROM chat_history WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatHistory> findRecentChatHistoryWithLimit(@Param("userId") String userId, @Param("limit") int limit);
}
