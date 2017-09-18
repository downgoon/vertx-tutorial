package org.enterpriseintegration.vertx.tutorial.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

/**
 * GUI是图形的交互，CmdUI是基于命令行的交互
 * 如何基于Event-Bus，搞这个呢？
 * */
public class CmdUI {
	
	public static class Renderer extends AbstractVerticle {

		private BufferedWriter writer;
		
		public Renderer(BufferedWriter writer) {
			super();
			this.writer = writer;
		}

		@Override
		public void start() throws Exception {
			vertx.eventBus().consumer("cmdui", message -> {
				try {
					
					if ("start".equalsIgnoreCase((String)message.body())) {
						writer.write("wait your cmd > ");
						writer.flush();
						
					} else {
						writer.write("echo: "+ message.body());
						writer.newLine();
						writer.write("wait your cmd > ");
						writer.flush();
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		
	}

	
	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
		Renderer renderer = new Renderer(writer);
		
		vertx.deployVerticle(renderer, ar -> {
			if (ar.succeeded()) {
				System.out.println("deploy succ in " + Thread.currentThread().getName());
				vertx.eventBus().publish("cmdui", "start");
				
				vertx.executeBlocking(future -> {
					BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
					String line = null;
					try {
						while (! "quit".equalsIgnoreCase((line = reader.readLine())) ) {
							vertx.eventBus().publish("cmdui", line);
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						future.complete();
					}
					
				}, asyncResult -> {
					System.out.println("程序结束");
					vertx.close();
				});
				

			}
		});
	}

	
	
	
}
