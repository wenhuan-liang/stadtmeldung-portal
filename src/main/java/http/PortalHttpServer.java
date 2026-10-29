package http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Report;
import model.User;
import model.Comment;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.interfaces.ReportService;
import service.interfaces.CommentService;
import service.interfaces.ReportVoteService;
import service.interfaces.UserService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public final class PortalHttpServer {
    private static final Logger logger =
            LoggerFactory.getLogger(PortalHttpServer.class);
    private static final int MAX_REQUEST_BYTES = 8 * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60;
    private static final String SESSION_COOKIE = "STADTMELDUNG_SESSION";
    private static final Path FRONTEND_DIRECTORY =
            Path.of("public", "frontend").toAbsolutePath().normalize();
    private static final Path UPLOAD_DIRECTORY =
            Path.of("uploads").toAbsolutePath().normalize();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final HttpServer server;
    private final UserService userService;
    private final ReportService reportService;
    private final CommentService commentService;
    private final ReportVoteService reportVoteService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public PortalHttpServer(int port, UserService userService,
                            ReportService reportService,
                            CommentService commentService,
                            ReportVoteService reportVoteService)
            throws IOException {
        this.server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port),
                0);
        this.userService = userService;
        this.reportService = reportService;
        this.commentService = commentService;
        this.reportVoteService = reportVoteService;
        this.server.setExecutor(Executors.newFixedThreadPool(8));
        this.server.createContext("/api/", this::handleApi);
        this.server.createContext("/", this::handleStatic);
    }

    public void start() {
        server.start();
        logger.info("FixMyCity available at http://{}:{}",
                server.getAddress().getAddress().getHostAddress(),
                server.getAddress().getPort());
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        addSecurityHeaders(exchange);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/health".equals(path)) {
                sendJson(exchange, 200, Map.of("status", "ok"));
            } else if ("POST".equals(method) &&
                    "/api/login".equals(path)) {
                login(exchange, false);
            } else if ("POST".equals(method) &&
                    "/api/register".equals(path)) {
                login(exchange, true);
            } else if ("GET".equals(method) &&
                    "/api/admin/employees".equals(path)) {
                listEmployees(exchange);
            } else if ("POST".equals(method) &&
                    "/api/admin/employees".equals(path)) {
                createEmployee(exchange);
            } else if ("DELETE".equals(method) &&
                    path.matches("/api/admin/employees/\\d+")) {
                deleteEmployee(exchange, path);
            } else if ("POST".equals(method) &&
                    path.matches("/api/reports/\\d+/votes")) {
                voteForReport(exchange, path);
            } else if ("DELETE".equals(method) &&
                    path.matches("/api/reports/\\d+/votes")) {
                removeReportVote(exchange, path);
            } else if ("GET".equals(method) &&
                    path.matches("/api/reports/\\d+/comments")) {
                listReportComments(exchange, path);
            } else if ("POST".equals(method) &&
                    path.matches("/api/reports/\\d+/comments")) {
                createReportComment(exchange, path);
            } else if ("POST".equals(method) &&
                    "/api/logout".equals(path)) {
                logout(exchange);
            } else if ("GET".equals(method) &&
                    "/api/session".equals(path)) {
                Session session = requireSession(exchange);
                sendJson(exchange, 200, Map.of("user", publicUser(session)));
            } else if ("GET".equals(method) &&
                    "/api/reports".equals(path)) {
                getReports(exchange);
            } else if ("POST".equals(method) &&
                    "/api/reports".equals(path)) {
                createReport(exchange);
            } else if ("PATCH".equals(method) &&
                    path.startsWith("/api/reports/")) {
                updateReportStatus(exchange, path);
            } else {
                sendJson(exchange, 404, Map.of("message", "API endpoint not found."));
            }
        } catch (HttpFailure failure) {
            sendJson(exchange, failure.status, Map.of("message", failure.getMessage()));
        } catch (SecurityException e) {
            sendJson(exchange, 403, Map.of("message", "This action is not allowed."));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Request failed: {} {}", exchange.getRequestMethod(),
                    exchange.getRequestURI(), e);
            sendJson(exchange, 500,
                    Map.of("message", "A server error occurred."));
        } finally {
            exchange.close();
        }
    }

    private void login(HttpExchange exchange, boolean register)
            throws IOException {
        JsonObject body = readJson(exchange);
        String username = requiredString(body, "username");
        String password = requiredString(body, "password");
        User user;
        try {
            user = register
                    ? userService.registerCitizen(username, password)
                    : userService.login(username, password);
        } catch (IllegalArgumentException e) {
            int status = register ? 400 : 401;
            throw new HttpFailure(status,
                    register ? e.getMessage()
                            : "Benutzername oder Passwort ungültig.");
        }

        String token = newSessionToken();
        sessions.put(token, new Session(user.getId(), user.getRole(),
                user.getUsername(), user.getName(),
                Instant.now().getEpochSecond() + SESSION_TTL_SECONDS));
        exchange.getResponseHeaders().add("Set-Cookie",
                SESSION_COOKIE + "=" + token +
                        "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" +
                        SESSION_TTL_SECONDS);
        sendJson(exchange, register ? 201 : 200,
                Map.of("user", publicUser(sessions.get(token))));
    }

    private void logout(HttpExchange exchange) throws IOException {
        String token = sessionToken(exchange);
        if (token != null) {
            sessions.remove(token);
        }
        exchange.getResponseHeaders().add("Set-Cookie",
                SESSION_COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
        sendJson(exchange, 200, Map.of("message", "Abgemeldet."));
    }

    private void listEmployees(HttpExchange exchange) throws IOException {
        requireAdministrator(exchange);
        List<Map<String, Object>> employees = userService.findEmployees()
                .stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "displayName", user.getName(),
                        "role", "staff"))
                .toList();
        sendJson(exchange, 200, employees);
    }

    private void createEmployee(HttpExchange exchange) throws IOException {
        Session administrator = requireAdministrator(exchange);
        JsonObject body = readJson(exchange);
        String username = requiredString(body, "username");
        String password = requiredString(body, "password");
        try {
            User employee = userService.createEmployee(
                    administrator.userId, username, password);
            sendJson(exchange, 201, Map.of(
                    "employee", Map.of(
                            "id", employee.getId(),
                            "username", employee.getUsername(),
                            "displayName", employee.getName(),
                            "role", "staff")));
        } catch (IllegalArgumentException e) {
            throw new HttpFailure(400, e.getMessage());
        }
    }

    private void deleteEmployee(HttpExchange exchange, String path)
            throws IOException {
        Session administrator = requireAdministrator(exchange);
        long employeeId;
        try {
            employeeId = Long.parseLong(
                    path.substring("/api/admin/employees/".length()));
        } catch (NumberFormatException e) {
            throw new HttpFailure(400, "Ungültige Mitarbeiter-ID.");
        }
        userService.deleteEmployee(administrator.userId, employeeId);
        sessions.entrySet().removeIf(
                entry -> entry.getValue().userId == employeeId);
        sendJson(exchange, 200, Map.of(
                "message", "Mitarbeiterkonto wurde gelöscht."));
    }

    private Session requireAdministrator(HttpExchange exchange) {
        Session session = requireSession(exchange);
        if (session.role != model.enums.Role.ADMIN) {
            throw new SecurityException("Administrator access required");
        }
        return session;
    }

    private void voteForReport(HttpExchange exchange, String path)
            throws IOException {
        Session session = requireCitizen(exchange);
        long reportId = reportIdFromPath(path, "/votes");
        boolean added = reportVoteService.vote(reportId, session.userId);
        if (!added) {
            throw new HttpFailure(409, "Sie haben diese Meldung bereits unterstützt.");
        }
        sendJson(exchange, 201, Map.of(
                "voteCount", reportVoteService.count(reportId),
                "hasVoted", true));
    }

    private void removeReportVote(HttpExchange exchange, String path)
            throws IOException {
        Session session = requireCitizen(exchange);
        long reportId = reportIdFromPath(path, "/votes");
        reportVoteService.removeVote(reportId, session.userId);
        sendJson(exchange, 200, Map.of(
                "voteCount", reportVoteService.count(reportId),
                "hasVoted", false));
    }

    private void listReportComments(HttpExchange exchange, String path)
            throws IOException {
        long reportId = reportIdFromPath(path, "/comments");
        if (reportService.findById(reportId) == null) {
            throw new HttpFailure(404, "Meldung nicht gefunden.");
        }
        List<Map<String, Object>> comments = commentService
                .findByReportId(reportId).stream()
                .map(this::publicComment)
                .toList();
        sendJson(exchange, 200, comments);
    }

    private void createReportComment(HttpExchange exchange, String path)
            throws IOException {
        Session session = requireCitizen(exchange);
        long reportId = reportIdFromPath(path, "/comments");
        JsonObject body = readJson(exchange);
        String text = optionalString(body, "text").trim();
        String photoData = optionalString(body, "photoData");
        if (text.isBlank() && photoData.isBlank()) {
            throw new IllegalArgumentException(
                    "Bitte gib einen Text ein oder füge ein Foto hinzu.");
        }
        String photoPath = "";
        Path savedImage = null;
        if (!photoData.isBlank()) {
            ImageUpload image = decodeImage(photoData);
            Files.createDirectories(UPLOAD_DIRECTORY);
            String filename = java.util.UUID.randomUUID() + "." + image.extension;
            savedImage = UPLOAD_DIRECTORY.resolve(filename);
            Files.write(savedImage, image.bytes);
            photoPath = "/uploads/" + filename;
        }
        try {
            Comment comment = commentService.createComment(
                    reportId, session.userId, text, photoPath);
            sendJson(exchange, 201, Map.of("comment", publicComment(comment)));
        } catch (RuntimeException e) {
            if (savedImage != null) {
                Files.deleteIfExists(savedImage);
            }
            throw e;
        }
    }

    private Session requireCitizen(HttpExchange exchange) {
        Session session = requireSession(exchange);
        if (session.role != Role.CITIZEN) {
            throw new SecurityException("Only citizens can perform this action");
        }
        return session;
    }

    private long reportIdFromPath(String path, String suffix) {
        String idPart = path.substring("/api/reports/".length(),
                path.length() - suffix.length());
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new HttpFailure(400, "Ungültige Meldungs-ID.");
        }
    }

    private Map<String, Object> publicComment(Comment comment) {
        User author = userService.findById(comment.getUserId());
        return Map.of(
                "id", comment.getId(),
                "username", author == null ? "Bürger" : author.getUsername(),
                "text", comment.getText(),
                "photo", comment.getPhotoPath() == null
                        ? "" : comment.getPhotoPath(),
                "createdAt", comment.getCreatedAt() == null
                        ? "" : comment.getCreatedAt().toString());
    }

    private void getReports(HttpExchange exchange) throws IOException {
        boolean mine = exchange.getRequestURI().getRawQuery() != null &&
                List.of(exchange.getRequestURI().getRawQuery().split("&"))
                        .contains("mine=true");
        Session session = mine ? requireSession(exchange) : null;
        if (session != null && session.role != Role.CITIZEN) {
            throw new SecurityException("Citizen account required");
        }
        List<Map<String, Object>> reports = new ArrayList<>();
        Session viewer = optionalSession(exchange);
        java.util.Set<Long> relatedReportIds = mine
                ? new java.util.HashSet<>(
                        reportVoteService.findReportIdsByUserId(session.userId))
                : java.util.Set.of();
        java.util.Set<Long> votedReportIds = mine
                ? new java.util.HashSet<>(relatedReportIds)
                : java.util.Set.of();
        java.util.Set<Long> updatedReportIds = java.util.Set.of();
        if (mine) {
            updatedReportIds = commentService.findReportIdsByUserId(session.userId);
            relatedReportIds.addAll(updatedReportIds);
        }
        for (Report report : reportService.findAll()) {
            if (mine && (report.getUserId() == null ||
                    !report.getUserId().equals(session.userId)) &&
                    !relatedReportIds.contains(report.getId())) {
                continue;
            }
            Map<String, Object> result = publicReport(report, viewer);
            if (mine) {
                result.put("submittedByMe",
                        report.getUserId() != null &&
                                report.getUserId() == session.userId);
                result.put("votedByMe", votedReportIds.contains(report.getId()));
                result.put("updatedByMe", updatedReportIds.contains(report.getId()));
            }
            reports.add(result);
        }
        sendJson(exchange, 200, reports);
    }

    private void createReport(HttpExchange exchange) throws IOException {
        JsonObject body = readJson(exchange);
        String title = requiredString(body, "title").trim();
        String description = requiredString(body, "description").trim();
        String categoryValue = requiredString(body, "category");
        String defectType = optionalString(body, "defectType");
        double latitude = requiredNumber(body, "latitude");
        double longitude = requiredNumber(body, "longitude");
        String photoPath = "";
        Path savedImage = null;
        String imageData = optionalString(body, "photoData");

        if (!imageData.isBlank()) {
            ImageUpload image = decodeImage(imageData);
            Files.createDirectories(UPLOAD_DIRECTORY);
            String filename = java.util.UUID.randomUUID() + "." + image.extension;
            savedImage = UPLOAD_DIRECTORY.resolve(filename);
            Files.write(savedImage, image.bytes);
            photoPath = "/uploads/" + filename;
        }

        try {
            Session session = optionalSession(exchange);
            Report report = reportService.createReport(
                    session == null ? null : session.userId,
                    title,
                    parseCategory(categoryValue),
                    defectType,
                    description,
                    photoPath,
                    latitude,
                    longitude);
            sendJson(exchange, 201, Map.of(
                    "report", publicReport(report, optionalSession(exchange))));
        } catch (RuntimeException e) {
            if (savedImage != null) {
                Files.deleteIfExists(savedImage);
            }
            throw e;
        }
    }

    private void updateReportStatus(HttpExchange exchange, String path)
            throws IOException {
        Session session = requireSession(exchange);
        if (session.role != model.enums.Role.EMPLOYEE &&
                session.role != model.enums.Role.ADMIN) {
            throw new SecurityException("Staff access required");
        }
        String idPart = path.substring("/api/reports/".length());
        long reportId;
        try {
            reportId = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new HttpFailure(400, "Ungültige Meldungs-ID.");
        }
        Report currentReport = reportService.findById(reportId);
        if (currentReport == null) {
            throw new HttpFailure(404, "Meldung nicht gefunden.");
        }
        JsonObject body = readJson(exchange);
        Status status = parseStatus(requiredString(body, "status"));
        reportService.updateStatus(reportId, status, session.userId);
        Report report = reportService.findById(reportId);
        if (report == null) {
            throw new HttpFailure(404, "Meldung nicht gefunden.");
        }
        sendJson(exchange, 200, Map.of(
                "report", publicReport(report, session)));
    }

    private JsonObject readJson(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(
                MAX_REQUEST_BYTES + 1);
        if (bytes.length > MAX_REQUEST_BYTES) {
            throw new HttpFailure(413, "Die Anfrage ist zu groß.");
        }
        try {
            JsonElement parsed = JsonParser.parseString(
                    new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new HttpFailure(400, "Ungültige JSON-Anfrage.");
            }
            return parsed.getAsJsonObject();
        } catch (com.google.gson.JsonParseException e) {
            throw new HttpFailure(400, "Ungültige JSON-Anfrage.");
        }
    }

    private Session requireSession(HttpExchange exchange) {
        Session session = optionalSession(exchange);
        if (session == null) {
            throw new HttpFailure(401, "Bitte melden Sie sich an.");
        }
        return session;
    }

    private Session optionalSession(HttpExchange exchange) {
        String token = sessionToken(exchange);
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session != null &&
                session.expiresAt < Instant.now().getEpochSecond()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    private String sessionToken(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String cookie : cookies.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && SESSION_COOKIE.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private static Map<String, Object> publicUser(Session session) {
        return Map.of(
                "username", session.username,
                "displayName", session.displayName,
                "role", switch (session.role) {
                    case CITIZEN -> "user";
                    case EMPLOYEE -> "staff";
                    case ADMIN -> "admin";
                });
    }

    private Map<String, Object> publicReport(Report report, Session viewer) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", report.getId());
        result.put("title", report.getTitle());
        result.put("category", categoryLabel(report.getCategory()));
        result.put("defectType", report.getDefectType() == null
                ? "" : report.getDefectType());
        result.put("description", report.getDescription());
        result.put("latitude", report.getLatitude());
        result.put("longitude", report.getLongitude());
        result.put("status", switch (report.getStatus()) {
                    case NEW -> "new";
                    case IN_PROGRESS -> "in_progress";
                    case COMPLETED -> "resolved";
                });
        result.put("photo", report.getPhotoPath() == null
                ? "" : report.getPhotoPath());
        result.put("submittedBy", report.getUserId());
        result.put("voteCount", reportVoteService.count(report.getId()));
        result.put("hasVoted", viewer != null &&
                viewer.role == Role.CITIZEN &&
                reportVoteService.hasVoted(report.getId(), viewer.userId));
        result.put("createdAt", report.getCreatedAt() == null
                ? "" : report.getCreatedAt().toString());
        return result;
    }

    private static Category parseCategory(String value) {
        return switch (value) {
            case "Defekt" -> Category.POTHOLE;
            case "Verunreinigung" -> Category.LITTER;
            case "Parkvergehen" -> Category.PARKING_VIOLATION;
            case "Sonstiges" -> Category.OTHER;
            default -> throw new IllegalArgumentException(
                    "Ungültige Kategorie.");
        };
    }

    private static String categoryLabel(Category category) {
        return switch (category) {
            case POTHOLE, STREETLIGHT -> "Defekt";
            case LITTER -> "Verunreinigung";
            case PARKING_VIOLATION -> "Parkvergehen";
            case OTHER -> "Sonstiges";
        };
    }

    private static Status parseStatus(String value) {
        return switch (value) {
            case "new" -> Status.NEW;
            case "in_progress" -> Status.IN_PROGRESS;
            case "resolved" -> Status.COMPLETED;
            default -> throw new IllegalArgumentException(
                    "Ungültiger Status.");
        };
    }

    private static String requiredString(JsonObject body, String key) {
        String value = optionalString(body, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Bitte füllen Sie alle Pflichtfelder aus.");
        }
        return value;
    }

    private static String optionalString(JsonObject body, String key) {
        JsonElement element = body.get(key);
        return element == null || element.isJsonNull()
                ? "" : element.getAsString();
    }

    private static double requiredNumber(JsonObject body, String key) {
        try {
            return body.get(key).getAsDouble();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Bitte wählen Sie einen gültigen Standort.");
        }
    }

    private static ImageUpload decodeImage(String dataUrl) {
        String[] parts = dataUrl.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Ungültiges Foto.");
        }
        String extension;
        if ("data:image/jpeg;base64".equals(parts[0])) {
            extension = "jpg";
        } else if ("data:image/png;base64".equals(parts[0])) {
            extension = "png";
        } else if ("data:image/webp;base64".equals(parts[0])) {
            extension = "webp";
        } else if ("data:image/gif;base64".equals(parts[0])) {
            extension = "gif";
        } else {
            throw new IllegalArgumentException(
                    "Nur JPG-, PNG-, GIF- oder WEBP-Fotos sind erlaubt.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ungültiges Foto.");
        }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "Fotos dürfen höchstens 5 MB groß sein.");
        }
        return new ImageUpload(bytes, extension);
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        addSecurityHeaders(exchange);
        try {
            if (!"GET".equals(exchange.getRequestMethod()) &&
                    !"HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            Path base = path.startsWith("/uploads/")
                    ? UPLOAD_DIRECTORY : FRONTEND_DIRECTORY;
            String relative = path.startsWith("/uploads/")
                    ? path.substring("/uploads/".length())
                    : path.equals("/") ? "login.html"
                    : path.substring(1);
            Path file = base.resolve(relative).normalize();
            if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] contents = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Content-Type",
                    contentType(file));
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, contents.length);
                exchange.getResponseBody().write(contents);
            }
        } finally {
            exchange.close();
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static void sendJson(HttpExchange exchange, int status,
                                 Object response) throws IOException {
        byte[] bytes = new com.google.gson.Gson().toJson(response)
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set(
                "Referrer-Policy", "strict-origin-when-cross-origin");
    }

    private static String newSessionToken() {
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        return HexFormat.of().formatHex(token);
    }

    private record Session(long userId, model.enums.Role role,
                           String username, String displayName,
                           long expiresAt) { }

    private record ImageUpload(byte[] bytes, String extension) { }

    private static final class HttpFailure extends RuntimeException {
        private final int status;

        private HttpFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
