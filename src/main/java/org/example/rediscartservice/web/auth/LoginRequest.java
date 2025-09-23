package org.example.rediscartservice.web.auth;


import jakarta.validation.constraints.NotBlank;

/** Request body for /api/auth/login */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
