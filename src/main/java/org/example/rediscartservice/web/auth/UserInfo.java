package org.example.rediscartservice.web.auth;

import java.util.List;

/** Minimal user info returned by /api/auth/login and /api/auth/me */
public record UserInfo(
        String username,
        List<String> roles
) {}
