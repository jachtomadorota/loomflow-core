
# LoomFlow Core

LoomFlow Core is a lightweight Java backend core built for virtual threads, explicit dependency injection, and zero-magic routing.
It is designed to be small, predictable, and easy to understand from the first read.

## Why LoomFlow?

Most Java backend frameworks trade simplicity for abstraction. LoomFlow takes the opposite approach: keep the runtime lean, make behavior explicit, and let virtual threads handle concurrency without forcing async complexity into every layer.

## Goals

- Virtual threads first.
- Explicit code over hidden magic.
- Compile-time friendly architecture.
- Fast startup and low memory footprint.
- Clear debugging and predictable runtime behavior.

## What it includes

- HTTP server integration.
- Route registration.
- Middleware pipeline.
- Request and response abstractions.
- Constructor-based DI.
- Validation hooks.
- Global error handling.
- Health checks.

## What it does not try to be

- A full enterprise platform.
- An ORM framework.
- A reactive framework by default.
- A heavy annotation-driven ecosystem.
- A replacement for every Spring use case.

## Installation

```xml
<dependency>
  <groupId>io.loomflow</groupId>
  <artifactId>loomflow-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick start

```java
public class App {
    public static void main(String[] args) {
        LoomFlow.app()
            .service(UserService.class, UserServiceImpl.class)
            .route("GET", "/users/{id}", UserController::getById)
            .health("/health")
            .start();
    }
}
```

```java
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
```

## Core concepts

### Application builder
The application starts from a fluent builder that registers services, routes, and infrastructure components explicitly.

### Virtual-thread execution model
Every request is handled in a virtual thread by default, so blocking code remains readable without sacrificing concurrency.

### Explicit dependency injection
LoomFlow prefers constructor injection and build-time wiring over runtime scanning and reflection.

### Routing
Routes are registered directly in code, which keeps request flow easy to trace and debug.

### Error handling
Errors are mapped through a global handler so that validation failures, not-found responses, and internal exceptions remain consistent.

## Example project structure

```text
src/
  main/
    java/
      app/
        App.java
        UserController.java
        UserService.java
        UserServiceImpl.java
        dto/
          UserCreateRequest.java
          UserResponse.java
```

## Roadmap

- v0.1: HTTP core, routing, DI, error handling.
- v0.2: validation, observability, test support.
- v0.3: build-time generation improvements.
- v1.0: stable API and production-ready release.

## Contributing

Contributions are welcome. If you want to help, start by opening an issue or discussing the proposed change before writing large implementation work.

Please keep the design principles in mind:
- make the API explicit,
- avoid unnecessary magic,
- prefer simple runtime behavior,
- optimize for readability and predictability.

## License

This project is licensed under the Apache License 2.0.
