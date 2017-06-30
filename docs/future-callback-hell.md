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

### 多层调用

上面演示了一次异步调用时回调的使用方法。
但是如果我们要在“异步”调用上，实现类似“顺序调用”逻辑呢？比如：

``` java
obj.doSomething1().doSomething2().doSomething3();
```

如果换做异步的回调，应该是：

``` java
doSomething1("Peter").setHandler(r1 -> {
			doSomething2(r1.result()).setHandler(r2 -> {
				doSomething3(r2.result()).setHandler(r3 -> {
					System.out.println("S1->S2->S3: " + r3.result() + " in Thread: " + Thread.currentThread().getName());
				});
			});
		});
```

对比一下，我们就能发现，异步的方式，使用起来实在太麻烦。得亏是Java8支持``Lambda``，否则语法会极其复杂。

具体三个加工环节：

``` java
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
```

### 考虑异常

刚才我们并没有考虑异常处理，如果每个步骤考虑异常处理，这个多层调用会显得更加臃肿：

``` java
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
```

运行结果：

```
Append Hello in Thread: Thread-S1
Append World in Thread: Thread-S2
Append ! in Thread: Thread-S3
S1->S2->S3: Peter, Hello World ! in Thread: Thread-S3
```

### fluent 方式

上面，我们采用了 ``fluent`` 的风格 + Java8的 ``Lambda``，才让异步调用变得简单。如果我们不采用``fluent`` 风格呢？

``` java
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
```

### compose 有何用？

我们刚看了 ``future.setHandler()``，是用来等异步结果出来后，回调对结果做处理的，我们可以在Handler里面进行判断，如果成功，执行一段逻辑；如果失败，执行另一段逻辑。``Future``类，搞了个``compose``方法提供两个参数，第一个是成功时调用的，第二个是失败时调用的。

``` java
default <U> void compose(Handler<T> handler, Future<U> next) {
  setHandler(ar -> {
    if (ar.succeeded()) {
      try {
        handler.handle(ar.result());
      } catch (Throwable err) {
        if (next.isComplete()) {
          throw err;
        }
        next.fail(err);
      }
    } else {
      next.fail(ar.cause());
    }
  });
}
```

> When this future succeeds, the handler will be called with the value.
> When this future fails, the failure will be propagated to the {@code next} future.

尽管它的代码我看明白了，但是似乎这个东西对简化代码没什么价值？！
