
package com.codecommitai.ai.controller;

import com.codecommitai.ai.dto.ChatRequest;
import com.codecommitai.ai.dto.ChatResponse;
import com.codecommitai.ai.dto.SourceReference;
import com.codecommitai.ai.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController(chatService);
    }

    @Test
    void chat_shouldReturnAnswerAndSources() {

        UUID repositoryId = UUID.randomUUID();

        ChatRequest request =
                new ChatRequest("Explain repository indexing");

        ChatResponse expectedResponse =
                new ChatResponse(
                        "Repository indexing processes files into searchable chunks.",
                        List.of(
                                new SourceReference(
                                        "src/main/java/IndexingService.java",
                                        10,
                                        25
                                )
                        )
                );

        when(chatService.chat(
                repositoryId,
                request.question()
        )).thenReturn(expectedResponse);

        ChatResponse response =
                chatController.chat(
                        repositoryId,
                        request
                );

        assertEquals(expectedResponse, response);

        verify(chatService).chat(
                repositoryId,
                "Explain repository indexing"
        );
    }

    @Test
    void chat_shouldReturnResponseWithEmptySources() {

        UUID repositoryId = UUID.randomUUID();

        ChatRequest request =
                new ChatRequest("Explain an unknown feature");

        ChatResponse expectedResponse =
                new ChatResponse(
                        "I could not find relevant code.",
                        List.of()
                );

        when(chatService.chat(
                repositoryId,
                request.question()
        )).thenReturn(expectedResponse);

        ChatResponse response =
                chatController.chat(
                        repositoryId,
                        request
                );

        assertEquals(
                "I could not find relevant code.",
                response.answer()
        );

        assertTrue(response.sources().isEmpty());
    }
}
