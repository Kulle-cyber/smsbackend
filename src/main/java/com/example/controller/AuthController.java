package com.example.controller;

import com.example.service.AuthService;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class AuthController {
    private final AuthService authService;

    public AuthController(Vertx vertx, Router router, AuthService authService) {
        this.authService = authService;

        router.route().handler(BodyHandler.create());
        router.post("/api/login").handler(this::handleLogin);
    }

    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body.getString("username");
        String password = body.getString("password");

        // Check hardcoded admin credentials first
        if ("kulani".equals(username) && "123".equals(password)) {
            JsonObject response = new JsonObject()
                .put("message", "Admin login successful")
                .put("username", "kulani")
                .put("role", "admin"); // Or use any identifier your frontend expects

            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response.encode());
            return;
        }

        // Otherwise, authenticate against database
        authService.getUserByUsername(username).onSuccess(user -> {
            if (user == null || !authService.checkPassword(password, user.getPasswordHash())) {
                ctx.response().setStatusCode(401).end("Invalid credentials");
            } else {
                JsonObject response = new JsonObject()
                    .put("message", "Login successful")
                    .put("username", user.getUsername())
                    .put("role", user.getRoleId());
                ctx.response()
                   .putHeader("Content-Type", "application/json")
                   .end(response.encode());
            }
        }).onFailure(err -> {
            ctx.response().setStatusCode(500).end("Login failed: " + err.getMessage());
        });
    }
}
