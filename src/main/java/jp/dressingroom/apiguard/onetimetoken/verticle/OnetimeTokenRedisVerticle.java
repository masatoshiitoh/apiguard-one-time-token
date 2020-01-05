package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import jp.dressingroom.apiguard.onetimetoken.ConfigKeyNames;

import java.util.UUID;

/**
 * Receive Onetime token handling request.  store and query token to Redis server.
 */

public class OnetimeTokenRedisVerticle extends AbstractVerticle {
  EventBus eventBus;
  Redis redisClient;
  String redisHost;
  int redisPort;
  int expireSeconds;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    eventBus = vertx.eventBus();
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_INIT.value(), oneTimeTokenResetHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_VERIFY.value(), oneTimeTokenVerifyHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_UPDATE.value(), oneTimeTokenUpdateHandler());

        JsonObject result = json.result();
        redisHost = result.getString(ConfigKeyNames.ONETIME_TOKEN_REDIS_HOSTNAME.value(), "localhost");
        redisPort = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_REDIS_PORT.value(), 6379);
        expireSeconds = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_TOKEN_EXPIRE_SECONDS.value(), 300);

        RedisOptions redisOptions = new RedisOptions()
          .setEndpoint(SocketAddress.inetSocketAddress(redisPort, redisHost));

        Redis.createClient(vertx, redisOptions)
          .connect(onConnect -> {
            if (onConnect.succeeded()) {
              redisClient = onConnect.result();
              startPromise.complete();
            } else {
              startPromise.fail(this.getClass().getName() + ": redis connection failed");
            }
          });
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));
  }

  /*
   * Input: String via Message: userId
   * Process: generate new UUID and set it for userId
   * Output: generated UUID
   */
  private Handler<Message<String>> oneTimeTokenResetHandler() {
    return messageHandler -> {
      String userId = messageHandler.body();
      UUID generatedUuid = UUID.randomUUID();

      RedisAPI redis = RedisAPI.api(redisClient);
      // setex REPLIES only RESULT (OK|NG)
      redis.setex(
        userId, // KEY
        String.valueOf(expireSeconds), // Expire seconds
        generatedUuid.toString(), // VALUE
        res -> {
          if (res.succeeded() && res.result().toString().equals("OK")) {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenResetHandler: initialized "+ userId +" token to:" + generatedUuid.toString());
            messageHandler.reply(generatedUuid.toString());
          } else {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenUpdateHandler: initialize "+ userId +"  failed");
            messageHandler.fail(1, "oneTimeTokenResetHandler: reset token failed.");
          }
        });
    };
  }

  private Handler<Message<JsonObject>> oneTimeTokenVerifyHandler() {
    return messageHandler -> {
      JsonObject verifyRequestContent = messageHandler.body();
      String userId = verifyRequestContent.getString("user");
      String token = verifyRequestContent.getString("token");

      RedisAPI redis = RedisAPI.api(redisClient);
      redis.get(userId, ar -> {
        if (ar.succeeded()) {
          if (ar.result().toString().equals(token)) {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenVerifyHandler: verified  " + userId + " token");
            messageHandler.reply(Boolean.TRUE);
          } else {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenVerifyHandler: token mismatch  " + userId + " token");
            messageHandler.reply(Boolean.FALSE);
          }
        } else {
          System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenVerifyHandler: verify "+ userId +"  failed");
          messageHandler.fail(1, "oneTimeTokenResetHandler: reset token failed.");
        }
      });
    };
  }

  private Handler<Message<String>> oneTimeTokenUpdateHandler() {
    return messageHandler -> {
      String userId = messageHandler.body();
      UUID generatedUuid = UUID.randomUUID();

      RedisAPI redis = RedisAPI.api(redisClient);
      // setex REPLIES only RESULT (OK|NG)
      redis.setex(
        userId, // KEY
        String.valueOf(expireSeconds), // Expire seconds
        generatedUuid.toString(), // VALUE
        res -> {
          if (res.succeeded() && res.result().toString().equals("OK")) {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenUpdateHandler: updated "+ userId +" token to:" + generatedUuid.toString());
            messageHandler.reply(generatedUuid.toString());
          } else {
            System.out.println("OnetimeTokenRedisVerticle.oneTimeTokenUpdateHandler: update "+ userId +"  failed");
            messageHandler.fail(1, "oneTimeTokenUpdateHandler: reset token failed.");
          }
        });
    };
  }
}
