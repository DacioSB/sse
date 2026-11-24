# SSE — Server-Sent Events Playground

A Java demo project exploring **Server-Sent Events (SSE)** with Jakarta REST (Jersey) on an embedded Grizzly HTTP server. It simulates an IT Service Management automation ("ServiceNow Workflow Tracker") that streams live workflow progress updates to a browser, plus several companion endpoints demonstrating async request handling patterns.

## Tech Stack

- **Java 17** (Maven)
- **Jersey 3.1.5** (JAX-RS implementation) on **Grizzly** HTTP server
- **Jakarta EE API 11** (JAX-RS, JSON Processing)
- **JUnit 4** for tests

## Getting Started

```bash
# Build
mvn package

# Start the server (default: http://localhost:8080/)
mvn exec:java -Dexec.mainClass="com.example.sse.App"
# or run with a custom base URI:
java -cp target/classes:... com.example.sse.App http://localhost:8080/

# Run tests
mvn test
```

Then open `workflow_task.html` in a browser (or serve it with any static file server — it must be reachable from `http://localhost:8080`). Enter a ticket ID and click **Run Workflow** to watch progress stream in real time.

## Project Structure

```
src/
├── main/java/com/example/sse/
│   ├── App.java                          # Entry point: boots Grizzly server, registers resources + CORS
│   ├── CorsFilter.java                   # Permissive CORS response filter (for browser demos)
│   ├── Consumers/
│   │   └── SsePostConsumer.java          # Standalone CLI client that consumes an SSE stream
│   └── Resources/
│       ├── HeartbeatSseResource.java     # POST /workflows/track — main workflow demo
│       ├── SsePostResource.java          # POST /events/stream — simple SSE stream
│       ├── JobResource.java              # POST /jobs/process — async job with timeout/cancellation
│       └── TaskResource.java             # POST /tasks/start — basic async task
└── test/java/com/example/sse/
    ├── AppTest.java                      # Placeholder unit test
    └── WorkflowSseStreamingIntegrationTest.java  # Regression test for SSE streaming
```

## Endpoints

### `POST /workflows/track` — Workflow Tracker (main demo)

Produces `text/event-stream`. Accepts a JSON body (e.g. `{"ticketId": "INC-009842"}`) and streams a simulated remediation workflow as `workflow-update` events. Each event is a JSON payload:

```json
{"percentage": 40, "status": "IN_PROGRESS", "message": "Diagnostic health checks in progress..."}
```

- Steps progress 15 → 40 → 75 → 90 → 100% (`COMPLETED`), with delays between steps.
- While no step is pending, the server writes `: keep-alive\n\n` SSE comments every 2 seconds — the browser UI uses these as a heartbeat pulse.
- Response headers disable caching/proxy buffering (`Cache-Control: no-cache, no-transform`, `X-Accel-Buffering: no`).
- Implemented with Jersey's `ChunkedOutput` + a producer thread feeding a `LinkedBlockingDeque`.

### `POST /events/stream` — Simple SSE Stream

Produces `text/event-stream`. Streams 10 `data-update` events (one per second), each with an `id`, `event` name, and `data` JSON payload, followed by a `: ping` heartbeat comment. Implemented with `StreamingOutput`.

### `POST /jobs/process` — Async Job with Timeout

Produces `application/json`. Submits a 3-second simulated job on a fixed thread pool and resumes the suspended `AsyncResponse` when done. Demonstrates:

- `setTimeout` + `setTimeoutHandler` → responds `408 Request Timeout` if the job exceeds 5 seconds.
- `CompletionCallback` → cancels the future when the request completes with an error.
- `ConnectionCallback` → cancels the job if the client disconnects.

### `POST /tasks/start` — Basic Async Task

Produces `application/json`. The simplest example: a 3-second simulated task on a single-thread executor, then resumes with `{"status": "completed", "message": "Task finished successfully"}`.

## Browser Client (`workflow_task.html`)

A single-page UI that consumes the `/workflows/track` stream using the Fetch API's `ReadableStream` reader (note: `EventSource` only supports GET, so a POST-based SSE endpoint needs manual stream parsing):

- Parses the SSE wire format: splits on blank lines, reads `data:` fields as JSON, and treats `:`-prefixed comment lines as heartbeat pulses (the status dot pulses on each keep-alive).
- Updates a progress bar, status text, and a timestamped log timeline; flips the badge to **Resolved** when `status === "COMPLETED"`.
- Shows socket state (Active / Closed / Disconnected) and resets on each run.

A matching `workflow_task.css` provides the styling.

There's also a Java CLI consumer (`Consumers/SsePostConsumer`) that hits `POST /events/stream` with `HttpClient` + `BodyHandlers.ofLines()` and prints `event:`/`data:` lines to stdout.

## Tests

- **`WorkflowSseStreamingIntegrationTest`** — boots the server on port 18081 and guards against two streaming defects:
  1. **Buffering**: Jersey's `CommittingOutputStream` (jersey-common 3.1.5) doesn't commit on `flush()`, so small `StreamingOutput` bodies can be sent as one packet at close. The test asserts the first event arrives within 5 seconds.
  2. **Framing**: `event:` and `data:` fields must be on separate wire lines so line-based clients parse them correctly.

Run with:

```bash
mvn test
```

## Notes

- `CorsFilter` sends `Access-Control-Allow-Origin: *` — fine for local demos, lock it down for anything real.
- All endpoints are simulated with `Thread.sleep`; there's no persistence or real ServiceNow integration.
