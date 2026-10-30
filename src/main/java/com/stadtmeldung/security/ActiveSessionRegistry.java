package com.stadtmeldung.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveSessionRegistry {
    private final ConcurrentHashMap<Long, Set<HttpSession>> sessions =
            new ConcurrentHashMap<>();

    public void register(long userId, HttpSession session) {
        sessions.computeIfAbsent(userId, ignored ->
                ConcurrentHashMap.newKeySet()).add(session);
    }

    public void invalidate(long userId) {
        Set<HttpSession> userSessions = sessions.remove(userId);
        if (userSessions == null) {
            return;
        }
        for (HttpSession session : userSessions) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // An already expired session needs no further invalidation.
            }
        }
    }
}
