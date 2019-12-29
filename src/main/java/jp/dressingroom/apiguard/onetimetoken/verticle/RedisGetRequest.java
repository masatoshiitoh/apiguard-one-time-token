package jp.dressingroom.apiguard.onetimetoken.verticle;

public class RedisGetRequest {
  String key;

  public RedisGetRequest(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
