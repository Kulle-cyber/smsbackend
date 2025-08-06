package com.example.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ProductService {
    private final PgPool client;

    public ProductService(PgPool client) {
        this.client = client;
    }

    // GET ALL PRODUCTS
    public void getAll(RoutingContext ctx) {
        client.query("SELECT * FROM products")
            .execute()
            .onSuccess(rows -> {
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(JsonArrayFromRows(rows).encode());
            })
            .onFailure(err -> {
                ctx.response().setStatusCode(500).end(err.getMessage());
            });
    }

    // CREATE PRODUCT
    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String name = body.getString("name");
        String description = body.getString("description");
        Double price = body.getDouble("price");
        Integer stock = body.getInteger("stock");
        String imageUrl = body.getString("image_url");

        // Get the logged-in salesperson ID from context
        Integer salespersonId = ctx.get("userId"); // This must be set by JWT handler

        if (salespersonId == null) {
            ctx.response().setStatusCode(401).end("Unauthorized: Salesperson ID missing");
            return;
        }

        String query = "INSERT INTO products (name, description, price, stock, image_url, salesperson_id) " +
                       "VALUES ($1, $2, $3, $4, $5, $6) RETURNING id";

        client.preparedQuery(query)
            .execute(Tuple.of(name, description, price, stock, imageUrl, salespersonId))
            .onSuccess(rows -> {
                int id = rows.iterator().next().getInteger("id");
                body.put("id", id);
                body.put("salesperson_id", salespersonId);

                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(201)
                    .end(body.encode());
            })
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    // GET PRODUCT BY ID
    public void getById(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        String query = "SELECT * FROM products WHERE id = $1";

        client.preparedQuery(query)
            .execute(Tuple.of(id))
            .onSuccess(rows -> {
                if (!rows.iterator().hasNext()) {
                    ctx.response().setStatusCode(404).end("Product not found");
                    return;
                }
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(rows.iterator().next().toJson().encode());
            })
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    // UPDATE PRODUCT
    public void update(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();

        String query = "UPDATE products SET name=$1, description=$2, price=$3, stock=$4, image_url=$5 WHERE id=$6";

        client.preparedQuery(query)
            .execute(Tuple.of(
                body.getString("name"),
                body.getString("description"),
                body.getDouble("price"),
                body.getInteger("stock"),
                body.getString("image_url"),
                id
            ))
            .onSuccess(rows -> {
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(body.put("id", id).encode());
            })
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    // DELETE PRODUCT
    public void delete(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        String query = "DELETE FROM products WHERE id = $1";

        client.preparedQuery(query)
            .execute(Tuple.of(id))
            .onSuccess(rows -> {
                ctx.response().setStatusCode(204).end();
            })
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    // Helper: Convert RowSet to JsonArray
    private JsonArray JsonArrayFromRows(RowSet<Row> rows) {
        JsonArray array = new JsonArray();
        for (Row row : rows) {
            array.add(row.toJson());
        }
        return array;
    }
}
