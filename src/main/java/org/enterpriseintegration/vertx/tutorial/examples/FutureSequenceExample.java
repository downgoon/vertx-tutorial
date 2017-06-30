package org.enterpriseintegration.vertx.tutorial.examples;

import io.vertx.core.Future;

public class FutureSequenceExample {

	public static void main(String[] args) {
		// Future<String> s1 = doSomething1("Peter");
		
		doSomething1("Peter").setHandler(r1 -> {
			doSomething2(r1.result()).setHandler(r2 -> {
				doSomething3(r2.result()).setHandler(r3 -> {
					System.out.println("S1->S2->S3: " + r3.result() + " in Thread: " + Thread.currentThread().getName());
				});
			});
		});
		
		
		doSomething1("Peter").setHandler(r1 -> {
			
			if(r1.failed()) {
				throw new IllegalStateException("Fail in Step-1", r1.cause());
			}
			
			doSomething2(r1.result()).setHandler(r2 -> {
				
				if (r2.failed()) {
					throw new IllegalStateException("Fail in Step-2", r2.cause());
				}
				
				doSomething3(r2.result()).setHandler(r3 -> {
					
					if (r3.failed()) {
						throw new IllegalStateException("Fail in Step-3", r3.cause());
					}
					
					System.out.println("S1->S2->S3: " + r3.result() + " in Thread: " + Thread.currentThread().getName());
				});
			});
		});
		
		
		
		Future<String> s1 = doSomething1("Peter");
		s1.setHandler(r1 -> {
			if (r1.failed()) {
				throw new IllegalStateException("Fail in Step-1", r1.cause());
			}
			
			Future<String> s2 = doSomething2(r1.result());
			s2.setHandler(r2 -> {
				if (r2.failed()) {
					throw new IllegalStateException("Fail in Step-2", r2.cause());
				}
				Future<String> s3 = doSomething3(r2.result());
				System.out.println("S1->S2->S3: " + s3.result() + " in Thread: " + Thread.currentThread().getName());
				
			});
			
		});
		
		// compose
		
		doSomething1("Peter").setHandler(r1 -> {
			doSomething2(r1.result()).setHandler(r2 -> {
				doSomething3(r2.result()).setHandler(r3 -> {
					System.out.println("S1->S2->S3: " + r3.result() + " in Thread: " + Thread.currentThread().getName());
				});
			});
		});
		
	}
	
	
	static Future<String> doSomething1(String input) {
		Future<String> r1 = Future.future();
		new Thread(() -> {
			try {
				Thread.sleep(1000L * 3);
				System.out.println("Append Hello in Thread: " + Thread.currentThread().getName());
				r1.complete(input+ ", Hello");
			} catch (Exception e) {
				r1.fail(e);
			}
			
		}, "Thread-S1").start();
		
		return r1;
	}

	static Future<String> doSomething2(String input) {
		Future<String> r2 = Future.future();
		new Thread(() -> {
			try {
				Thread.sleep(1000L * 2);
				System.out.println("Append World in Thread: " + Thread.currentThread().getName());
				r2.complete(input+ " World");
			} catch (Exception e) {
				r2.fail(e);
			}
			
		}, "Thread-S2").start();
		
		return r2;
		
	}
	
	static Future<String> doSomething3(String input) {
		Future<String> r3 = Future.future();
		new Thread(() -> {
			try {
				Thread.sleep(1000L * 1);
				System.out.println("Append ! in Thread: " + Thread.currentThread().getName());
				r3.complete(input+ " !");
			} catch (Exception e) {
				r3.fail(e);
			}
			
		}, "Thread-S3").start();
		
		return r3;
	}

}
