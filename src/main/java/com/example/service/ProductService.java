package com.example.service;

import com.example.model.Product;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;

public class ProductService {
    private final PgPool client;

    public ProductService(PgPool client) {
        this.client = client;
    }

    public Future<Void> addProduct(Product product) {
        String query = "INSERT INTO products (name, description, price, stock, image_url, category_id, salesperson_id) " +
                "VALUES ($1, $2, $3, $4, $5, $6, $7)";
        Tuple params = Tuple.of(product.getName(), product.getDescription(), product.getPrice(),
                product.getStock(), product.getImageUrl(), product.getCategoryId(), product.getSalespersonId());

        Promise<Void> promise = Promise.promise();
        client.preparedQuery(query).execute(params, ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<List<Product>> getAllProducts() {
        String query = "SELECT * FROM products";
        Promise<List<Product>> promise = Promise.promise();

        client.query(query).execute(ar -> {
            if (ar.succeeded()) {
                List<Product> products = new ArrayList<>();
                RowSet<Row> rows = ar.result();
                for (Row row : rows) {
                    products.add(mapRowToProduct(row));
                }
                promise.complete(products);
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<Product> getProductById(int id) {
        String query = "SELECT * FROM products WHERE id = $1";
        Promise<Product> promise = Promise.promise();

        client.preparedQuery(query).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                RowSet<Row> rows = ar.result();
                if (rows.size() > 0) {
                    Product product = mapRowToProduct(rows.iterator().next());
                    promise.complete(product);
                } else {
                    promise.fail("Product not found");
                }
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<Void> updateProduct(int id, Product product) {
        String query = "UPDATE products SET name = $1, description = $2, price = $3, stock = $4, " +
                "image_url = $5, category_id = $6, salesperson_id = $7 WHERE id = $8";
        Tuple params = Tuple.of(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getCategoryId(),
                product.getSalespersonId(),
                id
        );

        Promise<Void> promise = Promise.promise();
        client.preparedQuery(query).execute(params, ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    public Future<Void> deleteProduct(int id) {
        String query = "DELETE FROM products WHERE id = $1";
        Promise<Void> promise = Promise.promise();

        client.preparedQuery(query).execute(Tuple.of(id), ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    private Product mapRowToProduct(Row row) {
        Product product = new Product();
        product.setId(row.getInteger("id"));
        product.setName(row.getString("name"));
        product.setDescription(row.getString("description"));
        product.setPrice(row.getDouble("price"));
        product.setStock(row.getInteger("stock"));
        product.setImageUrl(row.getString("image_url"));
        product.setCategoryId(row.getInteger("category_id"));
        product.setSalespersonId(row.getInteger("salesperson_id"));
        return product;
    }
}
