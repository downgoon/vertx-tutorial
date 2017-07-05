package org.enterpriseintegration.vertx.tutorial.examples;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncCallJava8Lambda {

	public static void main(String[] args) throws Exception {

		Future<String> asyncResult = Executors.newFixedThreadPool(1).submit(() -> {
			System.out.println("\tCall Begin: " + Thread.currentThread().getName());
			Thread.sleep(1000L * 3); // Time Cost
			String result = "Hello, Peter";
			System.out.println("\tCall Data Processed: " + result);
			System.out.println("\tCall Ended: " + Thread.currentThread().getName());
			return result;
		});

		System.out.println("Async Call Return: " + Thread.currentThread().getName());
		String finalResult = asyncResult.get();
		System.out.println("final result: " + finalResult);

	}

}
