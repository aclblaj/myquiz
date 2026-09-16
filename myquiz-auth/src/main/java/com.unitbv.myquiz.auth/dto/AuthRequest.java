package com.unitbv.myquiz.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {
    @NotBlank(message = "Identifier is required")
    @Size(max = 255, message = "Identifier cannot exceed 255 characters")
    private String identifier;
    @NotBlank(message = "Password is required")
    private String password;
}
