package io.loomflow.http;

public record Response(int status, String contentType, String body) {
    public static Response ok(String body) { return new Response(200, "text/plain", body); }
    public static Response created(String body) { return new Response(201, "text/plain", body); }
    public static Response badRequest(String body) { return new Response(400, "text/plain", body); }
    public static Response notFound() { return new Response(404, "text/plain", "Not Found"); }
    public static Response internalServerError() { return new Response(500, "text/plain", "Internal Server Error"); }
}
