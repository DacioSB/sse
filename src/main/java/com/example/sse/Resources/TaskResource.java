package com.example.sse.Resources;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;

@Path("tasks")
public class TaskResource {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @POST
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    public void startTask(
            @Suspended AsyncResponse asyncResponse) {
        
        executor.submit(() -> {
            try {
                Thread.sleep(3000);
                JsonObject json = Json.createObjectBuilder().add("status", "completed").add("message", "Task finished successfully").build();
                asyncResponse.resume(json);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                asyncResponse.resume(e);
            }
        });

    }
}
