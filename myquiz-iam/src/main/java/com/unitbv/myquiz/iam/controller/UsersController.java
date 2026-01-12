package com.unitbv.myquiz.iam.controller;


import com.unitbv.myquiz.iam.dto.RegisterRequest;
import com.unitbv.myquiz.iam.dto.RegisterResponse;
import com.unitbv.myquiz.iam.dto.UserDTO;
import com.unitbv.myquiz.iam.dto.UserDetailsDTO;
import com.unitbv.myquiz.iam.entity.User;
import com.unitbv.myquiz.iam.exceptions.UserAlreadyExistsException;
import com.unitbv.myquiz.iam.service.UsersService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("api/users")
public class UsersController {
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/test")
    public RegisterResponse test() {
        return new
                RegisterResponse(true, "yay", null);
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = usersService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("find/{identifier}")
    public ResponseEntity<UserDetailsDTO> findByIdentifier(@PathVariable String identifier) {
        try {
            Optional<User> user = usersService.getByUsernameOrEmail(identifier);

            if (!user.isEmpty()) {
                UserDetailsDTO userDetailsDTO = new UserDetailsDTO(
                        user.get().getId(),
                        user.get().getUsername(),
                        user.get().getEmail(),
                        user.get().getHashedPassword()
                );
                return ResponseEntity.ok(userDetailsDTO);
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new UserDetailsDTO(null, null, null, null)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserDetailsDTO(null, null, null, null));
        }
    }

    @GetMapping("is-available/{username}")
    public ResponseEntity<Boolean> getUserByUsername(@PathVariable String username) {
        Optional<User> user = usersService.getUserByUsername(username);
        return ResponseEntity.ok(user.isEmpty());
    }

    @PostMapping("create")
    public ResponseEntity<RegisterResponse> createUser(@RequestBody RegisterRequest request) {
        log.info("Received registration: username={}, email={}", request.getUsername(), request.getEmail());
        try {
            User user = usersService.createUser(request.getUsername(), request.getEmail(), request.getPassword());

            UserDTO userDTO = new UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail()
            );

            RegisterResponse response = new RegisterResponse(true, "User created successfully", userDTO);
            log.info("User created: {}", userDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserAlreadyExistsException e) {
            log.warn("User already exists: {}", request.getUsername());
            RegisterResponse response = new RegisterResponse(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            String msg = "Duplicate username or email.";
            RegisterResponse response = new RegisterResponse(false, msg, null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("Registration error: ", e);
            RegisterResponse response = new RegisterResponse(false, "Internal server error", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}