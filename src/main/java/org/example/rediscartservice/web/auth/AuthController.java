package org.example.rediscartservice.web.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.web.security.annotations.Authenticated;
import org.example.rediscartservice.web.security.annotations.RotateSession;
import org.example.rediscartservice.web.security.annotations.SessionTouch;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Auth", description = "Session-based authentication endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Operation(summary = "Login with username/password (creates session)")
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @RotateSession
    @SessionTouch
    public UserInfo login(@RequestBody LoginRequest req,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        // build and set context
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // IMPORTANT: persist to session so subsequent requests are authenticated
        securityContextRepository.saveContext(context, request, response);

        // Optionally rotate the session id
        request.changeSessionId();

        return toUserInfo(auth);
    }

    @Operation(summary = "Get the current authenticated user (if any)")
    @GetMapping("/me")
    @Authenticated
    @SessionTouch
    public UserInfo me(Authentication auth) {
        return toUserInfo(auth);
    }

    @Operation(summary = "Logout and invalidate session")
    @PostMapping("/logout")
    @Authenticated
    public void logout(HttpSession session) {
        if (session != null) session.invalidate();
    }

    private UserInfo toUserInfo(Authentication auth) {
        String username = auth.getName();
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return new UserInfo(username, roles);
    }
}