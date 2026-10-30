package service.implementation.security;

import org.mindrot.jbcrypt.BCrypt;
import service.interfaces.PasswordHasher;

public class BCryptPasswordHasher implements PasswordHasher {

    private static final int WORK_FACTOR = 12;

    @Override
    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password must not be empty");
        }

        return BCrypt.hashpw(
                password,
                BCrypt.gensalt(WORK_FACTOR)
        );
    }

    @Override
    public boolean verify(String password, String passwordHash) {
        if (password == null || passwordHash == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}