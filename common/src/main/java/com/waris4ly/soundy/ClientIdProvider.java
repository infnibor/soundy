package com.waris4ly.soundy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientIdProvider {

    private static final String SOUNDCLOUD_HOME = "https://soundcloud.com";
    private static final Pattern SCRIPT_SRC = Pattern.compile(
            "<script[^>]+src=\"(https://a-v2\\.sndcdn\\.com/assets/[^\"]+\\.js)\"");
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile(
            "[,{]client_id:\"([a-zA-Z0-9]{32})\"");
    private static final long CACHE_TTL_MS = 6 * 60 * 60 * 1000L;

    private final HttpClient http;

    // only one fetch happens at a time, all other threads wait on the same future
    private final AtomicReference<CompletableFuture<String>> inflight = new AtomicReference<>(null);

    private volatile String cached;
    private volatile long cacheTime = 0;

    public ClientIdProvider(HttpClient http) {
        this.http = http;
    }

    public void invalidate() {
        cached = null;
        cacheTime = 0;
    }

    public String getClientId() throws IOException, InterruptedException {
        String c = cached;
        if (c != null && System.currentTimeMillis() - cacheTime < CACHE_TTL_MS) {
            return c;
        }

        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> existing = inflight.compareAndExchange(null, mine);

        if (existing != null) {
            // another thread is already fetching
            try {
                return existing.get();
            } catch (Exception e) {
                throw new IOException("client_id fetch failed", e);
            }
        }

        try {
            String id = fetchClientId();
            cached = id;
            cacheTime = System.currentTimeMillis();
            mine.complete(id);
            return id;
        } catch (IOException | InterruptedException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inflight.set(null);
        }
    }

    private String fetchClientId() throws IOException, InterruptedException {
        String html = get(SOUNDCLOUD_HOME);
        Matcher scriptMatcher = SCRIPT_SRC.matcher(html);
        String lastScript = null;
        while (scriptMatcher.find()) {
            lastScript = scriptMatcher.group(1);
        }
        if (lastScript == null) {
            throw new IOException("No SoundCloud asset scripts found in homepage");
        }
        String js = get(lastScript);
        Matcher idMatcher = CLIENT_ID_PATTERN.matcher(js);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        }
        throw new IOException("client_id not found in script: " + lastScript);
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .header("Referer", "https://soundcloud.com/")
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("HTTP " + res.statusCode() + " from: " + url);
        }
        return res.body();
    }
}
