package service.interfaces;

import model.User;

public interface UserService {

    User register(String name, String email, String password);

    User login(String email, String password);

    User findById(long id);
}