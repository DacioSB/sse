package com.example.sse.Resources;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("events")
public class SsePostResource {
    @POST
    @Path("stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sendMessageStream(
        final String body,
        @Suspended final AsyncResponse asyncResponse
    ){
        StreamingOutput output = outpuStream -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(outpuStream, StandardCharsets.UTF_8))) {
                for (int i = 0; i < 10; i++) {
                    writer.write("id: " + i + "\n");
                    writer.write("event: data-update\n");
                    writer.write("data: {\"index\": " + i + "}\n\n");
                    writer.write(": ping\n\n"); // SSE comment = heartbeat
                    writer.flush();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Response response = Response.ok()
            .type(MediaType.SERVER_SENT_EVENTS_TYPE)
            .header("Cache-Control", "no-cache, no-transform")
            .entity(output)
            .build();
        asyncResponse.resume(response);
    }
}
