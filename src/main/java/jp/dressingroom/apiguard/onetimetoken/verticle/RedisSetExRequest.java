package jp.dressingroom.apiguard.onetimetoken.verticle;

public class RedisSetExRequest {
  String key;
  String value;
  Integer expireSeconds;

  public RedisSetExRequest(String key, String value, Integer expireSeconds) {
    this.key = key;
    this.value = value;
    this.expireSeconds = expireSeconds;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getExpireSeconds() {
    return expireSeconds;
  }

  public void setExpireSeconds(Integer expireSeconds) {
    this.expireSeconds = expireSeconds;
  }
}
