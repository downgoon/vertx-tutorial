package org.enterpriseintegration.vertx.tutorial.examples;

import io.vertx.core.Vertx;

public class VertxBlockingExample {

	public static void main(String[] args) throws Exception {
		Vertx vertx = Vertx.vertx();
		// vertx.executeBlocking(blockingCodeHandler, resultHandler);
		
		vertx.executeBlocking(future -> {
			
			try {
				System.out.println("\tCall Begin: " + Thread.currentThread().getName());
				Thread.sleep(1000L * 3); // Time Cost
				String result = "Hello, Peter";
				System.out.println("\tCall Data Processed: " + result);
				System.out.println("\tCall Ended: " + Thread.currentThread().getName());
				// return result;
				
				future.complete(result);
				
			} catch (Exception e) {
				future.fail(e);
			}
			
		}, asyncResult -> {
			
			if (asyncResult.succeeded()) {
				System.out.println("Async Result Got: " + Thread.currentThread().getName());
				String finalResult = (String) asyncResult.result();
				System.out.println("final result: " + finalResult);
			}
			
		});
		
		System.out.println("Async Call Return: " + Thread.currentThread().getName());
	}

}
