package service;

import model.Report;
import model.Comment;
import model.User;
import model.enums.Category;
import model.enums.Role;
import model.enums.Status;
import org.junit.jupiter.api.Test;
import repository.interfaces.ReportRepository;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportVoteRepository;
import repository.interfaces.UserRepository;
import service.implementation.CommentServiceImpl;
import service.implementation.ReportVoteServiceImpl;
import service.implementation.ReportServiceImpl;
import service.implementation.UserServiceImpl;
import service.interfaces.PasswordHasher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendServiceTest {
    @Test
    void registrationNormalizesUsernameAndStoresHashedPassword() {
        MemoryUsers users = new MemoryUsers();
        UserServiceImpl service = new UserServiceImpl(users, new TestHasher());

        User registered = service.registerCitizen("Alice.Example", "long-password-123");

        assertEquals("alice.example", registered.getUsername());
        assertNotEquals("long-password-123", registered.getPasswordHash());
        assertEquals(registered.getId(), users.findByUsername("alice.example").getId());
    }

    @Test
    void registrationRejectsWeakPasswordsAndInvalidNames() {
        UserServiceImpl service = new UserServiceImpl(
                new MemoryUsers(), new TestHasher());

        assertThrows(IllegalArgumentException.class,
                () -> service.registerCitizen("ab", "long-password-123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerCitizen("valid_user", "short"));
    }

    @Test
    void loginChecksPasswordHash() {
        UserServiceImpl service = new UserServiceImpl(
                new MemoryUsers(), new TestHasher());
        service.registerCitizen("alice", "long-password-123");

        assertEquals("alice",
                service.login("ALICE", "long-password-123").getUsername());
        assertThrows(IllegalArgumentException.class,
                () -> service.login("alice", "wrong-password-123"));
    }

    @Test
    void onlyAdministratorCanCreateEmployeeAccounts() {
        MemoryUsers users = new MemoryUsers();
        UserServiceImpl service = new UserServiceImpl(users, new TestHasher());
        User citizen = service.registerCitizen("citizen", "citizen-password-123");
        User administrator = service.provisionAdmin(
                "admin", "administrator-password-123");

        assertThrows(SecurityException.class,
                () -> service.createEmployee(citizen.getId(),
                        "worker", "employee-password-123"));

        User employee = service.createEmployee(administrator.getId(),
                "Worker", "employee-password-123");
        assertEquals(Role.EMPLOYEE, employee.getRole());
        assertEquals("worker", employee.getUsername());
        assertEquals(List.of(employee), service.findEmployees());
        assertEquals("worker",
                service.login("worker", "employee-password-123").getUsername());
    }

    @Test
    void onlyAdministratorCanDeleteEmployeeAccounts() {
        MemoryUsers users = new MemoryUsers();
        UserServiceImpl service = new UserServiceImpl(users, new TestHasher());
        User citizen = service.registerCitizen("citizen", "citizen-password-123");
        User administrator = service.provisionAdmin(
                "admin", "administrator-password-123");
        User employee = service.createEmployee(administrator.getId(),
                "worker", "employee-password-123");

        assertThrows(SecurityException.class,
                () -> service.deleteEmployee(citizen.getId(), employee.getId()));
        service.deleteEmployee(administrator.getId(), employee.getId());

        assertTrue(service.findEmployees().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteEmployee(administrator.getId(),
                        employee.getId()));
    }

    @Test
    void creatingReportRequiresConcreteDefectAndValidCoordinates() {
        MemoryUsers users = new MemoryUsers();
        MemoryReports reports = new MemoryReports();
        User citizen = new User("alice", "alice", "hash", Role.CITIZEN);
        citizen.setId(1L);
        users.save(citizen);
        ReportServiceImpl service = new ReportServiceImpl(reports, users);

        assertThrows(IllegalArgumentException.class,
                () -> service.createReport(1L, "Damage", Category.POTHOLE,
                        "", "There is a problem.", "", 52.5, 13.4));
        assertThrows(IllegalArgumentException.class,
                () -> service.createReport(1L, "Damage", Category.POTHOLE,
                        "POTHOLE", "There is a problem.", "", 91, 13.4));

        Report report = service.createReport(1L, "Large pothole",
                Category.POTHOLE, "POTHOLE", "Near the crossing.",
                "", 52.5, 13.4);
        assertEquals("Large pothole", report.getTitle());
        assertEquals("POTHOLE", report.getDefectType());
        assertEquals(Status.NEW, report.getStatus());
    }

    @Test
    void onlyStaffCanChangeReportStatus() {
        MemoryUsers users = new MemoryUsers();
        MemoryReports reports = new MemoryReports();
        User citizen = new User("alice", "alice", "hash", Role.CITIZEN);
        citizen.setId(1L);
        users.save(citizen);
        Report report = new Report(1L, "Pothole", Category.POTHOLE,
                "POTHOLE", "Large pothole", "", 52.5, 13.4);
        report.setId(1L);
        reports.save(report);
        ReportServiceImpl service = new ReportServiceImpl(reports, users);

        assertThrows(SecurityException.class,
                () -> service.updateStatus(1L, Status.COMPLETED, 1L));

        User employee = new User("staff", "staff", "hash", Role.EMPLOYEE);
        employee.setId(2L);
        users.save(employee);
        service.updateStatus(1L, Status.COMPLETED, 2L);
        assertEquals(Status.COMPLETED, service.findById(1L).getStatus());
    }

    @Test
    void citizenCanVoteOnceAndStaffCannotVote() {
        MemoryUsers users = new MemoryUsers();
        MemoryReports reports = new MemoryReports();
        User citizen = new User("citizen", "citizen", "hash", Role.CITIZEN);
        users.save(citizen);
        User employee = new User("employee", "employee", "hash", Role.EMPLOYEE);
        users.save(employee);
        Report report = new Report(citizen.getId(), "Pothole", Category.POTHOLE,
                "POTHOLE", "Large pothole", "", 52.5, 13.4);
        reports.save(report);
        ReportVoteServiceImpl service = new ReportVoteServiceImpl(
                new MemoryVotes(), reports, users);

        assertTrue(service.vote(report.getId(), citizen.getId()));
        assertTrue(service.hasVoted(report.getId(), citizen.getId()));
        assertEquals(1, service.count(report.getId()));
        assertFalse(service.vote(report.getId(), citizen.getId()));
        assertEquals(Set.of(report.getId()),
                service.findReportIdsByUserId(citizen.getId()));
        assertTrue(service.removeVote(report.getId(), citizen.getId()));
        assertFalse(service.hasVoted(report.getId(), citizen.getId()));
        assertEquals(0, service.count(report.getId()));
        assertFalse(service.removeVote(report.getId(), citizen.getId()));
        assertThrows(SecurityException.class,
                () -> service.vote(report.getId(), employee.getId()));
        assertThrows(SecurityException.class,
                () -> service.removeVote(report.getId(), employee.getId()));
    }

    @Test
    void onlyCitizensCanAddValidProgressInformation() {
        MemoryUsers users = new MemoryUsers();
        MemoryReports reports = new MemoryReports();
        User citizen = new User("citizen", "citizen", "hash", Role.CITIZEN);
        users.save(citizen);
        User employee = new User("employee", "employee", "hash", Role.EMPLOYEE);
        users.save(employee);
        Report report = new Report(citizen.getId(), "Pothole", Category.POTHOLE,
                "POTHOLE", "Large pothole", "", 52.5, 13.4);
        reports.save(report);
        MemoryComments comments = new MemoryComments();
        CommentServiceImpl service = new CommentServiceImpl(
                comments, reports, users);

        Comment created = service.createComment(report.getId(), citizen.getId(),
                "  The city has placed a barrier.  ");

        assertEquals("The city has placed a barrier.", created.getText());
        assertEquals(1, service.findByReportId(report.getId()).size());
        assertEquals(Set.of(report.getId()),
                service.findReportIdsByUserId(citizen.getId()));
        Comment photoUpdate = service.createComment(report.getId(),
                citizen.getId(), "", "/uploads/update-photo.png");
        assertEquals("/uploads/update-photo.png", photoUpdate.getPhotoPath());
        assertThrows(SecurityException.class,
                () -> service.createComment(report.getId(), employee.getId(),
                        "A staff-only update"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createComment(report.getId(), citizen.getId(),
                        "x".repeat(1001)));
        assertThrows(IllegalArgumentException.class,
                () -> service.createComment(report.getId(), citizen.getId(),
                        "", ""));
    }

    private static final class TestHasher implements PasswordHasher {
        @Override
        public String hash(String password) {
            return "hashed:" + password;
        }

        @Override
        public boolean verify(String password, String passwordHash) {
            return hash(password).equals(passwordHash);
        }
    }

    private static final class MemoryUsers implements UserRepository {
        private final Map<Long, User> users = new HashMap<>();
        private long nextId = 1;

        @Override
        public void save(User user) {
            user.setId(nextId++);
            users.put(user.getId(), user);
        }

        @Override
        public User findById(long id) {
            return users.get(id);
        }

        @Override
        public User findByEmail(String email) {
            return users.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst().orElse(null);
        }

        @Override
        public User findByUsername(String username) {
            return users.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst().orElse(null);
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public void update(User user) {
            users.put(user.getId(), user);
        }

        @Override
        public void deleteById(long id) {
            users.remove(id);
        }
    }

    private static final class MemoryReports implements ReportRepository {
        private final Map<Long, Report> reports = new HashMap<>();
        private long nextId = 1;

        @Override
        public void save(Report report) {
            report.setId(nextId++);
            reports.put(report.getId(), report);
        }

        @Override
        public Report findById(long id) {
            return reports.get(id);
        }

        @Override
        public List<Report> findAll() {
            return new ArrayList<>(reports.values());
        }

        @Override
        public List<Report> findByStatus(Status status) {
            return reports.values().stream()
                    .filter(report -> report.getStatus() == status)
                    .toList();
        }

        @Override
        public void update(Report report) {
            reports.put(report.getId(), report);
        }

        @Override
        public void deleteById(long id) {
            reports.remove(id);
        }
    }

    private static final class MemoryVotes implements ReportVoteRepository {
        private final Set<String> votes = new HashSet<>();

        @Override
        public int countByReportId(long reportId) {
            return (int) votes.stream()
                    .filter(vote -> vote.startsWith(reportId + ":"))
                    .count();
        }

        @Override
        public boolean existsByReportIdAndUserId(long reportId, long userId) {
            return votes.contains(reportId + ":" + userId);
        }

        @Override
        public boolean addVote(long reportId, long userId) {
            return votes.add(reportId + ":" + userId);
        }

        @Override
        public boolean removeVote(long reportId, long userId) {
            return votes.remove(reportId + ":" + userId);
        }

        @Override
        public Set<Long> findReportIdsByUserId(long userId) {
            Set<Long> reportIds = new HashSet<>();
            for (String vote : votes) {
                String[] ids = vote.split(":");
                if (Long.parseLong(ids[1]) == userId) {
                    reportIds.add(Long.parseLong(ids[0]));
                }
            }
            return reportIds;
        }
    }

    private static final class MemoryComments implements CommentRepository {
        private final Map<Long, Comment> comments = new HashMap<>();
        private long nextId = 1;

        @Override
        public void save(Comment comment) {
            comment.setId(nextId++);
            comments.put(comment.getId(), comment);
        }

        @Override
        public Comment findById(long id) {
            return comments.get(id);
        }

        @Override
        public List<Comment> findByReportId(long reportId) {
            return comments.values().stream()
                    .filter(comment -> comment.getReportId() == reportId)
                    .toList();
        }

        @Override
        public List<Comment> findByUserId(long userId) {
            return comments.values().stream()
                    .filter(comment -> comment.getUserId() == userId)
                    .toList();
        }

        @Override
        public Set<Long> findReportIdsByUserId(long userId) {
            Set<Long> reportIds = new HashSet<>();
            comments.values().stream()
                    .filter(comment -> comment.getUserId() == userId)
                    .forEach(comment -> reportIds.add(comment.getReportId()));
            return reportIds;
        }

        @Override
        public void update(Comment comment) {
            comments.put(comment.getId(), comment);
        }

        @Override
        public void deleteById(long id) {
            comments.remove(id);
        }
    }
}
