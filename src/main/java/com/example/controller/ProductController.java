// ProductController.java
package com.example.controller;

import com.example.middleware.JwtAuthHandler;
import com.example.service.ProductService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProductController {
    private final Router router;

    public ProductController(Vertx vertx, ProductService productService) {
        this.router = Router.router(vertx);

        // Protect all /products routes with JWT middleware
        router.route("/products*").handler(JwtAuthHandler::handle);

        // Actual routes
        router.get("/products").handler(ctx -> productService.getAll(ctx));
        router.post("/products").handler(ctx -> productService.create(ctx));
        router.get("/products/:id").handler(ctx -> productService.getById(ctx));
        router.put("/products/:id").handler(ctx -> productService.update(ctx));
        router.delete("/products/:id").handler(ctx -> productService.delete(ctx));
    }

    public Router getRouter() {
        return router;
    }
}
