package com.example.sse;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class App {
    public static void main(String[] args) throws Exception {
        final URI baseUri = URI.create("http://localhost:8080/");

        ResourceConfig config = new ResourceConfig();
        config.packages("com.example.sse.Resources");

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

        System.out.println("Server started at " + baseUri);
        System.out.println("Press Ctrl+C to stop...");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.shutdownNow();
        }));

        Thread.currentThread().join();
    }
}
