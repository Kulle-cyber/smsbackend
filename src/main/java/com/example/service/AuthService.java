package com.example.service;

import org.mindrot.jbcrypt.BCrypt;

import com.example.model.User;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class AuthService {
    private final PgPool client;

    public AuthService(PgPool client) {
        this.client = client;
    }

    public Future<User> getUserByUsername(String username) {
    String query = "SELECT * FROM users WHERE username = $1";
    return client.preparedQuery(query)
            .execute(Tuple.of(username))
            .map(rows -> {
                if (rows.rowCount() == 0) return null;
                Row row = rows.iterator().next();
                return new User(
                        row.getInteger("id"),
                        row.getString("username"),
                        row.getString("password_hash"),
                        row.getInteger("role_id"),
                        row.getString("full_name"),
                        row.getString("email")
                );
            });
}


    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
