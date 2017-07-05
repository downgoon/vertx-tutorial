# Java异步调用

## AsyncCallExample

``` java
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncCallExample {

	public static void main(String[] args) throws Exception {

		Future<String> asyncResult = Executors.newFixedThreadPool(1).submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				System.out.println("\tCall Begin: " + Thread.currentThread().getName());
				Thread.sleep(1000L * 3); // Time Cost
				String result = "Hello, Peter";
				System.out.println("\tCall Data Processed: " + result);
				System.out.println("\tCall Ended: " + Thread.currentThread().getName());
				return result;
			}
		});

		System.out.println("Async Call Return: " + Thread.currentThread().getName());
		String finalResult = asyncResult.get();
		System.out.println("final result: " + finalResult);

	}

}

```

上述代码就是在主线程main里面，执行了一个 **耗时的任务** （任务用``Callable<String>``描述），这个任务生成一个 “Hello, Peter” 字符串，需要耗时3秒，为了不阻塞main线程，特开了一个并发线程来执行它，它的运行结果用``Future<String>``来持有。在主线程中的 ``Future<String> asyncResult`` ，可以立即返回，无需等``Callable<String>``子任务完成。



- 一种运行结果

```
      Call Begin: pool-1-thread-1
Async Call Return: main
      Call Data Processed: Hello, Peter
      Call Ended: pool-1-thread-1
final result: Hello, Peter
```

- 另一种运行结果

```
Async Call Return: main
	Call Begin: pool-1-thread-1
	Call Data Processed: Hello, Peter
	Call Ended: pool-1-thread-1
final result: Hello, Peter
```

## AsyncCallJava8Lambda

上面``Callable``处理函数，用的是``匿名内部类``的形式。从Java8开始，支持``Lambda``表达式，其中一个使用场景就是可以去换掉``匿名内部类``，让代码更加简洁：

``` java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```

在Java8中，原来的``Callable``接口，也被标注为``@FunctionalInterface``。

``` java
Callable<String> callable = () -> { return "Hello, Peter"; };
```

由此``AsyncCallExample.java``可以改写成：

``` java
Future<String> asyncResult = Executors.newFixedThreadPool(1).submit( () -> {
			System.out.println("\tCall Begin: " + Thread.currentThread().getName());
			Thread.sleep(1000L * 3); // Time Cost
			String result = "Hello, Peter";
			System.out.println("\tCall Data Processed: " + result);
			System.out.println("\tCall Ended: " + Thread.currentThread().getName());
			return result;
		});
```
