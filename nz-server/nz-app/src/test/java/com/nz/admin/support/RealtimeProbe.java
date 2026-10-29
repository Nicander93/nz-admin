package com.nz.admin.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

/** 真实传输探针：持续读完事件，关闭底层流以解除阻塞读取。 */
public final class RealtimeProbe {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper JSON = new ObjectMapper();
    private RealtimeProbe() { }

    public static Sse sse(String base, String ticket) throws Exception {
        HttpResponse<InputStream> response = sseResponse(base, ticket);
        if (response.statusCode() != 200) {
            response.body().close();
            throw new AssertionError("SSE status " + response.statusCode());
        }
        return new Sse(response.body());
    }

    public static int sseStatus(String base, String ticket) throws Exception {
        HttpResponse<InputStream> response = sseResponse(base, ticket);
        try (InputStream ignored = response.body()) { return response.statusCode(); }
    }

    private static HttpResponse<InputStream> sseResponse(String base, String ticket) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + "/realtime/sse?ticket=" + ticket))
                .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
    }

    public static Socket socket(String base, String ticket) throws Exception {
        Socket listener = new Socket();
        listener.socket = HTTP.newWebSocketBuilder().buildAsync(URI.create(base.replace("http://", "ws://")
                + "/realtime/ws?ticket=" + ticket), listener).get(8, TimeUnit.SECONDS);
        return listener;
    }

    public static void assertSocketRejected(String base, String ticket) throws Exception {
        try {
            Socket opened = socket(base, ticket);
            opened.close();
            throw new AssertionError("invalid ticket opened WebSocket");
        } catch (ExecutionException error) {
            assertThat(error.getCause()).isInstanceOf(WebSocketHandshakeException.class);
            assertThat(((WebSocketHandshakeException) error.getCause()).getResponse().statusCode()).isEqualTo(401);
        }
    }

    public abstract static class Events implements AutoCloseable {
        protected final List<JsonNode> received = new CopyOnWriteArrayList<>();
        protected final CountDownLatch closed = new CountDownLatch(1);
        protected volatile Throwable failure;

        public List<JsonNode> events() { return List.copyOf(received); }
        public boolean isClosed() { return closed.getCount() == 0; }
        public void await(String marker) {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
                assertThat(failure).isNull();
                assertThat(received.toString()).contains(marker);
            });
        }
        public void awaitClosed() throws InterruptedException {
            assertThat(closed.await(8, TimeUnit.SECONDS)).as("server closes revoked connection").isTrue();
        }
        protected void receive(String text) {
            try { received.add(JSON.readTree(text)); }
            catch (IOException error) { failure = error; }
        }
        @Override public abstract void close() throws IOException;
    }

    public static final class Sse extends Events {
        private final InputStream input;
        private final Thread readerThread;
        private Sse(InputStream input) {
            this.input = input;
            readerThread = new Thread(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) if (line.startsWith("data:")) receive(line.substring(5).trim());
                } catch (IOException error) {
                    // EOF/reset are both valid server closure signals for these probes.
                } finally { closed.countDown(); }
            }, "nz-realtime-sse-probe");
            readerThread.setDaemon(true);
            readerThread.start();
        }
        @Override public void close() throws IOException {
            input.close();
            try { readerThread.join(1000); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
    }

    public static final class Socket extends Events implements WebSocket.Listener {
        private final StringBuilder frame = new StringBuilder();
        private WebSocket socket;
        @Override public void onOpen(WebSocket connection) { connection.request(1); }
        @Override public CompletionStage<?> onText(WebSocket connection, CharSequence data, boolean last) {
            frame.append(data);
            if (last) { receive(frame.toString()); frame.setLength(0); }
            connection.request(1);
            return CompletableFuture.completedFuture(null);
        }
        @Override public CompletionStage<?> onClose(WebSocket connection, int status, String reason) {
            closed.countDown(); return CompletableFuture.completedFuture(null);
        }
        @Override public void onError(WebSocket connection, Throwable error) { failure = error; closed.countDown(); }
        @Override public void close() { if (socket != null) socket.abort(); }
    }
}
