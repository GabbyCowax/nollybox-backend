package com.example.nollybox;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ArchiveMetadataResponse {
    @SerializedName("files")
    public List<ArchiveFile> files;

    public static class ArchiveFile {
        @SerializedName("name")
        public String name;

        @SerializedName("format")
        public String format;

        @SerializedName("size")
        public String size;
    }
}
