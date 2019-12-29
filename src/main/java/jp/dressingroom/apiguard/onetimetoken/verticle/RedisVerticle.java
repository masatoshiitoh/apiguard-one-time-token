package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import jp.dressingroom.apiguard.onetimetoken.ConfigKeyNames;

public class RedisVerticle extends AbstractVerticle {
  Redis redisClient;

  String redisHost;
  int redisPort;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {


    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        JsonObject result = json.result();

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(ApiguardEventBusNames.REDIS_GET.value(), redisGetHandler());
        eventBus.consumer(ApiguardEventBusNames.REDIS_SETEX.value(), redisSetExHandler());

        RedisOptions redisOptions = new RedisOptions();

        redisHost = result.getString(ConfigKeyNames.ONETIME_TOKEN_REDIS_HOSTNAME.value(), "localhost");
        redisPort = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_REDIS_PORT.value(), 6379);

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

  // get handler
  private Handler<Message<RedisGetRequest>> redisGetHandler() {
    return messageHandler -> {
      RedisAPI redis = RedisAPI.api(redisClient);
      redis.get( // REPLIES VALUE
        messageHandler.body().getKey(), // KEY
        res -> {
          if (res.succeeded()) {
            messageHandler.reply(res.result());
          } else {
            messageHandler.fail(1, "redisGetHandler: redis get failed.");
          }
        });
    };
  }

  // setex handler
  private Handler<Message<RedisSetExRequest>> redisSetExHandler() {
    return messageHandler -> {
      RedisAPI redis = RedisAPI.api(redisClient);
      redis.setex( // REPLIES RESULT (OK|NG)
        messageHandler.body().getKey(), // KEY
        String.valueOf(messageHandler.body().getExpireSeconds()), // Expire seconds
        messageHandler.body().getValue(), // VALUE
        res -> {
          if (res.succeeded()) {
            messageHandler.reply(res.result());
          } else {
            messageHandler.fail(1, "redisSetExHandler: redis setex failed.");
          }
        });
    };
  }

}
