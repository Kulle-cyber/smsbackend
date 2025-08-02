package com.example.controller;

import com.example.model.User;
import com.example.service.UserService;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgPool;

public class UserController {

    private final UserService userService;

    public UserController(PgPool client) {
        this.userService = new UserService(client);
    }

    public void mountRoutes(Router router) {
        router.route("/api/users*").handler(BodyHandler.create());

        router.get("/api/roles").handler(this::handleGetRoles);
        router.get("/api/users").handler(this::handleGetAllUsers);
        router.post("/api/users").handler(this::handleCreateUser);
        router.put("/api/users/:id/role").handler(this::handleUpdateUserRole);
    }

    private void handleGetRoles(RoutingContext ctx) {
        userService.getAllRoles()
            .onSuccess(roles -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(roles)))
            .onFailure(err -> {
                err.printStackTrace();
                ctx.response().setStatusCode(500).end("Failed to fetch roles");
            });
    }

    private void handleGetAllUsers(RoutingContext ctx) {
        userService.getAllUsers()
            .onSuccess(users -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(users)))
            .onFailure(err -> {
                err.printStackTrace();
                ctx.response().setStatusCode(500).end("Failed to fetch users");
            });
    }

    private void handleCreateUser(RoutingContext ctx) {
        try {
            JsonObject json = ctx.getBodyAsJson();
            System.out.println("Received create user JSON: " + json.encodePrettily());

            User user = json.mapTo(User.class);

            userService.createUser(user)
                .onSuccess(res -> ctx.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodePrettily(user)))
                .onFailure(err -> {
                    err.printStackTrace();  // Print full error for debugging
                    ctx.response()
                       .setStatusCode(400)
                       .end("Failed to create user: " + err.getMessage());
                });
        } catch (Exception e) {
            e.printStackTrace();  // Print parsing/mapping error
            ctx.response()
                .setStatusCode(400)
                .end("Invalid user data");
        }
    }

    private void handleUpdateUserRole(RoutingContext ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));
            JsonObject json = ctx.getBodyAsJson();
            int roleId = json.getInteger("roleId");

            userService.updateUserRole(userId, roleId)
                .onSuccess(res -> ctx.response()
                    .setStatusCode(200)
                    .end("Role updated successfully"))
                .onFailure(err -> ctx.response()
                    .setStatusCode(400)
                    .end("Failed to update role: " + err.getMessage()));
        } catch (Exception e) {
            ctx.response()
                .setStatusCode(400)
                .end("Invalid request data");
        }
    }
}
