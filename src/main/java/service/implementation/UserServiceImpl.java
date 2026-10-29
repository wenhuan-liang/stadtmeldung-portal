package service.implementation;

import model.User;
import model.enums.Role;

import repository.interfaces.UserRepository;

import service.interfaces.PasswordHasher;
import service.interfaces.UserService;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
        user.setUsername(normalizedEmail);

        userRepository.save(user);

        return user;
    }

    @Override
    public User registerCitizen(String username, String password) {
        return registerWithRole(username, password, Role.CITIZEN);
    }

    @Override
    public User provisionAdmin(String username, String password) {
        return registerWithRole(username, password, Role.ADMIN);
    }

    @Override
    public User createEmployee(long administratorId, String username,
                               String password) {
        if (administratorId <= 0) {
            throw new IllegalArgumentException(
                    "Administrator ID must be greater than zero");
        }
        User administrator = userRepository.findById(administratorId);
        if (administrator == null || administrator.getRole() != Role.ADMIN) {
            throw new SecurityException(
                    "Only administrators can create employee accounts");
        }
        return registerWithRole(username, password, Role.EMPLOYEE);
    }

    @Override
    public List<User> findEmployees() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.EMPLOYEE)
                .toList();
    }

    @Override
    public void deleteEmployee(long administratorId, long employeeId) {
        User administrator = userRepository.findById(administratorId);
        if (administrator == null || administrator.getRole() != Role.ADMIN) {
            throw new SecurityException(
                    "Only administrators can delete employee accounts");
        }
        if (employeeId <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be greater than zero");
        }
        User employee = userRepository.findById(employeeId);
        if (employee == null || employee.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException(
                    "Employee account not found");
        }
        userRepository.deleteById(employeeId);
    }

    private User registerWithRole(String username, String password,
                                  Role role) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userRepository.findByUsername(normalizedUsername) != null) {
            throw new IllegalArgumentException("Username is already registered");
        }

        User user = new User(
                normalizedUsername,
                normalizedUsername,
                passwordHasher.hash(password),
                role
        );
        user.setUsername(normalizedUsername);
        userRepository.save(user);
        return user;
    }

    @Override
    public User login(String username, String password) {

        if (username == null || password == null) {
            throw new IllegalArgumentException(
                    "Invalid username or password");
        }

        String normalizedUsername =
                username.trim().toLowerCase(Locale.ROOT);

        User user = normalizedUsername.contains("@")
                ? userRepository.findByEmail(normalizedUsername)
                : userRepository.findByUsername(normalizedUsername);

        if (user == null ||
                !passwordHasher.verify(
                        password,
                        user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Invalid username or password");
        }

        return user;
    }

    @Override
    public User findById(long id) {
        return userRepository.findById(id);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[a-z0-9._-]{3,32}$")) {
            throw new IllegalArgumentException(
                    "Username must contain 3 to 32 letters, numbers, dots, underscores or hyphens");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
    }
}
