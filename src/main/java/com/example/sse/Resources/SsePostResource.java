package com.example.sse.Resources;

import jakarta.ws.rs.Path;

@Path("events")
public class SsePostResource {
    //sendMessageStream
    //we inform that it's a post, consumes json, produces server_sent_events and the path is "stream"
}
