package service.interfaces;

public interface PasswordHasher {

    String hash(String password);

    boolean verify(String password, String passwordHash);

}
