package com.fest.visuals.api.utils.jarvis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Talks to the Jarvis voice service over a websocket.
 *
 * <p>One utterance is one text frame carrying its format, immediately followed by one binary
 * frame carrying the raw PCM captured by {@link JarvisAudioCapture}; the server replies the same
 * way, a JSON text frame optionally followed by a WAV binary frame for the spoken reply. There is
 * only ever one utterance in flight, so nothing needs to be tagged with an id.
 *
 * <p>{@link java.net.http.WebSocket.Listener} callbacks run on the HTTP client's own executor, not
 * the game thread — every callback here hands off to {@code mc.execute} before touching anything
 * that isn't thread-safe on its own.
 */
public class JarvisClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private volatile WebSocket socket;
    private volatile boolean connecting;

    private Runnable onOpen;
    private Runnable onClose;
    private Consumer<String> onError;
    private BiConsumer<JsonObject, byte[]> onResult;

    public void onOpen(Runnable listener) { this.onOpen = listener; }
    public void onClose(Runnable listener) { this.onClose = listener; }
    public void onError(Consumer<String> listener) { this.onError = listener; }

    /** Called once per reply: the parsed JSON, and the WAV bytes of the spoken reply (may be empty). */
    public void onResult(BiConsumer<JsonObject, byte[]> listener) { this.onResult = listener; }

    public boolean isConnected() {
        WebSocket s = socket;
        return s != null && !s.isOutputClosed() && !s.isInputClosed();
    }

    public void connect(String url) {
        if (isConnected() || connecting) return;
        connecting = true;

        CompletableFuture<WebSocket> future;
        try {
            future = http.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(url), new FrameListener());
        } catch (IllegalArgumentException e) {
            connecting = false;
            fail("Bad server address: " + e.getMessage());
            return;
        }

        future.whenComplete((ws, error) -> {
            connecting = false;
            if (error != null) {
                fail(error.getMessage());
                return;
            }
            socket = ws;
            if (onOpen != null) onOpen.run();
        });
    }

    public void close() {
        WebSocket s = socket;
        socket = null;
        if (s != null) s.sendClose(WebSocket.NORMAL_CLOSURE, "");
    }

    /** Sends one finished utterance: a JSON header frame, then the raw PCM as a binary frame. */
    public void sendUtterance(byte[] pcm, int sampleRate, String token, JsonObject extra) {
        WebSocket s = socket;
        if (s == null || pcm.length == 0) return;

        JsonObject header = new JsonObject();
        header.addProperty("type", "utterance");
        header.addProperty("sampleRate", sampleRate);
        header.addProperty("bits", 16);
        header.addProperty("channels", 1);
        if (token != null && !token.isEmpty()) header.addProperty("token", token);
        if (extra != null) extra.entrySet().forEach(entry -> header.add(entry.getKey(), entry.getValue()));

        s.sendText(header.toString(), true)
                .thenCompose(ws -> ws.sendBinary(ByteBuffer.wrap(pcm), true))
                .exceptionally(error -> {
                    fail(error.getMessage());
                    return null;
                });
    }

    private void fail(String message) {
        if (onError != null) onError.accept(message == null ? "unknown error" : message);
    }

    private final class FrameListener implements WebSocket.Listener {
        private StringBuilder text;
        private JsonObject pendingResult;

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (text == null) text = new StringBuilder();
            text.append(data);
            webSocket.request(1);

            if (!last) return null;

            String payload = text.toString();
            text = null;

            try {
                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                if (json.has("type") && "error".equals(json.get("type").getAsString())) {
                    fail(json.has("message") ? json.get("message").getAsString() : "server error");
                    return null;
                }
                if (json.has("audio") && json.get("audio").getAsBoolean()) {
                    // A WAV binary frame follows; deliver both together once it arrives.
                    pendingResult = json;
                } else if (onResult != null) {
                    onResult.accept(json, new byte[0]);
                }
            } catch (RuntimeException e) {
                fail("Malformed reply: " + e.getMessage());
            }
            return null;
        }

        private ByteBuffer binaryBuffer;

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            webSocket.request(1);
            
            if (binaryBuffer == null) {
                binaryBuffer = ByteBuffer.allocate(1024 * 1024 * 5); // 5MB max
            }
            binaryBuffer.put(data);

            if (!last) return null;

            binaryBuffer.flip();
            byte[] wav = new byte[binaryBuffer.remaining()];
            binaryBuffer.get(wav);
            binaryBuffer = null;

            if (pendingResult == null) return null;

            JsonObject result = pendingResult;
            pendingResult = null;
            if (onResult != null) onResult.accept(result, wav);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            socket = null;
            fail(error.getMessage());
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            socket = null;
            if (onClose != null) onClose.run();
            return null;
        }
    }
}
