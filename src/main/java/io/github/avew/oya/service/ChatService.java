package io.github.avew.oya.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import io.github.avew.oya.dto.ChatRequest;
import io.github.avew.oya.dto.ChatResponse;
import io.github.avew.oya.entity.ChatHistory;
import io.github.avew.oya.entity.Document;
import io.github.avew.oya.entity.DocumentChunk;
import io.github.avew.oya.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final DocumentService documentService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String chatModel;

    private static final String REDIS_HISTORY_KEY = "chat_history:";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int MAX_CONTEXT_DOCUMENTS = 3;

    public ChatResponse chat(ChatRequest request) {
        log.info("Processing chat request for user: {}", request.getUserId());

        try {
            // Get conversation history from Redis
            List<ChatMessage> conversationHistory = getConversationHistory(request.getUserId());

            // Add user message to history
            conversationHistory.add(new ChatMessage(ChatMessageRole.USER.value(), request.getMessage()));

            // Perform semantic search for relevant documents
            List<Document> relevantDocuments = searchRelevantDocuments(request.getMessage());

            // Build context from documents
            String documentContext = buildDocumentContext(relevantDocuments);

            // Create system prompt with context
            String systemPrompt = buildSystemPrompt(documentContext);

            // Prepare messages for OpenAI
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
            messages.addAll(conversationHistory);

            // Call OpenAI Chat Completion
            String aiResponse = callOpenAiChatCompletion(messages);

            // Save conversation to Redis and database
            saveConversationHistory(request.getUserId(), request.getMessage(), aiResponse);

            // Build response
            ChatResponse response = ChatResponse.builder()
                    .reply(aiResponse)
                    .userId(request.getUserId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Chat response generated for user: {}", request.getUserId());
            return response;

        } catch (Exception e) {
            log.error("Error processing chat request for user: {}", request.getUserId(), e);
            return ChatResponse.builder()
                    .reply("Sorry, I encountered an error while processing your request. Please try again.")
                    .userId(request.getUserId())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ChatMessage> getConversationHistory(String userId) {
        String key = REDIS_HISTORY_KEY + userId;
        List<ChatMessage> history = new ArrayList<>();

        try {
            List<Object> redisHistory = redisTemplate.opsForList().range(key, 0, -1);
            if (redisHistory != null) {
                for (Object item : redisHistory) {
                    if (item instanceof ChatMessage) {
                        history.add((ChatMessage) item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error retrieving conversation history from Redis for user: {}", userId, e);
        }

        return history;
    }

    private List<Document> searchRelevantDocuments(String message) {
        try {
            // Use vector search with hybrid approach for better results
            return documentService.searchDocumentChunksWithHybrid(message, MAX_CONTEXT_DOCUMENTS)
                    .stream()
                    .map(DocumentChunk::getDocument)
                    .distinct()
                    .limit(MAX_CONTEXT_DOCUMENTS)
                    .toList();
        } catch (Exception e) {
            log.warn("Error searching relevant documents with vector search, falling back to text search", e);
            // Fallback to regular text search
            try {
                return documentService.searchDocumentChunks(message, MAX_CONTEXT_DOCUMENTS)
                        .stream()
                        .map(DocumentChunk::getDocument)
                        .distinct()
                        .limit(MAX_CONTEXT_DOCUMENTS)
                        .toList();
            } catch (Exception fallbackError) {
                log.warn("Error with fallback search", fallbackError);
                return new ArrayList<>();
            }
        }
    }

    private String buildDocumentContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "No relevant documents found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Relevant documents:\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("Document ").append(i + 1).append(" (")
                   .append(doc.getFilename()).append("):\n");

            // Get the most relevant chunks for this document
            List<DocumentChunk> chunks = documentService.getDocumentChunks(doc.getId());
            chunks.stream()
                    .limit(2) // Limit to 2 chunks per document
                    .forEach(chunk -> {
                        String content = chunk.getContent();
                        if (content.length() > 300) {
                            content = content.substring(0, 300) + "...";
                        }
                        context.append(content).append("\n");
                    });
            context.append("\n");
        }

        return context.toString();
    }

    private String buildSystemPrompt(String documentContext) {
        return """
                You are an AI assistant that helps users by answering questions based on uploaded documents.
                
                Your role:
                1. Answer questions using the provided document context when relevant
                2. If the context doesn't contain relevant information, provide a helpful general response
                3. Be concise but informative
                4. Always be polite and professional
                
                Document Context:
                %s
                
                Please answer the user's question based on this context and your general knowledge.
                """.formatted(documentContext);
    }

    private String callOpenAiChatCompletion(List<ChatMessage> messages) {
        try {
            if (openAiApiKey == null || openAiApiKey.equals("your-api-key-here")) {
                log.warn("OpenAI API key not configured, returning mock response");
                return "I'm sorry, but I'm not properly configured to process your request. Please check the OpenAI API key configuration.";
            }

            OpenAiService openAiService = new OpenAiService(openAiApiKey);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(chatModel)
                    .messages(messages)
                    .maxTokens(1000)
                    .temperature(0.7)
                    .build();

            var result = openAiService.createChatCompletion(request);

            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                return result.getChoices().get(0).getMessage().getContent();
            }

            return "I'm sorry, I couldn't generate a response. Please try again.";

        } catch (Exception e) {
            log.error("Error calling OpenAI Chat Completion", e);
            return "I'm sorry, I encountered an error while processing your request. Please try again.";
        }
    }

    private void saveConversationHistory(String userId, String userMessage, String aiResponse) {
        try {
            // Save to database
            ChatHistory chatHistory = ChatHistory.builder()
                    .userId(userId)
                    .message(userMessage)
                    .response(aiResponse)
                    .build();

            chatHistoryRepository.save(chatHistory);

            // Save to Redis for quick access
            String key = REDIS_HISTORY_KEY + userId;

            // Add user message
            redisTemplate.opsForList().rightPush(key, new ChatMessage(ChatMessageRole.USER.value(), userMessage));

            // Add AI response
            redisTemplate.opsForList().rightPush(key, new ChatMessage(ChatMessageRole.ASSISTANT.value(), aiResponse));

            // Trim history to max size
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_HISTORY_SIZE * 2) { // *2 because we store both user and assistant messages
                redisTemplate.opsForList().trim(key, size - MAX_HISTORY_SIZE * 2, -1);
            }

            // Set expiration (24 hours)
            redisTemplate.expire(key, Duration.ofHours(24));

        } catch (Exception e) {
            log.error("Error saving conversation history for user: {}", userId, e);
        }
    }
}
