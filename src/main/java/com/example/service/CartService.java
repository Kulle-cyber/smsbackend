package com.example.service;

import java.util.ArrayList;
import java.util.List;

import com.example.model.Cart;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class CartService {

    private final PgPool client;

    public CartService(PgPool client) {
        this.client = client;
    }

    // Add item to cart
    public Future<Void> addCartItem(Cart cart) {
        Promise<Void> promise = Promise.promise();

        // First check if item already exists in cart
        String checkQuery = "SELECT id, quantity FROM cart WHERE customer_id = $1 AND product_id = $2";

        client.preparedQuery(checkQuery)
            .execute(Tuple.of(cart.getCustomerId(), cart.getProductId()))
            .compose(rows -> {
                if (rows.size() > 0) {
                    // Item exists, update quantity
                    Row row = rows.iterator().next();
                    Integer existingId = row.getInteger("id");
                    Integer existingQuantity = row.getInteger("quantity");
                    Integer newQuantity = existingQuantity + (cart.getQuantity() != null ? cart.getQuantity() : 1);

                    String updateQuery = "UPDATE cart SET quantity = $1 WHERE id = $2";
                    return client.preparedQuery(updateQuery)
                        .execute(Tuple.of(newQuantity, existingId))
                        .mapEmpty();
                } else {
                    // Item doesn't exist, insert new
                    String insertQuery = "INSERT INTO cart (customer_id, product_id, quantity, name, price, image_url) " +
                                       "VALUES ($1, $2, $3, $4, $5, $6)";
                    return client.preparedQuery(insertQuery)
                        .execute(Tuple.of(
                            cart.getCustomerId(),
                            cart.getProductId(),
                            cart.getQuantity() != null ? cart.getQuantity() : 1,
                            cart.getName(),
                            cart.getPrice(),
                            cart.getImageUrl()
                        ))
                        .mapEmpty();
                }
            })
            .onSuccess(res -> promise.complete())
            .onFailure(promise::fail);

        return promise.future();
    }

    // Get cart by customer with product details
    public Future<List<Cart>> getCartByCustomerId(Integer customerId) {
        Promise<List<Cart>> promise = Promise.promise();

        // Join with products table to get latest product info
        String query = "SELECT c.*, p.name as product_name, p.price as product_price, p.image_url as product_image_url " +
                      "FROM cart c " +
                      "LEFT JOIN products p ON c.product_id = p.id " +
                      "WHERE c.customer_id = $1 " +
                      "ORDER BY c.id";

        client.preparedQuery(query)
            .execute(Tuple.of(customerId))
            .onSuccess(rows -> {
                List<Cart> list = new ArrayList<>();
                for (Row row : rows) {
                    Cart cart = new Cart();
                    cart.setId(row.getInteger("id"));
                    cart.setCustomerId(row.getInteger("customer_id"));
                    cart.setProductId(row.getInteger("product_id"));
                    cart.setQuantity(row.getInteger("quantity"));

                    // Use product details from products table if available
                    // Fall back to cart stored values if not
                    String productName = row.getString("product_name");
                    if (productName != null) {
                        cart.setName(productName);
                    } else {
                        cart.setName(row.getString("name"));
                    }

                    Double productPrice = row.getDouble("product_price");
                    if (productPrice != null) {
                        cart.setPrice(productPrice);
                    } else {
                        cart.setPrice(row.getDouble("price"));
                    }

                    String productImageUrl = row.getString("product_image_url");
                    if (productImageUrl != null) {
                        cart.setImageUrl(productImageUrl);
                    } else {
                        cart.setImageUrl(row.getString("image_url"));
                    }

                    list.add(cart);
                }
                promise.complete(list);
            })
            .onFailure(promise::fail);

        return promise.future();
    }

    // Update cart quantity
    public Future<Void> updateCartItem(Integer id, Integer quantity) {
        Promise<Void> promise = Promise.promise();

        if (quantity <= 0) {
            // If quantity is 0 or negative, delete the item
            return deleteCartItem(id);
        }

        String query = "UPDATE cart SET quantity = $1 WHERE id = $2";
        client.preparedQuery(query)
            .execute(Tuple.of(quantity, id))
            .onSuccess(res -> promise.complete())
            .onFailure(promise::fail);

        return promise.future();
    }

    // Delete cart item
    public Future<Void> deleteCartItem(Integer id) {
        Promise<Void> promise = Promise.promise();
        String query = "DELETE FROM cart WHERE id = $1";
        client.preparedQuery(query)
            .execute(Tuple.of(id))
            .onSuccess(res -> promise.complete())
            .onFailure(promise::fail);
        return promise.future();
    }
}
