package com.blintest.blintest_backend.dto;

import java.util.List;

public class PlaylistDTO {
    private String id;         // ID unique de la playlist
    private String name;       // Nom de la playlist
    private String banner;     // URL de la bannière
    private List<SongDTO> songs; // Liste des chansons (peut être vide si non chargée)

    public PlaylistDTO(String id, String name, String banner, List<SongDTO> songs) {
        this.id = id;
        this.name = name;
        this.banner = banner;
        this.songs = songs;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBanner() {
        return banner;
    }

    public List<SongDTO> getSongs() {
        return songs;
    }

    public void setSongs(List<SongDTO> songs) {
        this.songs = songs;
    }
}
