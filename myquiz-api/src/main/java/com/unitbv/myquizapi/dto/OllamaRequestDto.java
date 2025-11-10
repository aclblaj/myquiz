package com.unitbv.myquizapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for Ollama AI requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ollama request DTO for AI operations")
public class OllamaRequestDto {

    @Schema(description = "The prompt to send to Ollama")
    @NotBlank(message = "Prompt cannot be blank")
    @Size(max = 5000, message = "Prompt cannot exceed 5000 characters")
    @JsonProperty("prompt")
    private String prompt;

    @Schema(description = "The model to use")
    @JsonProperty("model")
    private String model;

    @Schema(description = "Temperature for response generation")
    @JsonProperty("temperature")
    private Double temperature;

    @Schema(description = "Maximum tokens in response")
    @JsonProperty("maxTokens")
    private Integer maxTokens;

    @Schema(description = "If true, enables real-time streaming of the model’s output")
    @JsonProperty("stream")
    @Builder.Default
    private boolean stream = false;
}