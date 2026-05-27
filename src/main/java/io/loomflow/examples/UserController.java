package io.loomflow.examples;

import io.loomflow.http.Request;
import io.loomflow.http.Response;

public final class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public Response getById(Request req) {
        long id = req.pathLong("id");
        return Response.ok(userService.findById(id));
    }
}
