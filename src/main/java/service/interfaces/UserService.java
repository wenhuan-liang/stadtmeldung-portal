package service.interfaces;

import model.User;

import java.util.List;

public interface UserService {

    User register(String name, String email, String password);

    User registerCitizen(String username, String password);

    User provisionAdmin(String username, String password);

    User createEmployee(long administratorId, String username,
                        String password);

    List<User> findEmployees();

    void deleteEmployee(long administratorId, long employeeId);

    User login(String username, String password);

    User findById(long id);
}
