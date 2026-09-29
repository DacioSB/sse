package com.example.sse.Resources;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("workflows")
public class HeartbeatSseResource {

    record WorkflowStep(int percentage, String status, String message, boolean isTerminal) {}
  
    @POST 
    @Path("track")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void streamWithHeartbeat(
        final String requestBody,
        @Suspended AsyncResponse asyncResponse) {
        final LinkedBlockingDeque<WorkflowStep> queue = new LinkedBlockingDeque<>();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                queue.put(new WorkflowStep(15, "IN_PROGRESS", "Incident ticket assigned to automation bot.", false));
                Thread.sleep(2000);
                queue.put(new WorkflowStep(40, "IN_PROGRESS", "Diagnostic health checks in progress...", false));
                Thread.sleep(5000);
                queue.put(new WorkflowStep(75, "IN_PROGRESS", "Applying server patch and restarting service...", false));
                Thread.sleep(2500);
                queue.put(new WorkflowStep(90, "IN_PROGRESS", "Verifying service availability via synthetic probes...", false));
                Thread.sleep(1500);
                queue.put(new WorkflowStep(100, "COMPLETED", "Incident resolved successfully. Closed in ServiceNow.", true));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        //
        StreamingOutput stream = (outputStream) -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                boolean active = true;
                while (active) {
                    
                    WorkflowStep step = queue.poll(2, TimeUnit.SECONDS);
                    if (step == null) {
                        writer.write(": keep-alive\n\n");
                        writer.flush();
                        continue;                        
                    }
                    String jsonPayload = String.format(
                            "{\"percentage\": %d, \"status\": \"%s\", \"message\": \"%s\"}",
                            step.percentage(), step.status(), step.message()
                    );
                    writer.write("event: workflow-update\\n");
                    writer.write("data: " + jsonPayload + "\n\n");
                    writer.flush();
                    if (step.isTerminal) {
                        active = false;
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        };
        asyncResponse.resume(
                Response.ok(stream)
                        .header("Cache-Control", "no-cache, no-transform")
                        .header("Connection", "keep-alive")
                        .header("X-Accel-Buffering", "no") 
                        .build());
    }
}
