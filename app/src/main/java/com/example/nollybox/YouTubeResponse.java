package com.example.nollybox;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class YouTubeResponse {
    public List<Item> items;
    public String nextPageToken;

    public static class Item {
        public Id id;
        public Snippet snippet;
    }

    public static class Id {
        public String videoId;
    }

    public static class Snippet {
        public String title;
        public String description;
        public String publishedAt;
        public List<String> tags;
        public Thumbnails thumbnails;
    }

    public static class Thumbnails {
        @SerializedName("default")
        public ThumbnailInfo defaultThumbnail;
        public ThumbnailInfo medium;
        public ThumbnailInfo high;
        public ThumbnailInfo standard;
        public ThumbnailInfo maxres;
    }

    public static class ThumbnailInfo {
        public String url;
    }
}
