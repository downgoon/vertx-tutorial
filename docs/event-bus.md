# event-bus


``` java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class EventBusExample {

	public static class PubVerticle extends AbstractVerticle {

		@Override
		public void start(Future<Void> startFuture) throws Exception {
			System.out.println("PubVerticle start(Future) in: " + Thread.currentThread().getName());

			vertx.setPeriodic(1000L, tm -> {
				System.out.println("pub in thread: " + Thread.currentThread().getName());
				vertx.eventBus().publish("exchange", "Hello");

			});
			startFuture.complete();

		}

	}

	public static class SubVerticle extends AbstractVerticle {

		@Override
		public void start() throws Exception {

			vertx.eventBus().consumer("exchange", msg -> {
				System.out.println("\tconsume: " + msg.body() + " in thread: " + Thread.currentThread().getName());
			});

			System.out.println("SubVerticle start() in: " + Thread.currentThread().getName());
		}

	}


	public static void main(String[] args) throws Exception {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new PubVerticle());
		vertx.deployVerticle(new SubVerticle());
	}

}

```
