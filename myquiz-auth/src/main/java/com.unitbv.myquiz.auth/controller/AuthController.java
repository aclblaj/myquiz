package com.unitbv.myquiz.auth.controller;

import com.unitbv.myquiz.auth.config.JwtUtil;
import com.unitbv.myquiz.auth.dto.AuthRequest;
import com.unitbv.myquiz.auth.dto.AuthResponse;
import com.unitbv.myquiz.auth.dto.RegisterRequest;
import com.unitbv.myquiz.auth.dto.RegisterResponse;
import com.unitbv.myquiz.auth.dto.UserDTO;
import com.unitbv.myquiz.auth.dto.UserDetailsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("api/auth")
@CrossOrigin(origins = "${FRONTEND_URL}")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final String apiIamUrl;

    public AuthController(RestTemplate restTemplate,
                         JwtUtil jwtUtil,
                         PasswordEncoder passwordEncoder,
                         @Value("${MYQUIZ_IAM_URL}") String apiIamUrl) {
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.apiIamUrl = apiIamUrl;
    }

    @GetMapping("username-available")
    public ResponseEntity<Boolean> checkUsername(@RequestParam("username") String username) {
        logger.info("Checking if username is available: {}", username);
        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    apiIamUrl + "/users/is-available/" + username,
                    Boolean.class
            );
            logger.info("Username availability response for '{}': {}", username, response.getBody());
            return ResponseEntity.status(HttpStatus.OK).body(response.getBody());
        } catch (Exception ex) {
            logger.error("Error checking username availability for '{}': {}", username, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @PostMapping("register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        final String FIND_URL = "/users/find/";
        logger.info("Register attempt: username={}, email={}", request.getUsername(), request.getEmail());
        try {
            String url = apiIamUrl + FIND_URL + request.getUsername();
            logger.info("Checking if username '{}' exists at {}", request.getUsername(), url);
            ResponseEntity<UserDetailsDTO> response = restTemplate.getForEntity(url, UserDetailsDTO.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.warn("Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest().body(new AuthResponse(null));
            }
            url = apiIamUrl + FIND_URL + request.getEmail();
            logger.info("Checking if email '{}' exists at {}", request.getEmail(), url);
            response = restTemplate.getForEntity(url, UserDetailsDTO.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.warn("Email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest().body(new AuthResponse(null));
            }
        } catch (HttpClientErrorException.NotFound e) {
            logger.info("No existing user found, proceeding to register: username={}, email={}", request.getUsername(), request.getEmail());
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setUsername(request.getUsername());
            registerRequest.setEmail(request.getEmail());
            registerRequest.setPassword(passwordEncoder.encode(request.getPassword()));
            logger.info("Sending registration to IAM: username={}, email={}", registerRequest.getUsername(), registerRequest.getEmail());
            ResponseEntity<RegisterResponse> creationResponse = restTemplate.postForEntity(
                    apiIamUrl + "/users/create",
                    registerRequest,
                    RegisterResponse.class
            );
            logger.info("IAM response: status={}, body={}", creationResponse.getStatusCode(), creationResponse.getBody());
            if (creationResponse.getStatusCode() == HttpStatus.CREATED && creationResponse.getBody() != null) {
                UserDTO createdUser = creationResponse.getBody().getUser();
                String token = jwtUtil.generateToken(createdUser.getUsername());
                logger.info("Registration successful for: {}", createdUser.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
            } else {
                logger.error("IAM registration failed: status={}, body={}", creationResponse.getStatusCode(), creationResponse.getBody());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse(null));
            }
        } catch (Exception ex) {
            logger.error("Error during registration for username={}, email={}: {}", request.getUsername(), request.getEmail(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null));
        }
        logger.error("Registration failed for unknown reason: username={}, email={}", request.getUsername(), request.getEmail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthResponse(null));
    }

    @PostMapping("login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        logger.info("Login attempt for identifier: {}", request.getIdentifier());
        try {
            String url = apiIamUrl + "/users/find/" + request.getIdentifier();
            logger.info("Fetching user details for identifier '{}' from {}", request.getIdentifier(), url);
            ResponseEntity<UserDetailsDTO> response = restTemplate.getForEntity(url, UserDetailsDTO.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserDetailsDTO userDetails = response.getBody();
                logger.info("User found: {}", userDetails.getUsername());
                boolean passwordMatch = BCrypt.checkpw(request.getPassword(), userDetails.getHashedPassword());
                logger.info("Password match for '{}': {}", request.getIdentifier(), passwordMatch);
                if (passwordMatch) {
                    // IMPORTANT: generate token with canonical username to align with validator expectations
                    String token = jwtUtil.generateToken(userDetails.getUsername());
                    logger.info("Login successful for: {} (token subject: {})", request.getIdentifier(), userDetails.getUsername());
                    return ResponseEntity.ok(new AuthResponse(token));
                } else {
                    logger.warn("Invalid password for: {}", request.getIdentifier());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null));
                }
            } else {
                logger.warn("User not found: {}", request.getIdentifier());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse(null));
            }
        } catch (Exception e) {
            logger.error("Login error for: {} - {}", request.getIdentifier(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null));
        }
    }
}