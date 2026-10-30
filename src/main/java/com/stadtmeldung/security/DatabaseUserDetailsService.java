package com.stadtmeldung.security;

import model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import repository.interfaces.UserRepository;

import java.util.Locale;

public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByUsername(normalized);
        if (user == null) {
            user = userRepository.findByEmail(normalized);
        }
        if (user == null) {
            throw new UsernameNotFoundException("Invalid username or password");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
