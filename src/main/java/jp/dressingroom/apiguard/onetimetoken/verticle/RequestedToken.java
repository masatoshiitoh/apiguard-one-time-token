package jp.dressingroom.apiguard.onetimetoken.verticle;

public class RequestedToken {
  public UserId getUserId() {
    return userId;
  }

  public Token getToken() {
    return token;
  }

  public void setUserId(UserId userId) {
    this.userId = userId;
  }

  public void setToken(Token token) {
    this.token = token;
  }

  UserId userId;
  Token token;
}
