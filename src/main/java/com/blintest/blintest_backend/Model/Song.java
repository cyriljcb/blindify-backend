package com.blintest.blintest_backend.Model;

public class Song {
    private String id;         // ID unique de la chanson (Spotify ID)
    private String name;       // Nom de la chanson
    private String artist;     // Artiste principal
    private String previewUrl; // URL pour un extrait audio de la chanson (Spotify API)
    private int durationMs;    // Durée de la chanson en millisecondes

    public Song(String id, String name, String artist, String previewUrl, int durationMs) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int estimateRefrainPosition() {
        // Supposons que le refrain commence à 40% de la durée totale
        return (int) (durationMs * 0.4);
    }
    public int getDuration() {
        return durationMs;
    }

    public void setDuration(int duration) {
        this.durationMs = duration;
    }
}

