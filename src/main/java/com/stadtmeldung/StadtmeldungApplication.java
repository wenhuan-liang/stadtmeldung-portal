package com.stadtmeldung;

import database.initialization.DatabaseInitializer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import model.User;
import model.enums.Role;
import repository.interfaces.UserRepository;
import service.interfaces.UserService;

import java.util.Locale;

@SpringBootApplication
public class StadtmeldungApplication {
    private static final Logger logger =
            LoggerFactory.getLogger(StadtmeldungApplication.class);

    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        SpringApplication.run(StadtmeldungApplication.class, args);
    }

    @Bean
    ApplicationRunner provisionInitialAdministrator(
            UserService userService, UserRepository userRepository) {
        return arguments -> {
            boolean hasAdministrator = userRepository.findAll().stream()
                    .anyMatch(user -> user.getRole() == Role.ADMIN);
            if (hasAdministrator) {
                return;
            }

            String username = System.getenv("PORTAL_ADMIN_USERNAME");
            String password = System.getenv("PORTAL_ADMIN_PASSWORD");
            if ((username == null) != (password == null)) {
                throw new IllegalStateException(
                        "PORTAL_ADMIN_USERNAME and PORTAL_ADMIN_PASSWORD must be configured together.");
            }
            if (username == null) {
                throw new IllegalStateException(
                        "No administrator account exists. For the first start only, set PORTAL_ADMIN_USERNAME and PORTAL_ADMIN_PASSWORD.");
            }

            User existing = userRepository.findByUsername(
                    username.trim().toLowerCase(Locale.ROOT));
            if (existing != null) {
                if (existing.getRole() != Role.ADMIN) {
                    throw new IllegalStateException(
                            "PORTAL_ADMIN_USERNAME belongs to an account with a different role.");
                }
                return;
            }

            userService.provisionAdmin(username, password);
            logger.info("Provisioned initial administrator account");
        };
    }
}
