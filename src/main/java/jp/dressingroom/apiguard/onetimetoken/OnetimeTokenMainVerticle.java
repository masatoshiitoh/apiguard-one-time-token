package jp.dressingroom.apiguard.onetimetoken;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;


public class OnetimeTokenMainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.deployVerticle("jp.dressingroom.apiguard.onetimetoken.verticle.RedisVerticle", res -> {
      if (res.failed()) {
        startPromise.fail("RedisVerticle start failed: " + res.cause());
      } else {
        vertx.deployVerticle("jp.dressingroom.apiguard.onetimetoken.verticle.HttpReverseProxyVerticle", res2 -> {
          if (res2.failed()) {
            startPromise.fail("HttpReverseProxyVerticle start failed: " + res2.cause());
          } else {
            vertx.deployVerticle("jp.dressingroom.apiguard.onetimetoken.verticle.OnetimeTokenVerticle", res3 -> {
              if (res3.failed()) {
                startPromise.fail("OnetimeTokenVerticle start failed: " + res3.cause());
              }
              else {
                startPromise.complete();
              }
            });
          }
        });
      }
    }
    );
  }
}
