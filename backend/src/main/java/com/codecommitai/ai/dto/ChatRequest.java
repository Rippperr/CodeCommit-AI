
package com.codecommitai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @NotBlank(message = "Question is required")
        @Size(max = 4000, message = "Question must not exceed 4000 characters")
        String question

) {
}
