package com.unitbv.myquizapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for Ollama AI responses.
 */
@Schema(description = "Ollama response DTO for AI operations")
public class OllamaResponseDto {

    @Schema(description = "The generated response from Ollama")
    @JsonProperty("response")
    private String response;

    @Schema(description = "The model used for generation")
    @JsonProperty("model")
    private String model;

    @Schema(description = "Whether the response is complete")
    @JsonProperty("done")
    private Boolean done;

    @Schema(description = "Total duration of the request")
    @JsonProperty("totalDuration")
    private Long totalDuration;

    @Schema(description = "Time taken to load the model")
    @JsonProperty("loadDuration")
    private Long loadDuration;

    @Schema(description = "Number of tokens in the prompt")
    @JsonProperty("promptEvalCount")
    private Integer promptEvalCount;

    @Schema(description = "Time taken to evaluate the prompt")
    @JsonProperty("promptEvalDuration")
    private Long promptEvalDuration;

    @Schema(description = "Number of tokens in the response")
    @JsonProperty("evalCount")
    private Integer evalCount;

    @Schema(description = "Time taken to generate the response")
    @JsonProperty("evalDuration")
    private Long evalDuration;

    // Default constructor
    public OllamaResponseDto() {}

    // Getters and setters
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Long getLoadDuration() {
        return loadDuration;
    }

    public void setLoadDuration(Long loadDuration) {
        this.loadDuration = loadDuration;
    }

    public Integer getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(Integer promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }

    public Long getPromptEvalDuration() {
        return promptEvalDuration;
    }

    public void setPromptEvalDuration(Long promptEvalDuration) {
        this.promptEvalDuration = promptEvalDuration;
    }

    public Integer getEvalCount() {
        return evalCount;
    }

    public void setEvalCount(Integer evalCount) {
        this.evalCount = evalCount;
    }

    public Long getEvalDuration() {
        return evalDuration;
    }

    public void setEvalDuration(Long evalDuration) {
        this.evalDuration = evalDuration;
    }

    @Override
    public String toString() {
        return "OllamaResponseDto{" +
                "response='" + response + '\'' +
                ", model='" + model + '\'' +
                ", done=" + done +
                ", totalDuration=" + totalDuration +
                ", evalCount=" + evalCount +
                '}';
    }
}
