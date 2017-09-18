package org.enterpriseintegration.vertx.tutorial.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

public class Proxy extends AbstractVerticle {

	public static void main(String[] args) {

		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new Proxy(), ar -> {
			if (ar.succeeded()) {
				System.out.println("部署成功");
			} else {
				System.out.println("部署失败");
			}
		});

	}

	@Override
	public void start() throws Exception {
		HttpClient client = vertx.createHttpClient(new HttpClientOptions());
		
		vertx.createHttpServer().requestHandler(req -> {
			System.out.println("Proxying request: " + req.uri());
			
			HttpClientRequest fwd_req = client.request(req.method(), 8282, "localhost", req.uri(), fwd_res -> {
				System.out.println("Proxying response: " + fwd_res.statusCode());
				req.response().setChunked(true);
				req.response().setStatusCode(fwd_res.statusCode());
				req.response().headers().setAll(fwd_res.headers());
				fwd_res.handler(data -> {
					System.out.println("Proxying response body: " + data.toString("ISO-8859-1"));
					req.response().write(data);
				});
				fwd_res.endHandler((v) -> req.response().end());
				
				
			});
			
			
			fwd_req.setChunked(true);
			fwd_req.headers().setAll(req.headers());
			
			req.handler(data -> {
				System.out.println("Proxying request body " + data.toString("ISO-8859-1"));
				fwd_req.write(data);
			});
			
			 req.endHandler((v) -> fwd_req.end());
			
		}).listen(8080);
	}
}