package com.example.controller;

import com.example.service.AuthService;
import com.example.service.RoleService;
import com.example.util.JwtUtil;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class AuthController {
    private final AuthService authService;
    private final RoleService roleService;

    public AuthController(Vertx vertx, Router router, AuthService authService, RoleService roleService) {
        this.authService = authService;
        this.roleService = roleService;

        router.route().handler(BodyHandler.create());
        router.post("/api/login").handler(this::handleLogin);
    }

    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body.getString("username");
        String password = body.getString("password");

        System.out.println("Login attempt for username/email: " + username);

        // Hardcoded admin login
        if ("kulani".equals(username) && "123".equals(password)) {
            System.out.println("Hardcoded admin login successful.");
            String token = JwtUtil.generateToken(0, "kulani", "admin");

            JsonObject response = new JsonObject()
                .put("message", "Admin login successful")
                .put("username", "kulani")
                .put("role", "admin")
                .put("token", token);

            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response.encode());
            return;
        }

        // First: check system users
        authService.getUserByUsername(username).onSuccess(user -> {
            if (user != null) {
                System.out.println("User found: " + user.getUsername());
                boolean passwordMatches = authService.checkPassword(password, user.getPasswordHash());
                if (!passwordMatches) {
                    ctx.response().setStatusCode(401).end("Invalid credentials");
                    return;
                }

                int roleId = user.getRoleId();
                roleService.getRoles().onSuccess(roles -> {
                    String roleName = roles.stream()
                        .filter(role -> role.getInteger("id") == roleId)
                        .map(role -> role.getString("name").toLowerCase())
                        .findFirst()
                        .orElse("unknown");

                    String token = JwtUtil.generateToken(user.getId(), user.getUsername(), roleName);

                    JsonObject response = new JsonObject()
                        .put("message", "Login successful")
                        .put("username", user.getUsername())
                        .put("role", roleName)
                        .put("token", token);

                    ctx.response()
                       .putHeader("Content-Type", "application/json")
                       .end(response.encode());
                }).onFailure(err -> ctx.response().setStatusCode(500).end("Failed to load role"));
                return;
            }

            // Second: check customers by email
            authService.getCustomerByEmail(username).onSuccess(customer -> {
                if (customer == null) {
                    ctx.response().setStatusCode(401).end("Invalid credentials");
                    return;
                }

                boolean passwordMatches = authService.checkPassword(password, customer.getPassword());
                if (!passwordMatches) {
                    ctx.response().setStatusCode(401).end("Invalid credentials");
                    return;
                }

                String token = JwtUtil.generateToken(customer.getId(), customer.getEmail(), "customer");

                JsonObject response = new JsonObject()
                    .put("message", "Customer login successful")
                    .put("username", customer.getEmail())
                    .put("role", "customer")
                    .put("token", token);

                ctx.response()
                   .putHeader("Content-Type", "application/json")
                   .end(response.encode());
            }).onFailure(err -> {
                ctx.response().setStatusCode(500).end("Login failed: " + err.getMessage());
            });

        }).onFailure(err -> {
            ctx.response().setStatusCode(500).end("Login failed: " + err.getMessage());
        });
    }
}
