package org.example.rediscartservice.web.security.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.rediscartservice.config.CartIdleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class SessionTouchAspect {
    private static final Logger log = LoggerFactory.getLogger(SessionTouchAspect.class);

    private final JedisPooled jedis;
    private final CartIdleProperties props;
    private final HttpServletRequest request;

    @Around("@within(org.example.rediscartservice.web.security.annotations.SessionTouch) || " +
            "@annotation(org.example.rediscartservice.web.security.annotations.SessionTouch)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        try { touch(); } catch (Exception e) { log.debug("Session touch skipped: {}", e.toString()); }
        return pjp.proceed();
    }

    private void touch() {
        var session = request.getSession(false);
        if (session == null) return;

        String currentSessionId = session.getId();
        String metaKey = "sess:" + currentSessionId + ":meta";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                ? auth.getName() : "anonymous";

        long now = System.currentTimeMillis();
        long ttlMs = props.getIdleTtl().toMillis();

        // Set once
        jedis.hsetnx(metaKey, "user", user);
        jedis.hsetnx(metaKey, "created_at", String.valueOf(now));
        jedis.hset(metaKey, Map.of("last_active", String.valueOf(now), "active", "1"));

        // Ensure the session has a cart pointer (inherit from latest alive session if possible)
        String currentCartId = jedis.hget(metaKey, "cart_id");
        if (currentCartId == null) {
            String cartIdToUse = null;

            if (!"anonymous".equals(user)) {
                // Fetch the most recent sessionId for this user
                String latestSessionId = jedis.zrevrange("sess:user:" + user, 0, 0).stream().findFirst().orElse(null);

                // If the latest is different from current and still alive, inherit its cart_id
                if (!currentSessionId.equals(latestSessionId)) {
                    String prevMetaKey = "sess:" + latestSessionId + ":meta";
                    if (jedis.exists(prevMetaKey)) { // alive if meta still exists
                        String prevCartId = jedis.hget(prevMetaKey, "cart_id");
                        if (prevCartId != null && !prevCartId.isBlank()) {
                            cartIdToUse = prevCartId;
                        }
                    }
                }
            }

            if (cartIdToUse == null) {
                cartIdToUse = UUID.randomUUID().toString();
            }
            jedis.hset(metaKey, Map.of("cart_id", cartIdToUse));
        }

        // Sliding idle window (keep 'active' field hot)
        String script = "return redis.call('HPEXPIRE', KEYS[1], ARGV[1], 'FIELDS', 1, 'active')";
        jedis.eval(script, 1, metaKey, String.valueOf(ttlMs));

        // Track user recency
        if (!"anonymous".equals(user)) {
            jedis.zadd("sess:user:" + user, now, currentSessionId);
        }
    }
}