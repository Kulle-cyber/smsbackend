package com.example.controller;

import com.example.middleware.JwtAuthHandler;
import com.example.service.ProductService;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class ProductController {
    private final Router router;

    public ProductController(Vertx vertx, ProductService productService) {
        this.router = Router.router(vertx);

        // BodyHandler for POST/PUT to parse JSON body
        router.route().handler(io.vertx.ext.web.handler.BodyHandler.create());

        // Protect all /products routes with JWT middleware
        router.route().handler(JwtAuthHandler::handle);

        // Define routes relative to /products base path
        router.get("/").handler(productService::getAll);
        router.post("/").handler(productService::create);
        router.get("/:id").handler(productService::getById);
        router.put("/:id").handler(productService::update);
        router.delete("/:id").handler(productService::delete);
    }

    public Router getRouter() {
        return router;
    }
}
