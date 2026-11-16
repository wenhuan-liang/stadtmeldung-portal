package service.implementation.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-Tests für {@link BCryptPasswordHasher}. Hier wird bewusst nichts
 * gemockt: Geprüft wird das echte BCrypt-Verhalten.
 */
@DisplayName("BCryptPasswordHasher")
class BCryptPasswordHasherTest {

    private static final String PASSWORD = "sicheres-passwort-123";

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    @DisplayName("Hash enthält nicht das Klartext-Passwort und ist ein BCrypt-Hash")
    void hashDoesNotContainPlainPassword() {
        String hash = hasher.hash(PASSWORD);

        assertFalse(hash.contains(PASSWORD));
        assertTrue(hash.startsWith("$2a$12$"), "BCrypt mit Arbeitsfaktor 12 erwartet");
    }

    @Test
    @DisplayName("gleiches Passwort ergibt unterschiedliche Hashes (Salt)")
    void samePasswordGivesDifferentHashes() {
        assertNotEquals(hasher.hash(PASSWORD), hasher.hash(PASSWORD));
    }

    @Test
    @DisplayName("korrektes Passwort wird erkannt, falsches abgelehnt")
    void verifyChecksPassword() {
        String hash = hasher.hash(PASSWORD);

        assertTrue(hasher.verify(PASSWORD, hash));
        assertFalse(hasher.verify("falsches-passwort", hash));
    }

    @Test
    @DisplayName("kaputter Hash oder fehlende Werte führen zu false statt Absturz")
    void verifyHandlesInvalidInput() {
        assertFalse(hasher.verify(PASSWORD, "kein-bcrypt-hash"));
        assertFalse(hasher.verify(null, "hash"));
        assertFalse(hasher.verify(PASSWORD, null));
    }

    @Test
    @DisplayName("leeres Passwort kann nicht gehasht werden")
    void emptyPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }
}
