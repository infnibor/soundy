package com.waris4ly.soundy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SoundCloudHttpClient {

    private static final String API_BASE = "https://api-v2.soundcloud.com";
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 500;

    private final HttpClient http;
    private final ClientIdProvider clientIdProvider;

    public SoundCloudHttpClient(HttpClient http, ClientIdProvider clientIdProvider) {
        this.http = http;
        this.clientIdProvider = clientIdProvider;
    }

    public String get(String path) throws IOException, InterruptedException {
        String sep = path.contains("?") ? "&" : "?";
        return executeWithRetry(API_BASE + path + sep + "client_id=");
    }

    public String getAbsolute(String url) throws IOException, InterruptedException {
        String sep = url.contains("?") ? "&" : "?";
        return executeWithRetry(url + sep + "client_id=");
    }

    private String executeWithRetry(String urlBase) throws IOException, InterruptedException {
        IOException lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                Thread.sleep(BACKOFF_BASE_MS * (1L << (attempt - 1)));
            }

            String url = urlBase + clientIdProvider.getClientId();
            HttpResponse<String> res = send(url);

            if (res.statusCode() == 200) {
                return res.body();
            }

            if (res.statusCode() == 401 || res.statusCode() == 403) {
                // client_id probably rotated
                clientIdProvider.invalidate();
                continue;
            }

            if (res.statusCode() == 429 || res.statusCode() >= 500) {
                lastException = new IOException("SoundCloud returned " + res.statusCode() + " for: " + url);
                continue;
            }

            throw new IOException("SoundCloud API HTTP " + res.statusCode() + " for: " + url);
        }

        throw lastException != null ? lastException
                : new IOException("SoundCloud request failed after " + MAX_RETRIES + " retries");
    }

    private HttpResponse<String> send(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, */*;q=0.8")
                .header("Referer", "https://soundcloud.com/")
                .header("Origin", "https://soundcloud.com")
                .GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
