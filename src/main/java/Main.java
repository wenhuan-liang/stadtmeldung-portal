import database.connection.ConnectionProvider;
import database.connection.DatabaseConnectionProvider;
import database.initialization.DatabaseInitializer;
import http.PortalHttpServer;
import model.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.implementation.DatabaseReportRepository;
import repository.implementation.DatabaseCommentRepository;
import repository.implementation.DatabaseReportVoteRepository;
import repository.implementation.DatabaseUserRepository;
import repository.implementation.mapper.CommentResultSetMapper;
import repository.implementation.mapper.ReportResultSetMapper;
import repository.implementation.mapper.UserResultSetMapper;
import service.implementation.ReportServiceImpl;
import service.implementation.CommentServiceImpl;
import service.implementation.ReportVoteServiceImpl;
import service.implementation.UserServiceImpl;
import service.implementation.security.BCryptPasswordHasher;
import service.interfaces.UserService;
import java.util.Locale;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        ConnectionProvider connections = new DatabaseConnectionProvider();
        DatabaseUserRepository users = new DatabaseUserRepository(
                connections, new UserResultSetMapper());
        DatabaseReportRepository reports = new DatabaseReportRepository(
                connections, new ReportResultSetMapper());
        DatabaseCommentRepository comments = new DatabaseCommentRepository(
                connections, new CommentResultSetMapper());
        DatabaseReportVoteRepository votes =
                new DatabaseReportVoteRepository(connections);
        UserServiceImpl userService = new UserServiceImpl(
                users, new BCryptPasswordHasher());
        provisionInitialAdministrator(userService, users);

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORTAL_PORT", "3000"));
        try {
            new PortalHttpServer(port, userService,
                    new ReportServiceImpl(reports, users),
                    new CommentServiceImpl(comments, reports, users),
                    new ReportVoteServiceImpl(votes, reports, users)).start();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "The local web server could not be started.", e);
        }
    }

    private static void provisionInitialAdministrator(
            UserService service, DatabaseUserRepository users) {
        boolean hasAdministrator = users.findAll().stream()
                .anyMatch(user -> user.getRole() == Role.ADMIN);
        if (hasAdministrator) {
            return;
        }
        provision(service, users, "PORTAL_ADMIN_USERNAME",
                "PORTAL_ADMIN_PASSWORD", Role.ADMIN);
        if (users.findAll().stream().noneMatch(
                user -> user.getRole() == Role.ADMIN)) {
            throw new IllegalStateException(
                    "No administrator account exists. For the first start only, set " +
                    "PORTAL_ADMIN_USERNAME and PORTAL_ADMIN_PASSWORD.");
        }
    }

    private static void provision(UserService service,
                                  DatabaseUserRepository users,
                                  String usernameVariable,
                                  String passwordVariable, Role role) {
        String username = System.getenv(usernameVariable);
        String password = System.getenv(passwordVariable);
        if ((username == null) != (password == null)) {
            throw new IllegalStateException(
                    usernameVariable + " and " + passwordVariable +
                    " must be configured together.");
        }
        if (username == null) {
            return;
        }
        var existing = users.findByUsername(
                username.trim().toLowerCase(Locale.ROOT));
        if (existing != null) {
            if (existing.getRole() != role) {
                throw new IllegalStateException(
                        usernameVariable + " belongs to an account with a different role.");
            }
            logger.info("Initial {} account already exists", role);
            return;
        }
        service.provisionAdmin(username, password);
        logger.info("Provisioned initial {} account", role);
    }
}