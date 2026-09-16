package com.unitbv.myquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Ollama AI requests.
 */
@Schema(description = "Ollama request DTO for AI operations")
public class OllamaRequestDto {

    @Schema(description = "The prompt to send to Ollama")
    @NotBlank(message = "Prompt cannot be blank")
    @Size(max = 5000, message = "Prompt cannot exceed 5000 characters")
    private String prompt;

    @Schema(description = "The model to use")
    @Size(max = 100, message = "Model name cannot exceed 100 characters")
    private String model;

    @Schema(description = "Temperature for response generation")
    @DecimalMin(value = "0.0", message = "Temperature cannot be negative")
    @DecimalMax(value = "2.0", message = "Temperature cannot exceed 2")
    private Double temperature;

    @Schema(description = "Maximum tokens in response")
    @Positive(message = "Maximum tokens must be positive")
    private Integer maxTokens;

    @Schema(description = "If true, enables real-time streaming of the model’s output")
    private boolean stream = false;

    public OllamaRequestDto() {
    }

    public OllamaRequestDto(String prompt, String model, Double temperature, Integer maxTokens, boolean stream) {
        this.prompt = prompt;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public static class Builder {
        private String prompt;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private boolean stream;

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public OllamaRequestDto build() {
            return new OllamaRequestDto(prompt, model, temperature, maxTokens, stream);
        }
    }
}
