package com.example;

import java.util.Set;

import com.example.config.DatabaseConfig;
import com.example.controller.AuthController;
import com.example.controller.CustomerController;
import com.example.controller.ProductController;
import com.example.controller.RoleController;
import com.example.controller.UserController;
import com.example.service.AuthService;
import com.example.service.CustomerService;
import com.example.service.ProductService;
import com.example.service.RoleService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    // Initialize database connection
    PgPool client = DatabaseConfig.getPgClient(vertx);

    // Main router
    Router router = Router.router(vertx);

    // Global CORS handler
    router.route().handler(
      CorsHandler.create("*")
        .allowedMethods(Set.of(
          HttpMethod.GET,
          HttpMethod.POST,
          HttpMethod.PUT,
          HttpMethod.DELETE,
          HttpMethod.OPTIONS
        ))
        .allowedHeaders(Set.of(
          "Content-Type",
          "Authorization",
          "Access-Control-Allow-Credentials"
        ))
        .allowCredentials(true)
    );

    // Pre-flight OPTIONS request handler
    router.options().handler(ctx -> ctx.response().setStatusCode(204).end());

    // Initialize services
    AuthService authService = new AuthService(client);
    RoleService roleService = new RoleService();
    CustomerService customerService = new CustomerService(client);
    ProductService productService = new ProductService(client);

    // Auth routes mounted at /api/auth
    new AuthController(vertx, router, authService, roleService);

    // Mount customer routes at /api/customers
    CustomerController customerController = new CustomerController(vertx, customerService);
    router.mountSubRouter("/api/customers", customerController.getRouter());

    // Mount role routes at /api/roles
    new RoleController(vertx, router);

    // Mount user routes at /api/users
    UserController userController = new UserController(client);
    userController.mountRoutes(router); // Make sure UserController mounts on distinct paths

    // Mount product routes at /api/products
    ProductController productController = new ProductController(vertx, productService);
    router.mountSubRouter("/api/products", productController.getRouter());

    // Start HTTP server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8889)
      .onSuccess(server -> {
        System.out.println("✅ Server running at http://localhost:8889");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }
}
