package com.blintest.blintest_backend.dto;

public class SongDTO {
    private String id;         // ID unique de la chanson
    private String name;       // Nom de la chanson
    private String artistName; // Nom de l'artiste
    private String previewUrl; // URL pour l'extrait audio (peut être null)
    private int duration;      // Durée en millisecondes

    public SongDTO(String id, String name, String artistName, String previewUrl, int duration) {
        this.id = id;
        this.name = name;
        this.artistName = artistName;
        this.previewUrl = previewUrl;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getDuration() {
        return duration;
    }
}
