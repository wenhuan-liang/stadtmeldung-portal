package com.stadtmeldung.controller;

import java.util.List;
import java.util.Map;

import com.stadtmeldung.security.ActiveSessionRegistry;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import service.interfaces.UserService;

@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {
    private final UserService userService;
    private final ActiveSessionRegistry activeSessionRegistry;
    private final AuthenticatedUserResolver userResolver;

    public AdminEmployeeController(UserService userService,
                                   ActiveSessionRegistry activeSessionRegistry,
                                   AuthenticatedUserResolver userResolver) {
        this.userService = userService;
        this.activeSessionRegistry = activeSessionRegistry;
        this.userResolver = userResolver;
    }

    @GetMapping
    public List<Map<String, Object>> employees() {
        return userService.findEmployees().stream()
                .map(AdminEmployeeController::employeeResponse)
                .toList();
    }

    @PostMapping
    public Map<String, Object> create(
            @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bitte geben Sie Benutzername und Passwort ein.");
        }
        User administrator =
                userResolver.requireAdministrator(authentication);
        User employee = userService.createEmployee(
                administrator.getId(), request.username(), request.password());
        return Map.of("employee", employeeResponse(employee));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable long id,
            Authentication authentication) {
        User administrator =
                userResolver.requireAdministrator(authentication);
        userService.deleteEmployee(administrator.getId(), id);
        activeSessionRegistry.invalidate(id);
        return Map.of("message", "Mitarbeiterkonto wurde gelöscht.");
    }

    private static Map<String, Object> employeeResponse(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getName(),
                "role", "staff");
    }

    public record CreateEmployeeRequest(String username, String password) {}
}
