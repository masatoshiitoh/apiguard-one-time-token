package jp.dressingroom.apiguard.onetimetoken;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import jp.dressingroom.apiguard.httpresponder.HttpResponderMainVerticle;
import jp.dressingroom.apiguard.onetimetoken.verticle.RedisSetExRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import redis.embedded.RedisServer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestOnetimeTokenMainVerticle {

  RedisServer redisServer;

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) throws Throwable {

    redisServer = new RedisServer(6379);
    redisServer.start();

    System.setProperty("server.port", "18888"); // http-responder listening port.
    System.setProperty("onetimetoken.server.port", "18891");
    System.setProperty("onetimetoken.proxy.port", "18888");

    System.setProperty("onetimetoken.redis.hostname", "localhost");
    System.setProperty("onetimetoken.redis.port", "6379");

    vertx.deployVerticle(new HttpResponderMainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    vertx.deployVerticle(new OnetimeTokenMainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @AfterEach
  void cleanup() {
    // do some work
    redisServer.stop();
  }

  @Test
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void checkHttpResponderDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18888, "localhost", "/hello")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.body().equals("Hello"));
        assertTrue(response.headers().contains("httpresponder"));
        assertTrue(response.headers().get("httpresponder").equals("true"));
        testContext.completeNow();
      })));
  }

  @Test
  void onetimeTokenHttpReverseProxyDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18891, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 200);
        assertTrue(response.headers().contains("httpresponder"));
        assertTrue(response.headers().get("httpresponder").equals("true"));
        testContext.completeNow();
      })));
  }

  @Test
  void redisServerDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {

    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig(json -> {
      try {
        JsonObject result = json.result();

        String redisHost;
        Integer redisPort;
        RedisOptions redisOptions = new RedisOptions();
        redisHost = result.getString(ConfigKeyNames.ONETIME_TOKEN_REDIS_HOSTNAME.value(), "localhost");
        redisPort = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_REDIS_PORT.value(), 6379);
        redisOptions.addEndpoint(SocketAddress.inetSocketAddress(redisPort, redisHost));
        Redis.createClient(vertx, redisOptions)
          .connect(onConnect -> {
            Redis redisClient;
            if (onConnect.succeeded()) {
              redisClient = onConnect.result();
              RedisAPI redis = RedisAPI.api(redisClient);
              redis.get( // get REPLIES VALUE
                "keyfortest", // KEY
                getResult -> {
                  if (getResult.succeeded()) {
                    redis.setex( // setex REPLIES RESULT (OK|NG)
                      "keyfortest", // KEY
                      "20", // Expire seconds
                      "Hello", // VALUE
                      setexResult -> {
                        if (setexResult.succeeded()) {
                          redis.get( // check after setex
                            "keyfortest",
                            get2Result -> {
                              if (get2Result.succeeded()) {
                                assertTrue(get2Result.result().toString().equals("Hello"), "result mismatch");
                                testContext.completeNow();
                              } else {
                                assertTrue(false, "get2result returns fail.");
                                testContext.completeNow();
                              }
                            });
                          } else {
                          assertTrue(false, "setExResult returns fail.");
                          testContext.completeNow();
                        }
                      });
                  } else {
                    assertTrue(false, "getResult returns fail.");
                    testContext.completeNow();
                  }
                });
            } else {
              assertTrue(false, "redisConnect returns fail.");
              testContext.completeNow();
            }
          });
      } catch (Exception e) {
        testContext.failNow(e);
      }
    });
  }
}
