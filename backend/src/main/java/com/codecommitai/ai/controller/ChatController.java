
package com.codecommitai.ai.controller;

import com.codecommitai.ai.dto.ChatRequest;
import com.codecommitai.ai.dto.ChatResponse;
import com.codecommitai.ai.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(
            @PathVariable UUID repositoryId,
            @Valid @RequestBody ChatRequest request
    ) {

        return chatService.chat(
                repositoryId,
                request.question()
        );
    }
}
