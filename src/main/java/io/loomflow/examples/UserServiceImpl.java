package io.loomflow.examples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UserServiceImpl implements UserService {

    private final Map<Long, String> users = new ConcurrentHashMap<>(Map.of(
            1L, "Alice",
            2L, "Bob",
            3L, "Charlie"
    ));

    @Override
    public String findById(long id) {
        return users.get(id);
    }
}
