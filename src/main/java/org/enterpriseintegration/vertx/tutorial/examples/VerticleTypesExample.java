package org.enterpriseintegration.vertx.tutorial.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class VerticleTypesExample {

	private static final Logger LOG = LoggerFactory.getLogger(VerticleTypesExample.class);

	public static class MyStandardVerticle extends AbstractVerticle {

		@Override
		public void start() throws Exception {
			super.start();
			LOG.info("MyStandardVerticle start() in Thread:  {} " + Thread.currentThread().getName());
		}
		

		
		@Override
		public void start(Future<Void> startFuture) throws Exception {
			
			/* 异步 start 和 同步 start 同时存在时，只会调用 异步start */
			
			// Thread.sleep(1000L * 60 * 60);
			LOG.info("MyStandardVerticle start(Future) in Thread:  {} " + Thread.currentThread().getName());
//			vertx.setTimer(1000L * 60, tm -> {
//				startFuture.complete();
//			});
			
			startFuture.complete();
			
		}



		@Override
		public void stop() throws Exception {
			super.stop();
			LOG.info("MyStandardVerticle stop in Thread:  {} " + Thread.currentThread().getName());
		}

	}

	public static void main(String[] args) throws Exception {
		Vertx vertx = Vertx.vertx();
		
		DeploymentOptions options = new DeploymentOptions().setWorker(true);
		
		LOG.info("create vertx: " + vertx.toString());
		
		MyStandardVerticle sv = new MyStandardVerticle();
		vertx.deployVerticle(sv, options);  // worker thread
		
//		Thread.sleep(1000L * 5);
//		
//		vertx.deployVerticle(sv);
//		
//		vertx.deployVerticle(sv);
	}

}
