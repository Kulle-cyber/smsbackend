package com.example.service;

import org.mindrot.jbcrypt.BCrypt;

import com.example.model.Customer;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class CustomerService {

    private final PgPool client;

    public CustomerService(PgPool client) {
        this.client = client;
    }

    // Register a new customer with hashed password
    public Future<Void> registerCustomer(Customer customer, String plainPassword) {
        Promise<Void> promise = Promise.promise();

        // Hash password only if provided
        String hashedPassword = null;
        if (plainPassword != null && !plainPassword.isEmpty()) {
            hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        } else {
            hashedPassword = ""; // store empty string if no password
        }

        String query = "INSERT INTO customers (name, email, phone, address, portal_access, password) VALUES ($1, $2, $3, $4, $5, $6)";
        client.preparedQuery(query)
              .execute(Tuple.of(
                      customer.getName(),
                      customer.getEmail(),
                      customer.getPhone(),
                      customer.getAddress(),
                      customer.getPortalAccess() != null ? customer.getPortalAccess() : false,
                      hashedPassword
              ))
              .onSuccess(res -> promise.complete())
              .onFailure(promise::fail);

        return promise.future();
    }

    // Get all customers (exclude password)
    public Future<JsonArray> getAllCustomers() {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT id, name, email, phone, address, portal_access FROM customers";

        client.query(query)
              .execute()
              .onSuccess(rows -> {
                  JsonArray customers = new JsonArray();
                  for (Row row : rows) {
                      JsonObject json = new JsonObject()
                              .put("id", row.getInteger("id"))
                              .put("name", row.getString("name"))
                              .put("email", row.getString("email"))
                              .put("phone", row.getString("phone"))
                              .put("address", row.getString("address"))
                              .put("portalAccess", row.getBoolean("portal_access"));
                      customers.add(json);
                  }
                  promise.complete(customers);
              })
              .onFailure(promise::fail);

        return promise.future();
    }

    // Update customer details
    public Future<Void> updateCustomer(Customer customer) {
        Promise<Void> promise = Promise.promise();

        String query = "UPDATE customers SET name=$1, email=$2, phone=$3, address=$4, portal_access=$5 WHERE id=$6";
        client.preparedQuery(query)
              .execute(Tuple.of(
                      customer.getName(),
                      customer.getEmail(),
                      customer.getPhone(),
                      customer.getAddress(),
                      customer.getPortalAccess() != null ? customer.getPortalAccess() : false,
                      customer.getId()
              ))
              .onSuccess(res -> promise.complete())
              .onFailure(promise::fail);

        return promise.future();
    }

    // Delete a customer by ID
    public Future<Void> deleteCustomer(int id) {
        Promise<Void> promise = Promise.promise();
        String query = "DELETE FROM customers WHERE id=$1";

        client.preparedQuery(query)
              .execute(Tuple.of(id))
              .onSuccess(res -> promise.complete())
              .onFailure(promise::fail);

        return promise.future();
    }

    // Customer login
    public Future<JsonObject> login(String email, String plainPassword) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "SELECT * FROM customers WHERE email=$1";
        client.preparedQuery(query)
              .execute(Tuple.of(email))
              .onSuccess(rows -> {
                  if (!rows.iterator().hasNext()) {
                      promise.fail("Invalid email or password");
                      return;
                  }

                  Row row = rows.iterator().next();
                  String hashedPassword = row.getString("password");

                  if (!BCrypt.checkpw(plainPassword, hashedPassword)) {
                      promise.fail("Invalid email or password");
                      return;
                  }

                  JsonObject customer = new JsonObject()
                          .put("id", row.getInteger("id"))
                          .put("name", row.getString("name"))
                          .put("email", row.getString("email"))
                          .put("phone", row.getString("phone"))
                          .put("address", row.getString("address"))
                          .put("portalAccess", row.getBoolean("portal_access"));

                  promise.complete(customer);
              })
              .onFailure(promise::fail);

        return promise.future();
    }
}
