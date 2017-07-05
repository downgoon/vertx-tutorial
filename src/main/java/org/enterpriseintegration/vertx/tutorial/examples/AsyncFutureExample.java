package org.enterpriseintegration.vertx.tutorial.examples;

import io.vertx.core.Future;

public class AsyncFutureExample {

	public static void main(String[] args) {
		
		String syncResult = doSync("hello");
		System.out.println(syncResult);
		
		// Future<String> future = doAsync("world");
		Future<String> future = doAsync2("world");
		future.setHandler(asyncResult -> {
			if (asyncResult.succeeded()) {
				System.out.println("get result in Handler: "+ future.result() + " in Thread:" + Thread.currentThread().getName());
			} else {
				System.out.println("future fail");
			}
		});
		System.out.println("async call returned");
		System.out.println("get result directly: "+ future.result());
		
	}

	
	static String doSync(String input) {
		System.out.println("doSync in Thread: " + Thread.currentThread().getName());
		return input.toUpperCase();
	}

	static Future<String> doAsync(String input) {
		Future<String> future = Future.future();
		
		try {
			Thread.sleep(1000L * 3);
			System.out.println("doAsync in Thread: " + Thread.currentThread().getName());
			String result = input.toUpperCase();
			future.complete(result);
		} catch (Exception e) {
			future.fail(e);
		}
		
		return future;
	}
	
	static Future<String> doAsync2(String input) {
		Future<String> future = Future.future();
		
		new Thread(() -> {
			
			try {
				Thread.sleep(1000L * 3);
				System.out.println("doAsync2 in Thread: " + Thread.currentThread().getName());
				String result = input.toUpperCase();
				future.complete(result);
			} catch (Exception e) {
				future.fail(e);
			}
			
		}).start();
		
		return future;
	}
	
}
