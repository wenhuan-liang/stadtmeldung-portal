package repository.interfaces;

import java.util.List;

import model.User;

public interface UserRepository {

    void save(User user);

    User findById(long id);

    User findByEmail(String email);

    List<User> findAll();

    void update(User user);

    void deleteById(long id);
}

