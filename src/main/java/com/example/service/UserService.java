// src/main/java/com/example/service/UserService.java
package com.example.service;

import java.util.ArrayList;
import java.util.List;

import com.example.model.Role;
import com.example.model.User;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class UserService {

    private final PgPool client;

    public UserService(PgPool client) {
        this.client = client;
    }

    // Fetch all roles
    public Future<List<Role>> getAllRoles() {
        Promise<List<Role>> promise = Promise.promise();

        client.query("SELECT id, name FROM roles ORDER BY id")
            .execute(ar -> {
                if (ar.succeeded()) {
                    RowSet<Row> rows = ar.result();
                    List<Role> roles = new ArrayList<>();
                    for (Row row : rows) {
                        roles.add(new Role(row.getInteger("id"), row.getString("name")));
                    }
                    promise.complete(roles);
                } else {
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    // Create user with role
    public Future<Void> createUser(User user) {
        Promise<Void> promise = Promise.promise();

        String sql = "INSERT INTO users (username, password_hash, role_id, full_name, email) VALUES ($1, $2, $3, $4, $5)";

        client.preparedQuery(sql)
            .execute(Tuple.of(user.getUsername(), user.getPasswordHash(), user.getRoleId(), user.getFullName(), user.getEmail()), ar -> {
                if (ar.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    // Update user's role
    public Future<Void> updateUserRole(int userId, int roleId) {
        Promise<Void> promise = Promise.promise();

        String sql = "UPDATE users SET role_id = $1 WHERE id = $2";

        client.preparedQuery(sql)
            .execute(Tuple.of(roleId, userId), ar -> {
                if (ar.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    // Get user by id (including role name)
    public Future<JsonObject> getUserById(int id) {
        Promise<JsonObject> promise = Promise.promise();

        String sql = "SELECT u.id, u.username, u.full_name, u.email, r.id AS role_id, r.name AS role_name " +
                "FROM users u LEFT JOIN roles r ON u.role_id = r.id WHERE u.id = $1";

        client.preparedQuery(sql).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                if (!rows.iterator().hasNext()) {
                    promise.complete(null);
                } else {
                    Row row = rows.iterator().next();
                    JsonObject userJson = new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("username", row.getString("username"))
                        .put("fullName", row.getString("full_name"))
                        .put("email", row.getString("email"))
                        .put("role", new JsonObject()
                            .put("id", row.getInteger("role_id"))
                            .put("name", row.getString("role_name")));
                    promise.complete(userJson);
                }
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    // List all users with roles
    public Future<List<JsonObject>> getAllUsers() {
        Promise<List<JsonObject>> promise = Promise.promise();

        String sql = "SELECT u.id, u.username, u.full_name, u.email, r.id AS role_id, r.name AS role_name " +
                "FROM users u LEFT JOIN roles r ON u.role_id = r.id ORDER BY u.id";

        client.query(sql).execute(ar -> {
            if (ar.succeeded()) {
                List<JsonObject> users = new ArrayList<>();
                for (Row row : ar.result()) {
                    JsonObject userJson = new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("username", row.getString("username"))
                        .put("fullName", row.getString("full_name"))
                        .put("email", row.getString("email"))
                        .put("role", new JsonObject()
                            .put("id", row.getInteger("role_id"))
                            .put("name", row.getString("role_name")));
                    users.add(userJson);
                }
                promise.complete(users);
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }
}
