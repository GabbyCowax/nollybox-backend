package com.example.nollybox;

import java.util.List;

public class PipedResponse {
    public List<Stream> videoStreams;

    public static class Stream {
        public String url;
        public String format;
        public String quality;
        public String mimeType;
        public boolean videoOnly;
    }
}
