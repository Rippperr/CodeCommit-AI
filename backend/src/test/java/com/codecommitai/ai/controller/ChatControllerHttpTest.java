
package com.codecommitai.ai.controller;

import com.codecommitai.ai.service.ChatService;
import com.codecommitai.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void chat_shouldRejectBlankQuestion() throws Exception {

        String repositoryId =
                "a2d08518-be93-4ba0-8931-76d50eef3fd3";

        mockMvc.perform(
                post(
                        "/api/v1/repositories/{repositoryId}/chat",
                        repositoryId
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "question": "   "
                        }
                        """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
                .value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors.question")
                .value("Question is required"));

        verifyNoInteractions(chatService);
    }
}
