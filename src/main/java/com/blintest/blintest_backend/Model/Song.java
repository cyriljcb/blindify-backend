package com.blintest.blintest_backend.Model;

import java.util.List;

public class Song {
    private String id;         // ID unique de la chanson (Spotify ID)
    private String name;       // Nom de la chanson
    private List<String> artistNames;
    private String previewUrl; // URL pour un extrait audio de la chanson (Spotify API)
    private int durationMs;    // Durée de la chanson en millisecondes

    public Song(String id, String name, List<String> artistNames, String previewUrl, int durationMs) {
        this.id = id;
        this.name = name;
        this.artistNames = artistNames;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getArtistNames() {
        return artistNames;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getDuration() {
        return durationMs;
    }

    public void setDuration(int duration) {
        this.durationMs = duration;
    }
}

