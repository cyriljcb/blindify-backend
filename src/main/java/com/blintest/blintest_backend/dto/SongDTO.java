package com.blintest.blintest_backend.dto;

public class SongDTO {
    private String id;         // ID unique de la chanson
    private String songName;       // Nom de la chanson
    private String artistName; // Nom de l'artiste
    private String previewUrl; // URL pour l'extrait audio (peut être null)
    private int duration;      // Durée en millisecondes

    public SongDTO(String id, String name, String artistName, String previewUrl, int duration) {
        this.id = id;
        this.songName = name;
        this.artistName = artistName;
        this.previewUrl = previewUrl;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getDurationMs() {
        return duration;
    }
}
