package service.implementation;

import model.User;
import model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.interfaces.UserRepository;
import service.interfaces.PasswordHasher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link UserServiceImpl}.
 *
 * Datenbank (UserRepository) und Passwort-Hashing (PasswordHasher) werden
 * mit Mockito ersetzt. So wird ausschließlich die Logik des Services geprüft.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceImplTest {

    private static final String VALID_PASSWORD = "sicheres-passwort-123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private UserServiceImpl userService;

    private static User user(long id, String username, Role role) {
        User user = new User(username, username, "hash-" + username, role);
        user.setId(id);
        return user;
    }

    @Nested
    @DisplayName("Registrierung von Bürgern")
    class RegisterCitizen {

        @Test
        @DisplayName("speichert Bürger mit gehashtem Passwort und normalisiertem Namen")
        void savesCitizenWithHashedPassword() {
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("HASH");

            User result = userService.registerCitizen("  Max.Muster ", VALID_PASSWORD);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertSame(result, saved.getValue());
            assertEquals("max.muster", result.getUsername());
            assertEquals("HASH", result.getPasswordHash());
            assertEquals(Role.CITIZEN, result.getRole());
        }

        @Test
        @DisplayName("lehnt bereits vergebenen Benutzernamen ab")
        void rejectsDuplicateUsername() {
            when(userRepository.findByUsername("max"))
                    .thenReturn(user(1, "max", Role.CITIZEN));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.registerCitizen("MAX", VALID_PASSWORD));
            verify(userRepository, never()).save(any());
        }

        @ParameterizedTest(name = "ungültiger Benutzername: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "ab", "name mit leerzeichen", "müller",
                "max@mail", "a23456789012345678901234567890123"})
        @DisplayName("lehnt ungültige Benutzernamen ab")
        void rejectsInvalidUsernames(String username) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.registerCitizen(username, VALID_PASSWORD));
            verifyNoInteractions(userRepository, passwordHasher);
        }

        @ParameterizedTest(name = "gültiger Benutzername: \"{0}\"")
        @ValueSource(strings = {"abc", "max_muster-1.0", "a2345678901234567890123456789012"})
        @DisplayName("akzeptiert Benutzernamen an den Grenzen (3 und 32 Zeichen)")
        void acceptsBoundaryUsernames(String username) {
            when(passwordHasher.hash(anyString())).thenReturn("HASH");

            userService.registerCitizen(username, VALID_PASSWORD);

            verify(userRepository).save(any(User.class));
        }

        @ParameterizedTest(name = "ungültiges Passwort mit Länge {0}")
        @ValueSource(ints = {0, 1, 11, 73, 100})
        @DisplayName("lehnt Passwörter unter 12 oder über 72 Zeichen ab")
        void rejectsPasswordsOutsideLengthLimits(int length) {
            String password = "x".repeat(length);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.registerCitizen("max", password));
            verify(userRepository, never()).save(any());
        }

        @ParameterizedTest(name = "gültiges Passwort mit Länge {0}")
        @ValueSource(ints = {12, 72})
        @DisplayName("akzeptiert Passwörter an den Grenzen (12 und 72 Zeichen)")
        void acceptsPasswordsAtLengthLimits(int length) {
            when(passwordHasher.hash(anyString())).thenReturn("HASH");

            userService.registerCitizen("max", "x".repeat(length));

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("zählt Bytes statt Zeichen: 37 Umlaute (74 Bytes) sind zu lang")
        void rejectsPasswordOverSeventyTwoBytes() {
            // BCrypt verarbeitet nur 72 Bytes. Ein "ü" belegt in UTF-8 zwei Bytes.
            String password = "ü".repeat(37);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.registerCitizen("max", password));
        }

        @Test
        @DisplayName("Passwort wird nie im Klartext gespeichert")
        void neverStoresPlainPassword() {
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("HASH");

            User result = userService.registerCitizen("max", VALID_PASSWORD);

            verify(passwordHasher).hash(VALID_PASSWORD);
            assertEquals("HASH", result.getPasswordHash());
        }
    }

    @Nested
    @DisplayName("Registrierung per E-Mail")
    class RegisterWithEmail {

        @Test
        @DisplayName("normalisiert die E-Mail-Adresse auf Kleinbuchstaben")
        void normalizesEmail() {
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("HASH");

            User result = userService.register("Max", " Max@Example.DE ", VALID_PASSWORD);

            assertEquals("max@example.de", result.getEmail());
            assertEquals(Role.CITIZEN, result.getRole());
            verify(userRepository).save(result);
        }

        @ParameterizedTest(name = "ungültige E-Mail: \"{0}\"")
        @ValueSource(strings = {"keine-mail", "max@", "@example.de", "max@example", "max @example.de"})
        @DisplayName("lehnt ungültige E-Mail-Adressen ab")
        void rejectsInvalidEmail(String email) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.register("Max", email, VALID_PASSWORD));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("lehnt bereits registrierte E-Mail-Adresse ab")
        void rejectsDuplicateEmail() {
            when(userRepository.findByEmail("max@example.de"))
                    .thenReturn(user(1, "max@example.de", Role.CITIZEN));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.register("Max", "max@example.de", VALID_PASSWORD));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("meldet Benutzer mit korrektem Passwort an (Groß-/Kleinschreibung egal)")
        void loginWithCorrectPassword() {
            User max = user(1, "max", Role.CITIZEN);
            when(userRepository.findByUsername("max")).thenReturn(max);
            when(passwordHasher.verify(VALID_PASSWORD, max.getPasswordHash())).thenReturn(true);

            assertSame(max, userService.login("  MAX ", VALID_PASSWORD));
        }

        @Test
        @DisplayName("sucht bei Eingabe mit @ nach der E-Mail-Adresse")
        void loginWithEmailUsesEmailLookup() {
            User max = user(1, "max@example.de", Role.CITIZEN);
            when(userRepository.findByEmail("max@example.de")).thenReturn(max);
            when(passwordHasher.verify(anyString(), anyString())).thenReturn(true);

            userService.login("Max@Example.de", VALID_PASSWORD);

            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("gleiche Fehlermeldung bei falschem Passwort und unbekanntem Benutzer")
        void sameErrorForWrongPasswordAndUnknownUser() {
            // Sicherheit: Angreifer sollen nicht herausfinden können,
            // welche Benutzernamen existieren.
            User max = user(1, "max", Role.CITIZEN);
            when(userRepository.findByUsername("max")).thenReturn(max);
            when(passwordHasher.verify("falsches-passwort", max.getPasswordHash())).thenReturn(false);

            IllegalArgumentException wrongPassword = assertThrows(IllegalArgumentException.class,
                    () -> userService.login("max", "falsches-passwort"));
            IllegalArgumentException unknownUser = assertThrows(IllegalArgumentException.class,
                    () -> userService.login("unbekannt", VALID_PASSWORD));

            assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        }

        @Test
        @DisplayName("lehnt fehlende Zugangsdaten ab, ohne die Datenbank abzufragen")
        void rejectsNullCredentials() {
            assertThrows(IllegalArgumentException.class, () -> userService.login(null, VALID_PASSWORD));
            assertThrows(IllegalArgumentException.class, () -> userService.login("max", null));
            verifyNoInteractions(userRepository, passwordHasher);
        }
    }

    @Nested
    @DisplayName("Mitarbeiterverwaltung")
    class EmployeeManagement {

        @Test
        @DisplayName("Admin kann Mitarbeiter anlegen")
        void adminCreatesEmployee() {
            when(userRepository.findById(1L)).thenReturn(user(1, "admin", Role.ADMIN));
            when(passwordHasher.hash(VALID_PASSWORD)).thenReturn("HASH");

            User employee = userService.createEmployee(1L, "Mitarbeiter1", VALID_PASSWORD);

            assertEquals(Role.EMPLOYEE, employee.getRole());
            assertEquals("mitarbeiter1", employee.getUsername());
            verify(userRepository).save(employee);
        }

        @ParameterizedTest(name = "Rolle {0} darf keine Mitarbeiter anlegen")
        @ValueSource(strings = {"CITIZEN", "EMPLOYEE"})
        @DisplayName("nur Admins dürfen Mitarbeiter anlegen")
        void nonAdminCannotCreateEmployee(Role role) {
            when(userRepository.findById(2L)).thenReturn(user(2, "nutzer", role));

            assertThrows(SecurityException.class,
                    () -> userService.createEmployee(2L, "neu", VALID_PASSWORD));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("unbekannte Admin-ID wird als fehlende Berechtigung behandelt")
        void unknownAdminCannotCreateEmployee() {
            when(userRepository.findById(99L)).thenReturn(null);

            assertThrows(SecurityException.class,
                    () -> userService.createEmployee(99L, "neu", VALID_PASSWORD));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Admin-ID 0 ist ungültig")
        void invalidAdminId() {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.createEmployee(0L, "neu", VALID_PASSWORD));
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Admin kann Mitarbeiter löschen")
        void adminDeletesEmployee() {
            when(userRepository.findById(1L)).thenReturn(user(1, "admin", Role.ADMIN));
            when(userRepository.findById(5L)).thenReturn(user(5, "worker", Role.EMPLOYEE));

            userService.deleteEmployee(1L, 5L);

            verify(userRepository).deleteById(5L);
        }

        @ParameterizedTest(name = "Konto mit Rolle {0} kann nicht als Mitarbeiter gelöscht werden")
        @ValueSource(strings = {"CITIZEN", "ADMIN"})
        @DisplayName("über die Mitarbeiterverwaltung lassen sich nur Mitarbeiterkonten löschen")
        void cannotDeleteNonEmployeeAccounts(Role role) {
            when(userRepository.findById(1L)).thenReturn(user(1, "admin", Role.ADMIN));
            when(userRepository.findById(5L)).thenReturn(user(5, "ziel", role));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.deleteEmployee(1L, 5L));
            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Mitarbeiter darf keine anderen Mitarbeiter löschen")
        void employeeCannotDeleteEmployee() {
            when(userRepository.findById(2L)).thenReturn(user(2, "worker", Role.EMPLOYEE));

            assertThrows(SecurityException.class,
                    () -> userService.deleteEmployee(2L, 5L));
            verify(userRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Mitarbeiterliste enthält nur Mitarbeiter, keine Bürger oder Admins")
        void findEmployeesFiltersByRole() {
            User citizen = user(1, "buerger", Role.CITIZEN);
            User employee = user(2, "worker", Role.EMPLOYEE);
            User admin = user(3, "admin", Role.ADMIN);
            when(userRepository.findAll()).thenReturn(List.of(citizen, employee, admin));

            assertEquals(List.of(employee), userService.findEmployees());
        }
    }
}
