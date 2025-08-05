package com.example.controller;

import com.example.model.Product;
import com.example.service.ProductService;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProductController {
    private final ProductService productService;
    private final Router router;

    // Pass Vertx here to create a sub-router
    public ProductController(Vertx vertx, ProductService productService) {
        this.productService = productService;
        this.router = Router.router(vertx);
        mountRoutes(this.router);
    }

    // Return sub-router to be mounted in MainVerticle
    public Router getRouter() {
        return router;
    }

    public void mountRoutes(Router router) {
        router.post("/products").handler(this::addProduct);
        router.get("/products").handler(this::getAllProducts);
        router.get("/products/:id").handler(this::getProductById);
        router.put("/products/:id").handler(this::updateProduct);
        router.delete("/products/:id").handler(this::deleteProduct);
    }

    // Handler methods (same as before)
    public void addProduct(RoutingContext ctx) {
        JsonObject body = ctx.getBodyAsJson();
        Product product = body.mapTo(Product.class);

        productService.addProduct(product).onSuccess(res -> {
            ctx.response()
               .setStatusCode(201)
               .putHeader("Content-Type", "application/json")
               .end(Json.encodePrettily("Product added"));
        }).onFailure(err -> {
            ctx.response()
               .setStatusCode(500)
               .end(err.getMessage());
        });
    }

    public void getAllProducts(RoutingContext ctx) {
        productService.getAllProducts().onSuccess(products -> {
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(Json.encodePrettily(products));
        }).onFailure(err -> {
            ctx.response()
               .setStatusCode(500)
               .end(err.getMessage());
        });
    }

    public void getProductById(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        productService.getProductById(id).onSuccess(product -> {
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(Json.encodePrettily(product));
        }).onFailure(err -> {
            ctx.response()
               .setStatusCode(404)
               .end(err.getMessage());
        });
    }

    public void updateProduct(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.getBodyAsJson();
        Product product = body.mapTo(Product.class);

        productService.updateProduct(id, product).onSuccess(res -> {
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(Json.encodePrettily("Product updated"));
        }).onFailure(err -> {
            ctx.response()
               .setStatusCode(500)
               .end(err.getMessage());
        });
    }

    public void deleteProduct(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        productService.deleteProduct(id).onSuccess(res -> {
            ctx.response()
               .setStatusCode(204)
               .end();
        }).onFailure(err -> {
            ctx.response()
               .setStatusCode(500)
               .end(err.getMessage());
        });
    }
}
