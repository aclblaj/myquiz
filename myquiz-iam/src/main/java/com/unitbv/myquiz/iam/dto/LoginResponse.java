package com.unitbv.myquiz.iam.dto;

import com.unitbv.myquiz.iam.entity.User;
import lombok.Data;

import java.util.Optional;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private User user;
    private String token;

    public LoginResponse(boolean success, String message, Optional<User> user, String token) {
        this.success = success;
        this.message = message;
        user.ifPresent(value -> this.user = value);
        this.token = token;
    }
}
