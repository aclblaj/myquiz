package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Ollama AI requests.
 */
@Schema(description = "Ollama request DTO for AI operations")
public class OllamaRequestDto {

    @Schema(description = "The prompt to send to Ollama", required = true)
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

    @Schema(description = "Whether to stream the response (default: false)")
    @JsonProperty("stream")
    private Boolean stream = false;

    // Default constructor
    public OllamaRequestDto() {}

    public OllamaRequestDto(String prompt) {
        this.prompt = prompt;
    }

    // Constructor with model and prompt
    public OllamaRequestDto(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }

    // Getters and setters
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    @Override
    public String toString() {
        return "OllamaRequestDto{" +
                "prompt='" + prompt + '\'' +
                ", model='" + model + '\'' +
                ", temperature=" + temperature +
                ", maxTokens=" + maxTokens +
                ", stream=" + stream +
                '}';
    }
}
