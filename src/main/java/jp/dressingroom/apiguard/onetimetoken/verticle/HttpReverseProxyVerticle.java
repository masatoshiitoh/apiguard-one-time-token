package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jp.dressingroom.apiguard.onetimetoken.ConfigKeyNames;

import java.util.*;

public class HttpReverseProxyVerticle extends AbstractVerticle {
  WebClient client;
  String proxyHost;
  String proxyUserAgent;
  int proxyPort;
  Boolean proxyUseSsl;

  List<String> guardMethods = new ArrayList<>();
  List<String> pathsWithoutToken = new ArrayList<>();
  String userIdParamName;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig(json -> {
      try {
        JsonObject result = json.result();

        // setup onetime token guard
        String methodsConfig = result.getString(ConfigKeyNames.ONETIME_TOKEN_GUARD_METHODS.value(), "GET,POST");
        String initializePathsConfig = result.getString(ConfigKeyNames.ONETIME_TOKEN_INITIALIZE_PATHS.value(), "/login,/init");

        Arrays.stream(methodsConfig.split(",")).forEach(s -> guardMethods.add(s));
        Arrays.stream(initializePathsConfig.split(",")).forEach(s -> pathsWithoutToken.add(s));

        System.out.println("guarding methods:" + guardMethods.toString());
        System.out.println("initialize paths:" + pathsWithoutToken.toString());

        userIdParamName = result.getString(ConfigKeyNames.ONETIME_TOKEN_USER_ID_PARAM_NAME.value(), "userid");

        // setup proxy client
        proxyHost = result.getString(ConfigKeyNames.ONETIME_TOKEN_PROXY_HOSTNAME.value(), "localhost");
        proxyPort = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_PROXY_PORT.value(), 8080);
        proxyUserAgent = result.getString(ConfigKeyNames.ONETIME_TOKEN_PROXY_USERAGENT.value(), "ApiGuard/PayloadEncrypt 1.0");
        proxyUseSsl = result.getBoolean(ConfigKeyNames.ONETIME_TOKEN_PROXY_USESSL.value(), false);

        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setUserAgent(proxyUserAgent);
        client = WebClient.create((Vertx) vertx, webClientOptions);

        Integer port = result.getInteger(ConfigKeyNames.ONETIME_TOKEN_SERVER_PORT.value(), 8891);
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        // Catch all - methods and paths.
        Route route = router.route();
        route.handler(proxyHandler());
        server.requestHandler(router).listen(port);
        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    });
  }

  private RequestOptions copyFromRequest(RoutingContext routingContext) {
    RequestOptions requestOptions = new RequestOptions();
    String uri = routingContext.request().uri();
    MultiMap headers = routingContext.request().headers();
    headers.entries().forEach(s -> requestOptions.addHeader(s.getKey(), s.getValue()));
    requestOptions.setHeaders(headers);
    requestOptions.setHost(proxyHost);
    requestOptions.setURI(uri);
    requestOptions.setSsl(proxyUseSsl);
    requestOptions.setPort(proxyPort);

    return requestOptions;
  }

  private Handler<RoutingContext> proxyHandler() {
    return requestorContext -> {

      requestorContext.request().bodyHandler(bodiedProxyHandler -> {
          EventBus eventBus = vertx.eventBus();

          HttpMethod method = requestorContext.request().method();
          String path = requestorContext.request().path();
          String userId = requestorContext.request().getParam(userIdParamName);
          System.out.println("method is:" + method.name());
          System.out.println("path is:" + path);
          System.out.println("userId is:" + userId);

          if (userId != null) {
            // user id is passed by requester.
            if (guardMethods.contains(method.name().toUpperCase())) {
              // this request must be guarded with onetime token.
              if (pathsWithoutToken.contains(path)) {
                // TODO: following code - work in progress. finish this.

                // initialize token to the user.
                System.out.println("OnetimeToken: init token for " + userId);
                eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_INIT.value(), userId, resetRequest -> {
                  // call proxy, return 200, and reset user's token (not 200, don't change token)
                  RequestOptions requestOptions = copyFromRequest(requestorContext);
                  HttpServerResponse responseToRequestor = requestorContext.response();

                  client
                    .request(method, requestOptions)
                    .ssl(requestOptions.isSsl())
                    .sendBuffer(
                      requestorContext.getBody(),
                      originRequest -> {
                        if (originRequest.succeeded()) {
                          HttpResponse<Buffer> responseFromOrigin = originRequest.result();
                          int statusCode = responseFromOrigin.statusCode();
                          HttpStatusCodes status = HttpStatusCodes.getHttpStatusCode(statusCode);

                          responseToRequestor.headers().setAll(responseFromOrigin.headers());
                          responseToRequestor.headers().set("guardtoken", "initialtoken");

                          if (originRequest.result().body() != null) {
                            responseToRequestor.write(originRequest.result().body());
                          }

                          responseToRequestor
                            .setStatusCode(originRequest.result().statusCode())
                            .end();
                        } else {
                          responseToRequestor
                            .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
                            .end("Origin request failed.");
                        }
                      });
                });

              } else {
                // TODO: following code - work in progress. finish this.

                // rotate token.

                String guardToekn = requestorContext.request().getHeader("guardtoken");
                System.out.println("OnetimeToken: verify token for:" + userId + " passed token is:" + guardToekn);
                JsonObject verifyRequestContent = new JsonObject()
                  .put("userId", userId)
                  .put("token", guardToekn);
                eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_VERIFY.value(), verifyRequestContent, verifyRequest -> {
                  // verify token
                  if (verifyRequest.succeeded()) {
                    System.out.println("OnetimeTokenVerticle replys " + verifyRequest.result().body().toString());
                    if (verifyRequest.result().body().equals(Boolean.TRUE)) {
                      // verify ok
                      System.out.println("OnetimeToken: verify token replies TRUE");

                      // call proxy, return 200, and reset user's token (not 200, don't change token)

                      RequestOptions requestOptions = copyFromRequest(requestorContext);
                      HttpServerResponse responseToRequestor = requestorContext.response();

                      client
                        .request(method, requestOptions)
                        .ssl(requestOptions.isSsl())
                        .sendBuffer(
                          requestorContext.getBody(),
                          originRequest -> {
                            if (originRequest.succeeded()) {
                              HttpResponse<Buffer> responseFromOrigin = originRequest.result();
                              int statusCode = responseFromOrigin.statusCode();
                              HttpStatusCodes status = HttpStatusCodes.getHttpStatusCode(statusCode);

                              responseToRequestor.headers().setAll(responseFromOrigin.headers());
                              responseToRequestor.headers().set("guardtoken", "none");
                              if (originRequest.result().body() != null) {
                                responseToRequestor.write(originRequest.result().body());
                              }
                              responseToRequestor
                                .setStatusCode(originRequest.result().statusCode())
                                .end();
                            } else {
                              responseToRequestor.headers().set("guardtoken", "none");
                              responseToRequestor
                                .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
                                .end("Origin request failed.");
                            }
                          });



                      System.out.println("OnetimeToken: update token for " + userId);
                      eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_UPDATE.value(), userId, resetRequest -> {
                      });
                    } else {
                      // not valid token!
                      System.out.println("OnetimeToken: verify token replies FALSE");
                      // return BAD REQUEST!!
                      HttpServerResponse responseToRequestor = requestorContext.response();
                      responseToRequestor.setStatusCode(HttpStatusCodes.BAD_REQUEST.value());
                      responseToRequestor.end();

                    }
                  }
                });

              }
            } else {
              // this request not be guarded.
              System.out.println("OnetimeToken: ignore method " + method.name());

              // TODO: following code - work in progress. finish this.
              // just proxy it.
              // don't change token.

              RequestOptions requestOptions = copyFromRequest(requestorContext);
              HttpServerResponse responseToRequestor = requestorContext.response();

              client
                .request(method, requestOptions)
                .ssl(requestOptions.isSsl())
                .sendBuffer(
                  requestorContext.getBody(),
                  originRequest -> {
                    if (originRequest.succeeded()) {
                      HttpResponse<Buffer> responseFromOrigin = originRequest.result();
                      int statusCode = responseFromOrigin.statusCode();
                      HttpStatusCodes status = HttpStatusCodes.getHttpStatusCode(statusCode);

                      responseToRequestor.headers().setAll(responseFromOrigin.headers());
                      responseToRequestor.headers().add("guardtoken", "none");
                      if (originRequest.result().body() != null) {
                        responseToRequestor.write(originRequest.result().body());
                      }
                      responseToRequestor
                        .setStatusCode(originRequest.result().statusCode())
                        .end();
                    } else {
                      responseToRequestor.headers().add("guardtoken", "none");
                      responseToRequestor
                        .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
                        .end("Origin request failed.");
                    }
                  });

            }
          } else {
            // user id missing.
            System.out.println("OnetimeToken: user id missing");

            // return BAD REQUEST
            // don't change any token.
            HttpServerResponse responseToRequestor = requestorContext.response();
            responseToRequestor.setStatusCode(HttpStatusCodes.BAD_REQUEST.value());
            responseToRequestor.end();
          }

//          RequestOptions requestOptions = copyFromRequest(requestorContext);
//          HttpServerResponse responseToRequestor = requestorContext.response();
//
//          client
//            .request(method, requestOptions)
//            .ssl(requestOptions.isSsl())
//            .sendBuffer(
//              requestorContext.getBody(),
//              originRequest -> {
//                if (originRequest.succeeded()) {
//                  HttpResponse<Buffer> responseFromOrigin = originRequest.result();
//                  int statusCode = responseFromOrigin.statusCode();
//                  HttpStatusCodes status = HttpStatusCodes.getHttpStatusCode(statusCode);
//
//                  responseToRequestor.headers().setAll(responseFromOrigin.headers());
//                  responseToRequestor.headers().add("guardtoken", "none");
//                  if (originRequest.result().body() != null) {
//                    responseToRequestor.write(originRequest.result().body());
//                  }
//                  responseToRequestor
//                    .setStatusCode(originRequest.result().statusCode())
//                    .end();
//                } else {
//                  responseToRequestor.headers().add("guardtoken", "none");
//                  responseToRequestor
//                    .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
//                    .end("Origin request failed.");
//                }
//              });
        }
      );
    };
  }

}

