package org.enterpriseintegration.vertx.tutorial.examples.example05;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Example 05 - Standard: Deploy a standard verticle
 */
public class StandardSample extends AbstractVerticle {

	private static final int instances = 2;

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle("org.enterpriseintegration.vertx.tutorial.examples.example05.StandardSample",
				// Instantiate a DeploymentOptions by setting an explicit number
				// of instances
				new DeploymentOptions().setInstances(instances), ar -> {
					if (ar.succeeded()) {
						System.out.println("Standard verticle deployed");

						// Send messages on the event bus
						EventBus eventBus = vertx.eventBus();
						eventBus.publish("event", "event01");
						eventBus.send("event", "event02");
						eventBus.send("event", "event03");
					} else {
						System.out.println("Error while deploying a verticle: " + ar.cause().getMessage());
					}
				});
	}

	@Override
	public void start(Future<Void> startFuture) {
		System.out.println("Creating an instance with PID " + Thread.currentThread().getId());

		EventBus eventBus = vertx.eventBus();

		// Consume messages on the event bus
		eventBus.consumer("event", message -> {
			System.out.println("PID " + Thread.currentThread().getId() + " received " + message.body());
			try {
				Thread.sleep(3000);
			} catch (Exception e) {
			}
		});

		startFuture.complete();
	}
}