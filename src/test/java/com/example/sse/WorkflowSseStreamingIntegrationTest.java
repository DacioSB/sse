package com.example.sse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for the SSE workflow-tracking endpoint.
 *
 * Guards against two defects that produced "nothing updates, then everything
 * arrives at once" in the browser:
 *
 * 1. Whole-response buffering: Jersey's CommittingOutputStream does not commit
 *    on flush() (jersey-common 3.1.5), so a StreamingOutput whose total output
 *    fits in the 8192-byte default buffer is sent as a single packet with a
 *    computed Content-Length when the stream closes. The test asserts the
 *    first event arrives well before the 12s producer finishes.
 *
 * 2. Malformed SSE framing: writing "event: workflow-update\\n" (escaped) put
 *    the event: and data: fields on one wire line, so line-based clients never
 *    matched the data: field. The test asserts event: and data: are separate
 *    lines.
 */
public class WorkflowSseStreamingIntegrationTest {

    private static final String BASE_URI = "http://localhost:18081/";
    private static final long FIRST_EVENT_DEADLINE_MS = 5000;

    private HttpServer server;

    @Before
    public void setUp() {
        ResourceConfig config = new ResourceConfig();
        config.packages("com.example.sse.Resources");
        config.register(CorsFilter.class);
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void firstWorkflowEventArrivesStreamedAndCorrectlyFramed() throws Exception {
        HttpURLConnection conn =
                (HttpURLConnection) URI.create(BASE_URI + "workflows/track").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");

        long start = System.nanoTime();
        conn.getOutputStream().write("{\"ticketId\":\"INC-009842\"}".getBytes("UTF-8"));
        conn.getOutputStream().flush();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {

            // Blocking read: with correct streaming this returns at ~1s (first
            // producer step); with the buffering defect it returns at ~12s when
            // the whole body is flushed at close.
            String eventLine = reader.readLine();
            long firstLineMillis = (System.nanoTime() - start) / 1_000_000;
            String dataLine = reader.readLine();

            assertTrue("first event arrived at " + firstLineMillis
                            + "ms; response is buffered instead of streamed",
                    firstLineMillis < FIRST_EVENT_DEADLINE_MS);

            assertEquals("event: and data: must be separate SSE lines",
                    "event: workflow-update", eventLine);
            assertNotNull("data line missing after event line", dataLine);
            assertTrue("expected a data: field, got: " + dataLine,
                    dataLine.startsWith("data: {"));
        } finally {
            conn.disconnect();
        }
    }
}
