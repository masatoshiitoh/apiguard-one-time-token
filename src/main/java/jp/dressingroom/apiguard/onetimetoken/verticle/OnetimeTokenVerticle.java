package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class OnetimeTokenVerticle extends AbstractVerticle {
  EventBus eventBus;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    eventBus = vertx.eventBus();

    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_INIT.value(), oneTimeTokenResetHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_VERIFY.value(), oneTimeTokenVerifyHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_UPDATE.value(), oneTimeTokenUpdateHandler());

        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));

  }

  private Handler<Message<String>> oneTimeTokenResetHandler() {
    return messageHandler -> {
      String userId = messageHandler.body();
      messageHandler.reply("newtoken");
    };
  }

  private Handler<Message<JsonObject>> oneTimeTokenVerifyHandler() {
    return messageHandler -> {
      JsonObject verifyRequest = messageHandler.body();
      String userId = verifyRequest.getString("userId");
      String token = verifyRequest.getString("token");
      System.out.println("OnetimeTokenVerticle.oneTimeTokenVerifyHandler: userId " + userId);
      System.out.println("OnetimeTokenVerticle.oneTimeTokenVerifyHandler: token " + token);
      messageHandler.reply(Boolean.TRUE);
    };
  }

  private Handler<Message<String>> oneTimeTokenUpdateHandler() {
    return messageHandler -> {
      messageHandler.reply("nexttoken");
    };
  }

}
