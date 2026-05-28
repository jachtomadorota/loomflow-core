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
        String result = userService.findById(id);
        if (result == null) {
            return Response.notFound("User " + id + " not found");
        }
        return Response.ok("{\"id\":" + id + ",\"name\":\"" + result + "\"}");
    }

    public Response create(Request req) {
        String body = req.body();
        if (body == null || body.isBlank()) {
            return Response.badRequest("Request body must not be empty");
        }
        return Response.created("{\"message\":\"User created\",\"body\":" + body + "}");
    }
}
