package org.example.rediscartservice.infrastructure.session;

import java.util.Optional;

public interface SessionOwnerRegistry {
    void register(String sessionId, String username);
    Optional<String> findUsernameBySessionId(String sessionId);
    void evict(String sessionId);
}