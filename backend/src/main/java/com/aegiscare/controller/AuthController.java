package com.aegiscare.controller;

import com.aegiscare.security.CurrentUserService;
import com.aegiscare.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUser;

    public AuthController(AuthService authService, CurrentUserService currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password
    ) { }

    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody LoginRequest request) {
        try {
            return authService.login(request.email(), request.password());
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
        }
    }

    @GetMapping("/me")
    public AuthService.UserView me() {
        return authService.currentUser(currentUser.userId());
    }
}
