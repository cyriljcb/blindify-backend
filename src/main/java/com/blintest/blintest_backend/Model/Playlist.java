package com.blintest.blintest_backend.Model;

import java.util.List;

public class Playlist {
    private String id;         // ID unique de la playlist (Spotify ID)
    private String name;       // Nom de la playlist
    private List<Song> songs;  // Liste des chansons de la playlist
    private String banner;     // url pour la bannière

    public Playlist(String id, String name, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public String getBanner(){return banner;}

    public void setBanner(String banner){
        this.banner = banner;
    }

}

