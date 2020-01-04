package jp.dressingroom.apiguard.onetimetoken;

public enum ConfigKeyNames {
  ONETIME_TOKEN_SERVER_PORT("onetimetoken.server.port"),

  ONETIME_TOKEN_PROXY_HOSTNAME("onetimetoken.proxy.hostname"),
  ONETIME_TOKEN_PROXY_PORT("onetimetoken.proxy.port"),
  ONETIME_TOKEN_PROXY_USERAGENT("onetimetoken.proxy.ua"),
  ONETIME_TOKEN_PROXY_USESSL("onetimetoken.proxy.usessl"),

  ONETIME_TOKEN_REDIS_HOSTNAME("onetimetoken.redis.hostname"),
  ONETIME_TOKEN_REDIS_PORT("onetimetoken.redis.port"),
  ONETIME_TOKEN_GUARD_METHODS("onetimetoken.guard.methods"),
  ONETIME_TOKEN_INITIALIZE_PATHS("onetimetoken.initialize.paths"),
  ONETIME_TOKEN_USER_ID_PARAM_NAME("onetimetoken.userid.parameter"),
  ONETIME_TOKEN_TOKEN_EXPIRE_SECONDS("onetimetoken.expire.seconds"),
  ;

  private final String text;

  ConfigKeyNames(final String text) {
    this.text = text;
  }

  public String value() {
    return this.text;
  }
}
