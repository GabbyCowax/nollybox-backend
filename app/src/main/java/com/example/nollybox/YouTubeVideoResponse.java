package com.example.nollybox;

import java.util.List;

public class YouTubeVideoResponse {
    public List<Item> items;

    public static class Item {
        public String id; // In the 'videos' endpoint, ID is a direct string
        public YouTubeResponse.Snippet snippet;
    }
}