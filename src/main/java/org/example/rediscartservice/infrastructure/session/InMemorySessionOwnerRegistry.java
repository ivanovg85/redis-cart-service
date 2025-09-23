package org.example.rediscartservice.infrastructure.session;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemorySessionOwnerRegistry implements SessionOwnerRegistry {
    private final ConcurrentMap<String, String> map = new ConcurrentHashMap<>();

    @Override
    public void register(String sessionId, String username) {
        if (sessionId != null && username != null && !username.isBlank()) {
            map.put(sessionId, username);
        }
    }

    @Override
    public Optional<String> findUsernameBySessionId(String sessionId) {
        return Optional.ofNullable(map.get(sessionId));
    }

    @Override
    public void evict(String sessionId) {
        if (sessionId != null) map.remove(sessionId);
    }
}