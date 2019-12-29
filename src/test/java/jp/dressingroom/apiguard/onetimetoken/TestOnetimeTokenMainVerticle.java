package jp.dressingroom.apiguard.onetimetoken;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestOnetimeTokenMainVerticle {

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {

    System.setProperty("server.port","18888");
    System.setProperty("onetimetoken.server.port","18889");
    System.setProperty("onetimetoken.proxy.port","18888");

    vertx.deployVerticle(new OnetimeTokenMainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }
}
