package com.unitbv.myquiz.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input DTO for creating or updating an Author.
 * <p>
 * Carries only the fields a caller may supply through the API: {@code name} and {@code initials}.
 * Statistics and context fields (question counts, course, questionBankName, etc.) are
 * computed server-side and returned in the richer {@link AuthorDto} response.
 * <p>
 * By accepting this type on write endpoints instead of the full {@link AuthorDto} the API
 * surface is simpler and clients cannot accidentally send read-only statistics in their request body.
 */
@Schema(description = "Input DTO for creating or updating an author")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorUpsertDto {

    @Schema(description = "Author's full name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Author's initials")
    @JsonProperty("initials")
    private String initials;

    /**
     * Converts this upsert input into a full {@link AuthorDto} so it can be passed
     * to service methods that still work with the legacy type.
     *
     * @param id optional author ID to embed (use {@code null} for create operations)
     */
    public AuthorDto toAuthorDto(Long id) {
        AuthorDto dto = new AuthorDto();
        dto.setId(id);
        dto.setName(this.name);
        dto.setInitials(this.initials);
        return dto;
    }
}

