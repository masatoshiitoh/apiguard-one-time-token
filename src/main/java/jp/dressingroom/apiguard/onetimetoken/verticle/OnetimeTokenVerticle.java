package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class OnetimeTokenVerticle extends AbstractVerticle {
  EventBus eventBus;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    eventBus = vertx.eventBus();

    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_RESET.value(), oneTimeTokenResetHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_VERIFY.value(), oneTimeTokenVerifyHandler());
        eventBus.consumer(ApiguardEventBusNames.ONETIME_TOKEN_UPDATE.value(), oneTimeTokenUpdateHandler());

        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));

  }

  // UserIDに紐付いたUUIDをリセットし、あたらしいUUIDを払い出す
  private Handler<Message<String>> oneTimeTokenResetHandler() {
    return messageHandler -> {
    };
  }


  // UserIDに紐付いたOnetime token (UUID) を取得し、
  // 前回Onetime tokenだったらキャッシュを返却
  // 今回Onetime tokenだったらVerifiledを返却(これを受けて、呼び出し元は実APIを呼び出すはず）
  // どちらでもなければ NotFound を返却（これを受けて、呼び出し元はエラーを返すはず）
  private Handler<Message<String>> oneTimeTokenVerifyHandler() {
    return messageHandler -> {
    };
  }


  // UserID とトークンとペイロードを受け取って、更新処理をおこなう
  // トークンがnextでなければ黙って成功を返す。内容は廃棄（前回トークンの更新処理が再度来た可能性があるので、失敗では無く、うまくいったと思わせる必要がある＿
  // トークンがnextなら、nextを今回に、レスポンスキャッシュを更新、トークンを払い出す、トークンをローテート更新して、新しい状態を返却する
  private Handler<Message<String>> oneTimeTokenUpdateHandler() {
    return messageHandler -> {
    };
  }

}
