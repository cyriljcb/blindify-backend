package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.Model.Song;
import com.blintest.blintest_backend.Service.SpotifyService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/blindtest")
public class BlindTestController {
    private final SpotifyService spotifyService;

    public BlindTestController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/start")
    public String startGame(@RequestParam("playlistId") String playlistId) {
        Playlist playlist = spotifyService.getPlaylist(playlistId);

        // Logique pour démarrer le jeu avec la playlist
        List<Song> songs = playlist.getSongs();

        // Exemple : Afficher les noms des chansons
        for (Song song : songs) {
            System.out.println("Playing: " + song.getName() + " by " + song.getArtist());
        }

        return "Blind test started for playlist: " + playlist.getName();
    }
}
