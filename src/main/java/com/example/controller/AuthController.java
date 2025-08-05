package com.example.controller;

import com.example.service.AuthService;
import com.example.service.RoleService;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class AuthController {
    private final AuthService authService;
    private final RoleService roleService; // ✅ Add this

    public AuthController(Vertx vertx, Router router, AuthService authService, RoleService roleService) { // ✅ Add RoleService param
        this.authService = authService;
        this.roleService = roleService;

        router.route().handler(BodyHandler.create());
        router.post("/api/login").handler(this::handleLogin);
    }

    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body.getString("username");
        String password = body.getString("password");

        System.out.println("Login attempt for username: " + username);

        // ✅ Hardcoded admin
        if ("kulani".equals(username) && "123".equals(password)) {
            System.out.println("Hardcoded admin login successful.");
            JsonObject response = new JsonObject()
                .put("message", "Admin login successful")
                .put("username", "kulani")
                .put("role", "admin");

            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response.encode());
            return;
        }

        // ✅ Existing DB user logic
        authService.getUserByUsername(username).onSuccess(user -> {
            if (user == null) {
                System.out.println("User not found for username: " + username);
                ctx.response().setStatusCode(401).end("Invalid credentials");
                return;
            }

            System.out.println("User found: " + user.getUsername());
            System.out.println("Stored hash: '" + user.getPasswordHash() + "'");
            boolean passwordMatches = authService.checkPassword(password, user.getPasswordHash());
            System.out.println("Password matches? " + passwordMatches);

            if (!passwordMatches) {
                ctx.response().setStatusCode(401).end("Invalid credentials");
            } else {
                int roleId = user.getRoleId();

                // ✅ Fetch role name from RoleService
                roleService.getRoles().onSuccess(roles -> {
                    String roleName = roles.stream()
                        .filter(role -> role.getInteger("id") == roleId)
                        .map(role -> role.getString("name").toLowerCase())
                        .findFirst()
                        .orElse("unknown");

                    JsonObject response = new JsonObject()
                        .put("message", "Login successful")
                        .put("username", user.getUsername())
                        .put("role", roleName); // ✅ Use role name

                    ctx.response()
                       .putHeader("Content-Type", "application/json")
                       .end(response.encode());
                }).onFailure(err -> {
                    ctx.response().setStatusCode(500).end("Failed to load role");
                });
            }
        }).onFailure(err -> {
            ctx.response().setStatusCode(500).end("Login failed: " + err.getMessage());
        });
    }
}
