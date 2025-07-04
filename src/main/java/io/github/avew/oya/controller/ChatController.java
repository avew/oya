package io.github.avew.oya.controller;

import io.github.avew.oya.constants.ResponseCodes;
import io.github.avew.oya.dto.ApiResponse;
import io.github.avew.oya.dto.ChatRequest;
import io.github.avew.oya.dto.ChatResponse;
import io.github.avew.oya.exception.ChatProcessingException;
import io.github.avew.oya.service.ChatService;
import io.github.avew.oya.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request from user: {}", request.getUserId());
        ChatResponse chatResponse = chatService.chat(request);

        ApiResponse<ChatResponse> response = ApiResponse.success(
            ResponseCodes.ChatSuccess.RESPONSE_GENERATED,
            messageService.getMessage(ResponseCodes.ChatSuccess.RESPONSE_GENERATED),
            chatResponse
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        ApiResponse<String> response = ApiResponse.success(
            ResponseCodes.ChatSuccess.SERVICE_HEALTHY,
            messageService.getMessage(ResponseCodes.ChatSuccess.SERVICE_HEALTHY),
            "Service is healthy"
        );
        return ResponseEntity.ok(response);
    }
}
