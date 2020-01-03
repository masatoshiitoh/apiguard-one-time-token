package jp.dressingroom.apiguard.onetimetoken.verticle;

public enum ApiguardEventBusNames {
  // HTTP_REVERSE_PROXY("http.reverseProxy"),
  ONETIME_TOKEN_INIT("onetimeToken.reset"),
  ONETIME_TOKEN_VERIFY("onetimeToken.verify"),
  ONETIME_TOKEN_UPDATE("onetimeToken.update"),

  REDIS_SETEX("redis.setes"),
  REDIS_GET("redis.get"),
  ;

  private final String text;

  ApiguardEventBusNames(final String text) {
    this.text = text;
  }

  public String value() {
    return this.text;
  }
}
