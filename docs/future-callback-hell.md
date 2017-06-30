# future-callback-hell

## Future方法介绍

``vert.x``是异步编程。编程方式上用``Future``机制来存储对未来异步执行结果的索取凭证。

### complete

``` java
void complete(T result);
```

>Set the result.
>Any handler will be called, if there is one, and the future will be marked as completed.

设置结果：因为``Future``往往是一个异步任务，完成后设置``complete``存结果。那如果是返回``void``的呢？很简单，不用放入result即可。


``` java
void complete();
```

>Set a null result.
>Any handler will be called, if there is one, and the future will be marked as completed.

### fail

结束任务的另一种形式，上面说的是成功，这里说的是失败。

``` java
void fail(Throwable throwable);
void fail(String failureMessage);
```

>Set the failure.
>Any handler will be called, if there is one, and the future will be marked as completed.

### result

``` java
T result();
```

>The result of the operation. This will be null if the operation failed.

当异步处理，有了结果后，我们可以通过``Future``，这个对未来结果的索取凭证来获取执行结果。

### setHandler

``` java
Future<T> setHandler(Handler<AsyncResult<T>> handler);
```

>**Set a handler for the result**.
>If the future has already been completed it will be called immediately. Otherwise it will be called when the future is completed.

对结果的处理器。传统Java里面，对``Future``结果的处理是这样的：

``` java
Future future = WorkerThreadPool.doJob(new Callable() { ... });
// do something else
String result = future.getResult();
// do something with result
```

这种``Future``的方式，好处是编程模型上比较方便，直接就是``sync``的方式，但是如果我们返回后，理解就执行了``future.getResult();``，还是会卡在那里。于是自然会想，有没有一种``Callback``方式呢？


``` java
void WorkerThreadPool.doJob(new Callable() { ... }, new Callback() {...} );
```

其中：异步任务执行用``Callable``描述，异步结果用``Callback``获取并处理。而``Vertx``的``Future``为了兼有传统``Future``的``future.getResult();``，和``Callback``方式。于是除了``future.getResult();``还有``setHandler``。


``` java
Future future = WorkerThreadPool.doJob(new Callable() { ... });

future.setHandler( asyncResult -> {
  // do something with result
  String result = asyncResult.getResult();
});
```

其中的：``setHandler``逻辑，会自动被调用，而且是等``Callable``完成后，才会执行（在执行任务完成后，必须显示的调用``future.complete()``或``future.fail()``，以实现类似``CountDownLatch``的倒计时信号量）。


## Future 使用

查看代码：[AsyncFutureExample.java](../src/main/java/org/enterpriseintegration/vertx/tutorial/examples/AsyncFutureExample.java)

### 同步中用Future

``` java
public class AsyncFutureExample {

	public static void main(String[] args) {

		String syncResult = doSync("hello");
		System.out.println(syncResult);

		Future<String> future = doAsync("world");

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

}

```

运行结果：

```
doSync in Thread: main
HELLO
doAsync in Thread: main
get result in Handler: WORLD in Thread:main
async call returned
get result directly: WORLD
```

### 异步中用Future

上述的 ``doAsync`` 本身并没有新开线程。改写一个 ``doAsync2`` ：

``` java
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
```

运行结果：

```
doSync in Thread: main
HELLO
async call returned  (因为 doAsync2 开了一个新线程去执行耗时的任务，所以调用会立即返回 )
get result directly: null   (如果异步调用的数据尚未生成，future.result() 不会阻塞，而是返回null )
doAsync2 in Thread: Thread-0  （耗时的任务在另外一个线程中执行）
get result in Handler: WORLD in Thread:Thread-0  （Future回调，当Future完成后，设置的Handler会被回调，而且在同一个线程里）
```

总结几点：

- 非阻塞：如果异步调用的数据尚未生成，``future.result()`` 不会阻塞，而是返回null
- 回调：Future回调，当Future完成后，设置的Handler会被回调，而且在同一个线程里。

## Callback Hell

上面演示了一次异步调用时回调的使用方法。
但是如果我们要在“异步”调用上，实现类似“顺序调用”逻辑呢？比如：

``` java
obj.doSomething1().doSomething2().doSomething3();
```
