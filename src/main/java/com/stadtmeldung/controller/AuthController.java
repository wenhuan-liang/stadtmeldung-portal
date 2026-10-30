package com.stadtmeldung.controller;

import com.stadtmeldung.security.ActiveSessionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import repository.interfaces.UserRepository;
import service.interfaces.UserService;

import java.util.Locale;
import java.util.Map;

@RestController
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final ActiveSessionRegistry activeSessionRegistry;
    private final ApiResponseMapper responseMapper;

    public AuthController(UserService userService,
                          UserRepository userRepository,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          ActiveSessionRegistry activeSessionRegistry,
                          ApiResponseMapper responseMapper) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.activeSessionRegistry = activeSessionRegistry;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(),
                csrfToken.getToken());
    }

    @PostMapping("/api/login")
    public Map<String, Object> login(
            @RequestBody AccountRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || isBlank(request.username()) ||
                isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte geben Sie Benutzername und Passwort ein.");
        }
        String username = request.username().trim()
                .toLowerCase(Locale.ROOT);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            username, request.password()));
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Benutzername oder Passwort ungültig.");
        }
        User user = userRepository.findByUsername(
                authentication.getName().toLowerCase(Locale.ROOT));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Benutzername oder Passwort ungültig.");
        }
        saveAuthentication(authentication, user, httpRequest, httpResponse);
        return Map.of("user", responseMapper.user(user));
    }

    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody AccountRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || isBlank(request.username()) ||
                isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte geben Sie Benutzername und Passwort ein.");
        }
        User user = userService.registerCitizen(
                request.username(), request.password());
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        user.getUsername(), request.password()));
        saveAuthentication(authentication, user, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("user", responseMapper.user(user)));
    }

    @GetMapping("/api/session")
    public Map<String, Object> session(Authentication authentication) {
        User user = authenticatedUser(authentication);
        return Map.of("user", responseMapper.user(user));
    }

    private void saveAuthentication(Authentication authentication, User user,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        activeSessionRegistry.register(user.getId(), request.getSession(false));
    }

    public User authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Bitte melden Sie sich an.");
        }
        User user = userRepository.findByUsername(
                authentication.getName().toLowerCase(Locale.ROOT));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Benutzerkonto nicht gefunden.");
        }
        return user;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AccountRequest(String username, String password) {}

    public record CsrfResponse(String headerName, String token) {}
}
