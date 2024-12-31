package com.blintest.blintest_backend.dto;

import java.util.List;

public class SongDTO {
    private String id;         // ID unique de la chanson
    private String songName;   // Nom de la chanson
    private List<String> artistNames; // Liste des noms des artistes
    private String previewUrl; // URL pour l'extrait audio (peut être null)
    private int duration;      // Durée en millisecondes

    public SongDTO(String id, String name, List<String> artistNames, String previewUrl, int duration) {
        this.id = id;
        this.songName = name;
        this.artistNames = artistNames;
        this.previewUrl = previewUrl;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getSongName() {
        return songName;
    }

    public List<String> getArtistNames() {
        return artistNames;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public int getDurationMs() {
        return duration;
    }
}
