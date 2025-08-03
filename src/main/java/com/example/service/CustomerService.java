package com.example.service;

import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.example.model.Customer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class CustomerService {

    private final SqlClient client;

    public CustomerService(SqlClient client) {
        this.client = client;
    }

    public Future<Void> registerCustomer(Customer customer, String plainPassword) {
        Promise<Void> promise = Promise.promise();

        String bcryptHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));

        String sql = "INSERT INTO customers (name, email, phone, address, password, portal_access) VALUES ($1, $2, $3, $4, $5, $6)";

        client.preparedQuery(sql).execute(
            Tuple.of(customer.getName(), customer.getEmail(), customer.getPhone(),
                   customer.getAddress(), bcryptHash, customer.getPortalAccess()),
            ar -> {
                if (ar.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    public Future<List<Customer>> getAllCustomers() {
        Promise<List<Customer>> promise = Promise.promise();

        client.query("SELECT id, name, email, phone, address, portal_access FROM customers").execute(ar -> {
            if (ar.succeeded()) {
                List<Customer> list = new ArrayList<>();
                for (Row row : ar.result()) {
                    Customer c = new Customer();
                    c.setId(row.getInteger("id"));
                    c.setName(row.getString("name"));
                    c.setEmail(row.getString("email"));
                    c.setPhone(row.getString("phone"));
                    c.setAddress(row.getString("address"));
                    c.setPortalAccess(row.getBoolean("portal_access"));
                    list.add(c);
                }
                promise.complete(list);
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<Void> updateCustomer(Customer customer) {
        Promise<Void> promise = Promise.promise();

        String sql = "UPDATE customers SET name = $1, email = $2, phone = $3, " +
                     "address = $4, portal_access = $5 WHERE id = $6";

        client.preparedQuery(sql).execute(
            Tuple.of(customer.getName(), customer.getEmail(), customer.getPhone(),
                   customer.getAddress(), customer.getPortalAccess(), customer.getId()),
            ar -> {
                if (ar.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(ar.cause());
                }
            });

        return promise.future();
    }

    public Future<Void> deleteCustomer(int id) {
        Promise<Void> promise = Promise.promise();

        client.preparedQuery("DELETE FROM customers WHERE id = $1").execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<Customer> login(String email, String password) {
        Promise<Customer> promise = Promise.promise();

        String sql = "SELECT * FROM customers WHERE email = $1 AND portal_access = TRUE";

        client.preparedQuery(sql).execute(Tuple.of(email), ar -> {
            if (ar.succeeded()) {
                if (ar.result().size() == 0) {
                    promise.fail("User not found or no portal access");
                    return;
                }
                Row row = ar.result().iterator().next();
                String storedHash = row.getString("password");

                if (BCrypt.checkpw(password, storedHash)) {
                    Customer c = new Customer();
                    c.setId(row.getInteger("id"));
                    c.setName(row.getString("name"));
                    c.setEmail(row.getString("email"));
                    c.setPhone(row.getString("phone"));
                    c.setAddress(row.getString("address"));
                    c.setPortalAccess(row.getBoolean("portal_access"));
                    promise.complete(c);
                } else {
                    promise.fail("Invalid password");
                }
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }
}
