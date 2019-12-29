package jp.dressingroom.apiguard.onetimetoken.verticle;

public class RequestedPayload {
  UserId userId;
  Token token;
  Payload payload;

  public UserId getUserId() {
    return userId;
  }

  public void setUserId(UserId userId) {
    this.userId = userId;
  }

  public Token getToken() {
    return token;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  public Payload getPayload() {
    return payload;
  }

  public void setPayload(Payload payload) {
    this.payload = payload;
  }
}
