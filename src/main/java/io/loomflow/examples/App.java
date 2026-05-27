package io.loomflow.examples;

import io.loomflow.LoomFlow;

public class App {
    public static void main(String[] args) {
        LoomFlow.app()
            .service(UserService.class, UserServiceImpl.class)
            .route("GET", "/users/{id}", req -> {
                var controller = new UserController(new UserServiceImpl());
                return controller.getById(req);
            })
            .health("/health")
            .start();
    }
}
