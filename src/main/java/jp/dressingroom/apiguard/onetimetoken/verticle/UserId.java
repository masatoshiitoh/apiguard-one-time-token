package jp.dressingroom.apiguard.onetimetoken.verticle;

public class UserId {
  String value;

  public UserId(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
