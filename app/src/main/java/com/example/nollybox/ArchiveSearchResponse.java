package com.example.nollybox;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ArchiveSearchResponse {
    @SerializedName("response")
    public ResponseData response;

    public static class ResponseData {
        @SerializedName("numFound")
        public int numFound;

        @SerializedName("docs")
        public List<DocItem> docs;
    }

    public static class DocItem {
        @SerializedName("identifier")
        public String identifier;

        @SerializedName("title")
        public String title;
    }
}
