package com.example.controller;

import com.example.model.Cart;
import com.example.service.CartService;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class CartController {

    private final CartService cartService;

    public CartController(Vertx vertx, Router router, CartService cartService) {
        this.cartService = cartService;

        router.route().handler(BodyHandler.create());

        router.post("/api/cart").handler(this::addToCart);
        router.get("/api/cart/:customerId").handler(this::getCart);
        router.put("/api/cart/:id").handler(this::updateCart);
        router.delete("/api/cart/:id").handler(this::deleteCart);
    }

    private void addToCart(RoutingContext ctx) {
        try {
            JsonObject json = ctx.getBodyAsJson();
            System.out.println("Received cart data: " + json.encodePrettily());

            Cart cart = json.mapTo(Cart.class);

            // Validate required fields
            if (cart.getCustomerId() == null || cart.getProductId() == null) {
                ctx.response()
                    .setStatusCode(400)
                    .end("Missing required fields: customerId and productId are required");
                return;
            }

            cartService.addCartItem(cart)
                .onSuccess(v -> ctx.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Added to cart").encode()))
                .onFailure(err -> {
                    System.err.println("Failed to add item: " + err.getMessage());
                    err.printStackTrace();
                    ctx.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                            .put("error", "Failed to add item to cart")
                            .put("details", err.getMessage())
                            .encode());
                });
        } catch (Exception e) {
            System.err.println("Error parsing cart: " + e.getMessage());
            e.printStackTrace();
            ctx.response()
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", "Invalid cart data")
                    .put("details", e.getMessage())
                    .encode());
        }
    }

    private void getCart(RoutingContext ctx) {
        try {
            Integer customerId = Integer.valueOf(ctx.pathParam("customerId"));

            cartService.getCartByCustomerId(customerId)
                .onSuccess(list -> ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(io.vertx.core.json.Json.encodePrettily(list)))
                .onFailure(err -> {
                    System.err.println("Failed to fetch cart: " + err.getMessage());
                    err.printStackTrace();
                    ctx.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                            .put("error", "Failed to fetch cart")
                            .put("details", err.getMessage())
                            .encode());
                });
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", "Invalid customer ID format")
                    .encode());
        }
    }

    private void updateCart(RoutingContext ctx) {
        try {
            Integer id = Integer.valueOf(ctx.pathParam("id"));
            JsonObject json = ctx.getBodyAsJson();

            if (json == null || !json.containsKey("quantity")) {
                ctx.response()
                    .setStatusCode(400)
                    .end(new JsonObject()
                        .put("error", "Quantity field is required")
                        .encode());
                return;
            }

            Integer quantity = json.getInteger("quantity");

            cartService.updateCartItem(id, quantity)
                .onSuccess(v -> ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Cart updated").encode()))
                .onFailure(err -> {
                    System.err.println("Failed to update cart: " + err.getMessage());
                    err.printStackTrace();
                    ctx.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                            .put("error", "Failed to update cart")
                            .put("details", err.getMessage())
                            .encode());
                });
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", "Invalid ID format")
                    .encode());
        }
    }

    private void deleteCart(RoutingContext ctx) {
        try {
            Integer id = Integer.valueOf(ctx.pathParam("id"));

            cartService.deleteCartItem(id)
                .onSuccess(v -> ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("message", "Cart item deleted").encode()))
                .onFailure(err -> {
                    System.err.println("Failed to delete cart item: " + err.getMessage());
                    err.printStackTrace();
                    ctx.response()
                        .setStatusCode(500)
                        .end(new JsonObject()
                            .put("error", "Failed to delete cart item")
                            .put("details", err.getMessage())
                            .encode());
                });
        } catch (NumberFormatException e) {
            ctx.response()
                .setStatusCode(400)
                .end(new JsonObject()
                    .put("error", "Invalid ID format")
                    .encode());
        }
    }
}
