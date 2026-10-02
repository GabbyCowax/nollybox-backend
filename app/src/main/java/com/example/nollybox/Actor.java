package com.example.nollybox;

public class Actor {
    private String name;
    private String imageUrl;

    public Actor(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
}