package com.stadtmeldung.controller;

import model.User;
import model.enums.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import repository.interfaces.UserRepository;

@Component
public class AuthenticatedUserResolver {
    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName());
    }

    public User requireCitizen(Authentication authentication) {
        return requireRole(authentication, Role.CITIZEN,
                "Diese Aktion ist Bürgern vorbehalten.");
    }

    public User requireStaff(Authentication authentication) {
        User user = currentUser(authentication);
        if (user == null || (user.getRole() != Role.EMPLOYEE &&
                user.getRole() != Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Diese Aktion ist Mitarbeitenden vorbehalten.");
        }
        return user;
    }

    public User requireAdministrator(Authentication authentication) {
        return requireRole(authentication, Role.ADMIN,
                "Diese Aktion ist Administratoren vorbehalten.");
    }

    private User requireRole(Authentication authentication, Role role,
                             String message) {
        User user = currentUser(authentication);
        if (user == null || user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
        return user;
    }
}
