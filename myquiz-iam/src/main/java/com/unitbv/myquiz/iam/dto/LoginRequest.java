package com.unitbv.myquiz.iam.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class LoginRequest {
    @NotBlank(message = "Identifier is required")
    @Size(max = 255, message = "Identifier cannot exceed 255 characters")
    private String identifier; // can be username or email
    @NotBlank(message = "Password is required")
    private String password;
}
