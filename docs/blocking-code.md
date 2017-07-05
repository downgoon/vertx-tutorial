# blocking-code

## 官方文档

``` java
/**
 * Safely execute some blocking code.
 * <p>
 * Executes the blocking code in the handler {@code blockingCodeHandler} using a thread from the worker pool.
 * <p>
 * When the code is complete the handler {@code resultHandler} will be called with the result on the original context
 * (e.g. on the original event loop of the caller).
 * <p>
 * A {@code Future} instance is passed into {@code blockingCodeHandler}. When the blocking code successfully completes,
 * the handler should call the {@link Future#complete} or {@link Future#complete(Object)} method, or the {@link Future#fail}
 * method if it failed.
 * <p>
 * In the {@code blockingCodeHandler} the current context remains the original context and therefore any task
 * scheduled in the {@code blockingCodeHandler} will be executed on the this context and not on the worker thread.
 *
 * @param blockingCodeHandler  handler representing the blocking code to run
 * @param resultHandler  handler that will be called when the blocking code is complete
 * @param ordered  if true then if executeBlocking is called several times on the same context, the executions
 *                 for that context will be executed serially, not in parallel. if false then they will be no ordering
 *                 guarantees
 * @param <T> the type of the result
 */
<T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, boolean ordered, Handler<AsyncResult<T>> resultHandler);

/**
 * Like {@link #executeBlocking(Handler, boolean, Handler)} called with ordered = true.
 */
<T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);
```

## 读摘要点


- blockingCodeHandler

>@param blockingCodeHandler  handler representing the blocking code to run

>Executes the blocking code in the handler {@code blockingCodeHandler} using **a thread from the worker pool**.

这个执行方式非常简单，就是类似 [async-call.md](async-call.md) 里面的方式。


执行代码时注意：

>A {@code Future} instance is passed into {@code blockingCodeHandler}. When the blocking code successfully completes, the handler should call the {@link Future#complete} or {@link Future#complete(Object)} method, or the {@link Future#fail} method if it failed.

当我们在``blockingCodeHandler``执行需要阻塞的代码时，我们做完了，一定要手动“显式的”调用 {@link Future#complete} 或 {@link Future#fail}，而不能靠方法自动``return``。

为什么执行阻塞代码的时候就需要``vertx.executeBlocking()``绕一趟下呢？本质上它就是个往线程池里面丢个Job。


- resultHandler

>@param resultHandler  handler that will be called **when the blocking code is complete**

>When the code (指的是``blockingCodeHandler`` ) is complete the handler {@code resultHandler} will be called with the result on the original context (e.g. on the original event loop of the caller).


## 实验代码


``` java
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

```

运行结果：

```
Async Call Return: main
	Call Begin: vert.x-worker-thread-0
	Call Data Processed: Hello, Peter
	Call Ended: vert.x-worker-thread-0
Async Result Got: vert.x-eventloop-thread-0
final result: Hello, Peter
```

从运行结果和官方文档综合看，有几点特别强调：

- ``blockingCodeHandler``代码：这段代码是通过执行在``vert.x-worker-thread-0``线程，以防止IO阻塞操作去阻碍``vert.x-eventloop-thread-0``的运行。
- ``resultHandler``代码：提取运行结果的代码已经逃离了``worker``线程，运行结果会看到“Async Result Got: vert.x-eventloop-thread-0”，它运行在``eventloop``线程上，既不是``worker``线程，也不是调用方``main``线程。
- 将异步进行到底：``vertx.executeBlocking()`` 执行调用后，立即就返回。尽管我们这个例子是运行在``main``线程里面，但如果我们在``vertx.createHttpServer()``里面，这个调用就运行在``eventloop``里面，它立即返回就不会丝毫阻塞``eventloop``。

## 对吗类比

将代码[VertxBlockingExample.java](../src/main/java/org/enterpriseintegration/vertx/tutorial/examples/VertxBlockingExample.java)与[AsyncCallJava8Lambda.java](../src/main/java/org/enterpriseintegration/vertx/tutorial/examples/AsyncCallJava8Lambda.java)做个对比

``` java
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
```

同样是，有段代码需要异步执行，JDK的就优雅很多，形如``<T> Future<T> submit(Callable<T> task);``，特点：

- 用``Callable``表示需要异步执行的代码，执行完，直接return即可。异步代码在``worker``线程池中运行。
- 用``Future``表示对未来执行结果索取凭证，需要结果时，``future.get()``一下。提取结果代码在调用者线程中运行。

然而，``vertx``的代码，就完全的``异步``范儿十足，形如：``<T> void executeBlocking(Handler<Future<T>> blockingCodeHandler, Handler<AsyncResult<T>> resultHandler);``，相当于把``<T> Future<T> submit(Callable<T> task);`` 变换成 ``void submit(Callable<T> task, Future<T> result);``。
