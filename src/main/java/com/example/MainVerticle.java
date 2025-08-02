package com.example;

import java.util.Set;

import com.example.config.DatabaseConfig;
import com.example.controller.AuthController;
import com.example.controller.RoleController;
import com.example.controller.UserController;
import com.example.service.AuthService;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    // Initialize database client
    PgPool client = DatabaseConfig.getPgClient(vertx);

    // Create main router
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
    router.options().handler(ctx -> {
      ctx.response().setStatusCode(204).end();
    });

    // Mount auth routes
    AuthService authService = new AuthService(client);
    new AuthController(vertx, router, authService);

    // Mount role routes
    new RoleController(vertx, router);

    // Mount user routes properly
    UserController userController = new UserController(client);
    userController.mountRoutes(router); // ✅ required to attach routes

    // Start HTTP server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8889)
      .onSuccess(server -> {
        System.out.println("✅ Server running at http://localhost:8889");
        startPromise.complete();
      })
      .onFailure(err -> {
        err.printStackTrace();
        startPromise.fail(err);
      });
  }
}
