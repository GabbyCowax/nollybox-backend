package com.example.nollybox;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class TmdbVideoResponse {
    @SerializedName("results")
    public List<VideoResult> results;

    public static class VideoResult {
        @SerializedName("key")
        public String key;

        @SerializedName("site")
        public String site;

        @SerializedName("type")
        public String type;
    }
}
