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
        router.get("/api/users/search").handler(this::handleSearchUsers);
        router.get("/api/users/:id").handler(this::handleGetUserById);
        router.post("/api/users").handler(this::handleCreateUser);
       // router.put("/api/users/:id/role").handler(this::handleUpdateUserRole);
        router.put("/api/users/:id").handler(this::handleUpdateUser);
        router.delete("/api/users/:id").handler(this::handleDeleteUser);
    }

    private void handleGetRoles(RoutingContext ctx) {
        userService.getAllRoles()
            .onSuccess(roles -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(roles)))
            .onFailure(err -> {
                err.printStackTrace();
                JsonObject errorJson = new JsonObject().put("error", "Failed to fetch roles");
                ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(errorJson.encode());
            });
    }

    private void handleGetAllUsers(RoutingContext ctx) {
        userService.getAllUsers()
            .onSuccess(users -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(users)))
            .onFailure(err -> {
                err.printStackTrace();
                JsonObject errorJson = new JsonObject().put("error", "Failed to fetch users");
                ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(errorJson.encode());
            });
    }

    private void handleSearchUsers(RoutingContext ctx) {
        String query = ctx.request().getParam("q");
        if (query == null || query.trim().isEmpty()) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "Search query is required").encode());
            return;
        }

        userService.searchUsers(query.trim())
            .onSuccess(users -> ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(users)))
            .onFailure(err -> {
                err.printStackTrace();
                JsonObject errorJson = new JsonObject().put("error", "Failed to search users");
                ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json")
                    .end(errorJson.encode());
            });
    }

    private void handleGetUserById(RoutingContext ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));
            userService.getUserById(userId)
                .onSuccess(user -> {
                    if (user == null) {
                        ctx.response()
                            .setStatusCode(404)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("error", "User not found").encode());
                    } else {
                        ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(user.encode());
                    }
                })
                .onFailure(err -> {
                    err.printStackTrace();
                    JsonObject errorJson = new JsonObject().put("error", "Failed to fetch user");
                    ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "application/json")
                        .end(errorJson.encode());
                });
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "Invalid user ID").encode());
        }
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
                    err.printStackTrace();
                    String msg = err.getMessage();
                    if ("Username already exists".equals(msg) || "Email already exists".equals(msg) || "Username or email already exists".equals(msg)) {
                        JsonObject errorJson = new JsonObject().put("error", msg);
                        ctx.response()
                            .setStatusCode(409) // Conflict
                            .putHeader("Content-Type", "application/json")
                            .end(errorJson.encode());
                    } else {
                        JsonObject errorJson = new JsonObject().put("error", "Failed to create user: " + msg);
                        ctx.response()
                            .setStatusCode(400)
                            .putHeader("Content-Type", "application/json")
                            .end(errorJson.encode());
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
            JsonObject errorJson = new JsonObject().put("error", "Invalid user data");
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(errorJson.encode());
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
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Role updated successfully").encode()))
                .onFailure(err -> {
                    err.printStackTrace();
                    JsonObject errorJson = new JsonObject().put("error", "Failed to update role: " + err.getMessage());
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end(errorJson.encode());
                });
        } catch (Exception e) {
            JsonObject errorJson = new JsonObject().put("error", "Invalid request data");
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(errorJson.encode());
        }
    }

    private void handleUpdateUser(RoutingContext ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));
            JsonObject json = ctx.getBodyAsJson();

            String username = json.getString("username");
            String email = json.getString("email");
            String password = json.getString("password"); // Optional

            userService.updateUserDetails(userId, username, email, password)
                .onSuccess(res -> ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "User updated successfully").encode()))
                .onFailure(err -> {
                    err.printStackTrace();
                    String msg = err.getMessage();
                    if ("Username or email already exists".equals(msg)) {
                        JsonObject errorJson = new JsonObject().put("error", msg);
                        ctx.response()
                            .setStatusCode(409) // Conflict
                            .putHeader("Content-Type", "application/json")
                            .end(errorJson.encode());
                    } else {
                        JsonObject errorJson = new JsonObject().put("error", "Failed to update user: " + msg);
                        ctx.response()
                            .setStatusCode(400)
                            .putHeader("Content-Type", "application/json")
                            .end(errorJson.encode());
                    }
                });
        } catch (Exception e) {
            JsonObject errorJson = new JsonObject().put("error", "Invalid request data");
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(errorJson.encode());
        }
    }

    private void handleDeleteUser(RoutingContext ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("id"));

            userService.deleteUser(userId)
                .onSuccess(res -> ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "User deleted successfully").encode()))
                .onFailure(err -> {
                    err.printStackTrace();
                    JsonObject errorJson = new JsonObject().put("error", "Failed to delete user: " + err.getMessage());
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("Content-Type", "application/json")
                        .end(errorJson.encode());
                });
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", "Invalid user ID").encode());
        }
    }
}
