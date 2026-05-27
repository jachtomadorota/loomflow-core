package io.loomflow.examples;

public final class UserServiceImpl implements UserService {
    @Override
    public String findById(long id) {
        return "User " + id;
    }
}
