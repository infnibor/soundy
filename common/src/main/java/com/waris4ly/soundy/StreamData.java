package com.waris4ly.soundy;

public class StreamData {

    public enum Protocol { PROGRESSIVE, HLS }

    private final String url;
    private final Protocol protocol;

    public StreamData(String url, Protocol protocol) {
        this.url = url;
        this.protocol = protocol;
    }

    public String getUrl() { return url; }
    public Protocol getProtocol() { return protocol; }
}
