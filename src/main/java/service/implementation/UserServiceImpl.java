package service.implementation;

import model.User;
import model.enums.Role;

import repository.interfaces.UserRepository;

import service.interfaces.PasswordHasher;
import service.interfaces.UserService;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordHasher passwordHasher) {

        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(String name, String email, String password) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Name must not be empty");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email must not be empty");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "Invalid email address");
        }

        if (password == null ||
                password.length() < 12 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Password must be at least 12 characters " +
                    "and at most 72 UTF-8 bytes");
        }

        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException(
                    "Email is already registered");
        }

        String passwordHash = passwordHasher.hash(password);

        User user = new User(
                name.trim(),
                normalizedEmail,
                passwordHash,
                Role.CITIZEN
        );

        userRepository.save(user);

        return user;
    }

    @Override
    public User login(String email, String password) {

        if (email == null || password == null) {
            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail);

        if (user == null ||
                !passwordHasher.verify(
                        password,
                        user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        return user;
    }

    @Override
    public User findById(long id) {
        return userRepository.findById(id);
    }
}
