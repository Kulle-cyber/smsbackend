package com.example.controller;

import com.example.model.Customer;
import com.example.service.CustomerService;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class CustomerController {

    private final CustomerService customerService;
    private final Router router;

    public CustomerController(Vertx vertx, CustomerService customerService) {
        this.customerService = customerService;
        this.router = Router.router(vertx);

        // Parse JSON bodies
        router.route().handler(BodyHandler.create());

        // Routes (mounted at /api/customers)
        router.post("/").handler(this::handleRegister);
        router.get("/").handler(this::handleGetAll);
        router.put("/:id").handler(this::handleUpdate);
        router.delete("/:id").handler(this::handleDelete);
        router.post("/login").handler(this::handleLogin);
    }

    public Router getRouter() {
        return router;
    }

    // Handle customer registration safely
    private void handleRegister(RoutingContext ctx) {
        var json = ctx.getBodyAsJson();
        Customer c = json.mapTo(Customer.class);
        String plainPassword = json.getString("password");

        // Default portalAccess to false if not provided
        if (c.getPortalAccess() == null) {
            c.setPortalAccess(false);
        }

        // Require password if portalAccess is true
        if (c.getPortalAccess() && (plainPassword == null || plainPassword.isEmpty())) {
            ctx.response().setStatusCode(400).end("Password is required for portal access");
            return;
        }

        customerService.registerCustomer(c, plainPassword)
                .onSuccess(v -> ctx.response().setStatusCode(201).end("Customer registered"))
                .onFailure(err -> {
                    err.printStackTrace(); // Logs exact error
                    ctx.response().setStatusCode(500).end(err.getMessage());
                });
    }

    private void handleGetAll(RoutingContext ctx) {
        customerService.getAllCustomers()
                .onSuccess(customers -> ctx.json(customers))
                .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    private void handleUpdate(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("Invalid customer ID");
            return;
        }

        var json = ctx.getBodyAsJson();
        Customer c = json.mapTo(Customer.class);
        c.setId(id);

        customerService.updateCustomer(c)
                .onSuccess(v -> ctx.response().setStatusCode(200).end("Customer updated"))
                .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    private void handleDelete(RoutingContext ctx) {
        int id;
        try {
            id = Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("Invalid customer ID");
            return;
        }

        customerService.deleteCustomer(id)
                .onSuccess(v -> ctx.response().setStatusCode(204).end())
                .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    private void handleLogin(RoutingContext ctx) {
        var json = ctx.getBodyAsJson();
        String email = json.getString("email");
        String password = json.getString("password");

        customerService.login(email, password)
                .onSuccess(customer -> ctx.json(customer))
                .onFailure(err -> ctx.response().setStatusCode(401).end(err.getMessage()));
    }
}
