package com.example.sse.Resources;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ConnectionCallback;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("jobs")
public class JobResource {
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    @POST
    @Path("process")
    @Produces(MediaType.APPLICATION_JSON)
    public void process(
            @Suspended AsyncResponse asyncResponse) {

        JsonObject jsonTimeout = Json.createObjectBuilder().add("status", "timeout").add("message", "Task timed out").build();
        JsonObject jsonCompleted = Json.createObjectBuilder().add("status", "completed").add("message", "Task completed successfully").build();

        asyncResponse.setTimeout(5, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(
                Response.status(Response.Status.REQUEST_TIMEOUT)
                    .entity(jsonTimeout)
                    .type(MediaType.APPLICATION_JSON)
                    .build()
            );
        });

        Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(3000);
                asyncResponse.resume(Response.ok(jsonCompleted)
                    .type(MediaType.APPLICATION_JSON)
                    .build());
            } catch (InterruptedException e) {
                System.out.println("Background job was successfully cancelled/interrupted!");
                Thread.currentThread().interrupt();
            }
        });

        asyncResponse.register((CompletionCallback) throwable -> {
            if (throwable != null) {
                System.out.println("Request completed with error: " + throwable.getMessage());
            }
            future.cancel(true);
        });
        asyncResponse.register((ConnectionCallback) disconnectedResponse -> {
            System.out.println("Client disconnected unexpectedly!");
            future.cancel(true);
        });

    }
}
