package com.example.sse.Consumers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

public class SsePostConsumer {
    public static void main(String[] args) {
        String targetUrl = "http://localhost:8080/events/stream";
        String body = "{\"query\": \"search-term\"}";

        HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(URI.create(targetUrl))
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        
        System.out.println("Connecting to stream...");

        try {
            HttpResponse<Stream<String>> streamResponse = client.send(request, HttpResponse.BodyHandlers.ofLines());

            if (streamResponse.statusCode() == 200) {
                streamResponse.body().forEach(line -> {
                    if (line.startsWith("data:")) {
                        String dataPayload = line.substring(5).trim();
                        System.out.println("Payload: " + dataPayload);
                    } else if (line.startsWith("event:")) {
                        String eventType = line.substring(6).trim();
                        System.out.println("Event Type: " + eventType);
                    }
                });
            } else {
                System.err.println("HTTP error: " + streamResponse.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
