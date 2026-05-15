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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @GetMapping("health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "myquiz-auth");
        return ResponseEntity.ok(response);
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
                Long userId = createdUser.getId();

                // Assign GUEST role to new user
                assignGuestRole(userId);

                // Return success without token - user needs to be activated by admin
                logger.info("Registration successful for: {} - awaiting admin activation", createdUser.getUsername());
                AuthResponse response = new AuthResponse(null);
                response.setMessage("Registration successful. Your account is pending activation by an administrator.");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        logger.info("=== LOGIN ATTEMPT START ===");
        logger.info("Login attempt for identifier: {}", request.getIdentifier());
        logger.info("Request received: identifier={}, password length={}",
                    request.getIdentifier(),
                    request.getPassword() != null ? request.getPassword().length() : 0);

        try {
            String url = apiIamUrl + "/users/find/" + request.getIdentifier();
            logger.info("Fetching user details from IAM: {}", url);
            logger.info("IAM URL configured as: {}", apiIamUrl);

            ResponseEntity<UserDetailsDTO> response = restTemplate.getForEntity(url, UserDetailsDTO.class);
            logger.info("IAM response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                UserDetailsDTO userDetails = response.getBody();
                logger.info("IAM response body received: id={}, username={}, email={}, hasPassword={}",
                           userDetails.getId(),
                           userDetails.getUsername(),
                           userDetails.getEmail(),
                           userDetails.getHashedPassword() != null);

                // Validate userDetails has required fields
                if (userDetails.getUsername() == null || userDetails.getHashedPassword() == null) {
                    logger.error("IAM returned incomplete user details for: {}. username={}, hasPassword={}",
                                request.getIdentifier(),
                                userDetails.getUsername(),
                                userDetails.getHashedPassword() != null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null));
                }

                logger.info("User found in IAM: {}", userDetails.getUsername());
                logger.debug("Password hash prefix: {}",
                           userDetails.getHashedPassword().substring(0, Math.min(7, userDetails.getHashedPassword().length())));

                // Check if user is enabled
                if (userDetails.getEnabled() != null && !userDetails.getEnabled()) {
                    logger.warn("Login attempt for inactive user: {}", request.getIdentifier());
                    logger.warn("=== LOGIN ATTEMPT FAILED: USER NOT ACTIVATED ===");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse(null));
                }

                // Verify password using passwordEncoder
                logger.debug("Validating password for user: {}", request.getIdentifier());
                boolean passwordMatch = passwordEncoder.matches(request.getPassword(), userDetails.getHashedPassword());
                logger.info("Password validation result for '{}': {}", request.getIdentifier(), passwordMatch);

                if (passwordMatch) {
                    // Fetch user roles and permissions from IAM
                    Long userId = userDetails.getId();
                    logger.info("Fetching roles and permissions for user ID: {}", userId);

                    Set<String> roles = fetchUserRoles(userId);
                    Set<String> permissions = fetchUserPermissions(userId);

                    logger.info("User {} authenticated successfully with {} roles and {} permissions",
                                userDetails.getUsername(), roles.size(), permissions.size());
                    logger.info("Roles: {}", roles);

                    // Generate token with username, roles, and permissions
                    logger.info("Generating JWT token");
                    String token = jwtUtil.generateToken(userDetails.getUsername(), roles, permissions);
                    logger.info("JWT token generated for user: {}, token length: {}",
                               userDetails.getUsername(),
                               token != null ? token.length() : 0);

                    logger.info("=== LOGIN ATTEMPT SUCCESS ===");
                    return ResponseEntity.ok(new AuthResponse(token));
                } else {
                    logger.warn("Authentication failed for '{}': invalid password", request.getIdentifier());
                    logger.warn("=== LOGIN ATTEMPT FAILED: INVALID PASSWORD ===");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null));
                }
            } else {
                logger.warn("User not found in IAM: {}. Status: {}, hasBody: {}",
                           request.getIdentifier(),
                           response.getStatusCode(),
                           response.getBody() != null);
                logger.warn("=== LOGIN ATTEMPT FAILED: USER NOT FOUND ===");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse(null));
            }
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("User not found in IAM (404): {}", request.getIdentifier());
            logger.warn("=== LOGIN ATTEMPT FAILED: NOT FOUND EXCEPTION ===");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse(null));
        } catch (Exception e) {
            logger.error("Login error for '{}': {} - {}", request.getIdentifier(), e.getClass().getSimpleName(), e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("=== LOGIN ATTEMPT FAILED: EXCEPTION ===");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null));
        }
    }

    /**
     * Check if a user has a specific permission
     * @param username The username to check
     * @param permission The permission name to check
     * @return true if user has the permission, false otherwise
     */
    @GetMapping("check-permission")
    public ResponseEntity<Boolean> checkPermission(
            @RequestParam("username") String username,
            @RequestParam("permission") String permission) {
        logger.info("Checking permission '{}' for user '{}'", permission, username);
        try {
            // Get user details to fetch user ID
            String userUrl = apiIamUrl + "/users/find/" + username;
            ResponseEntity<UserDetailsDTO> userResponse = restTemplate.getForEntity(userUrl, UserDetailsDTO.class);

            if (userResponse.getStatusCode() != HttpStatus.OK || userResponse.getBody() == null) {
                logger.warn("User not found: {}", username);
                return ResponseEntity.ok(false);
            }

            Long userId = userResponse.getBody().getId();

            // Fetch user permissions
            Set<String> permissions = fetchUserPermissions(userId);

            boolean hasPermission = permissions.contains(permission);
            logger.info("User '{}' has permission '{}': {}", username, permission, hasPermission);

            return ResponseEntity.ok(hasPermission);
        } catch (Exception e) {
            logger.error("Error checking permission '{}' for user '{}': {}", permission, username, e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Get IAM statistics (users, roles, permissions counts) from myquiz-iam
     * @return Map containing entity counts
     */
    @GetMapping("iam-statistics")
    public ResponseEntity<Map<String, Long>> getIamStatistics() {
        logger.info("Fetching IAM statistics");
        Map<String, Long> stats = new HashMap<>();

        try {
            // Get users count
            String usersUrl = apiIamUrl + "/users";
            ResponseEntity<Object[]> usersResponse = restTemplate.getForEntity(usersUrl, Object[].class);
            if (usersResponse.getStatusCode() == HttpStatus.OK && usersResponse.getBody() != null) {
                stats.put("users", (long) usersResponse.getBody().length);
                logger.debug("Users count: {}", usersResponse.getBody().length);
            }

            // Get roles count
            String rolesUrl = apiIamUrl + "/roles";
            ResponseEntity<Object[]> rolesResponse = restTemplate.getForEntity(rolesUrl, Object[].class);
            if (rolesResponse.getStatusCode() == HttpStatus.OK && rolesResponse.getBody() != null) {
                stats.put("roles", (long) rolesResponse.getBody().length);
                logger.debug("Roles count: {}", rolesResponse.getBody().length);
            }

            // Get permissions count
            String permissionsUrl = apiIamUrl + "/permissions";
            ResponseEntity<Object[]> permissionsResponse = restTemplate.getForEntity(permissionsUrl, Object[].class);
            if (permissionsResponse.getStatusCode() == HttpStatus.OK && permissionsResponse.getBody() != null) {
                stats.put("permissions", (long) permissionsResponse.getBody().length);
                logger.debug("Permissions count: {}", permissionsResponse.getBody().length);
            }

            logger.info("IAM statistics retrieved successfully: {} entities", stats.size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching IAM statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stats);
        }
    }

    /**
     * Assign GUEST role to new user
     */
    private void assignGuestRole(Long userId) {
        try {
            // First, get GUEST role ID
            String rolesUrl = apiIamUrl + "/roles";
            ResponseEntity<Object[]> rolesResponse = restTemplate.getForEntity(rolesUrl, Object[].class);
            if (rolesResponse.getStatusCode() == HttpStatus.OK && rolesResponse.getBody() != null) {
                for (Object roleObj : rolesResponse.getBody()) {
                    if (roleObj instanceof java.util.LinkedHashMap) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> roleMap = (java.util.Map<String, Object>) roleObj;
                        if ("GUEST".equals(roleMap.get("name"))) {
                            Object roleIdObj = roleMap.get("id");
                            Long guestRoleId = null;
                            if (roleIdObj instanceof Integer) {
                                guestRoleId = ((Integer) roleIdObj).longValue();
                            } else if (roleIdObj instanceof Long) {
                                guestRoleId = (Long) roleIdObj;
                            }

                            if (guestRoleId != null) {
                                // Assign GUEST role to user
                                String assignUrl = apiIamUrl + "/users/" + userId + "/roles/" + guestRoleId;
                                restTemplate.postForEntity(assignUrl, null, Object.class);
                                logger.info("Assigned GUEST role to user {}", userId);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to assign GUEST role to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Fetch user roles from IAM service
     */
    private Set<String> fetchUserRoles(Long userId) {
        try {
            String url = apiIamUrl + "/users/" + userId + "/roles";
            ResponseEntity<Object[]> response = restTemplate.getForEntity(url, Object[].class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Set<String> roleNames = new HashSet<>();
                for (Object roleObj : response.getBody()) {
                    if (roleObj instanceof java.util.LinkedHashMap) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> roleMap = (java.util.Map<String, Object>) roleObj;
                        if (roleMap.get("name") != null) {
                            roleNames.add(roleMap.get("name").toString());
                        }
                    }
                }
                return roleNames;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch roles for user {}: {}", userId, e.getMessage());
        }
        return new HashSet<>();
    }

    /**
     * Fetch user permissions from IAM service (merged from all roles)
     */
    private Set<String> fetchUserPermissions(Long userId) {
        try {
            String url = apiIamUrl + "/users/" + userId + "/permissions";
            ResponseEntity<Object[]> response = restTemplate.getForEntity(url, Object[].class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Set<String> permissionNames = new HashSet<>();
                for (Object permObj : response.getBody()) {
                    if (permObj instanceof java.util.LinkedHashMap) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> permMap = (java.util.Map<String, Object>) permObj;
                        if (permMap.get("name") != null) {
                            permissionNames.add(permMap.get("name").toString());
                        }
                    }
                }
                return permissionNames;
            }
        } catch (Exception e) {
            logger.error("Failed to fetch permissions for user {}: {}", userId, e.getMessage());
        }
        return new HashSet<>();
    }
}

