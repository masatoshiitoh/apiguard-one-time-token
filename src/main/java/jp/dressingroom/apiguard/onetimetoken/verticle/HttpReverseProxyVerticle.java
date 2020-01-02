package jp.dressingroom.apiguard.onetimetoken.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
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
        String methodsConfig= result.getString(ConfigKeyNames.ONETIME_TOKEN_GUARD_METHODS.value(), "GET,POST");
        String initializePathsConfig= result.getString(ConfigKeyNames.ONETIME_TOKEN_INITIALIZE_PATHS.value(), "/login,/init");

        Arrays.stream(methodsConfig.split(",")).forEach(s -> guardMethods.add(s));
        Arrays.stream(initializePathsConfig.split(",")).forEach(s -> pathsWithoutToken.add(s));
        userIdParamName = result.getString(ConfigKeyNames.ONETIME_TOKEN_USER_ID_PARAM_NAME.value(), "opensocial_owner_id");

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
          HttpMethod method = requestorContext.request().method();
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
        }
      );
    };
  }
}
