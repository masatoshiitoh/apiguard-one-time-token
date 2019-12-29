package jp.dressingroom.apiguard.onetimetoken.verticle;

import java.util.Arrays;

public enum HttpStatusCodes {
  OK(200),
  BAD_REQUEST(400),
  NOT_FOUND(404),
  INTERNAL_SERVER_ERROR(500),
  ;

  private final Integer status;

  static HttpStatusCodes getHttpStatusCode(Integer status) {
    HttpStatusCodes result = Arrays.stream(HttpStatusCodes.values())
      .filter(s -> s.value().equals(status))
      .findFirst()
      .orElse(null);
    return result;
  }

  HttpStatusCodes(final Integer status) {
    this.status = status;
  }

  public Integer value() {
    return this.status;
  }
}
