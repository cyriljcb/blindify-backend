package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.Model.Song;
import com.blintest.blintest_backend.Service.SpotifyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blindtest")
public class BlindTestController {
    private final SpotifyService spotifyService;
    private final RestTemplate restTemplate;

    public BlindTestController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
        this.restTemplate = new RestTemplate();
    }
    @GetMapping("/sequence")
    public ResponseEntity<?> startSequenceWithSpotify(@RequestParam("playlistId") String playlistId) {
        try {
            // Récupérer les chansons de la playlist
            Playlist playlist = spotifyService.getPlaylist(playlistId);
            List<Song> songs = playlist.getSongs();

            if (songs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No songs found in the playlist.");
            }

            // Mélanger les chansons pour lecture aléatoire
            Collections.shuffle(songs);

            for (Song song : songs) {
                // Lecture de l'introduction
                String playUrl = "http://localhost:8080/spotify/player/play";
                Map<String, Object> playRequest = new HashMap<>();
                playRequest.put("uris", List.of("spotify:track:" + song.getId()));

                restTemplate.put(playUrl, playRequest);

                // Lecture pendant 20 secondes
                System.out.println("Playing: " + song.getName() + " by " + song.getArtist());
                Thread.sleep(20000);

                // Passer au refrain estimé
                int estimatedRefrain = song.estimateRefrainPosition();
                Map<String, Object> playAtRefrainRequest = new HashMap<>();
                playAtRefrainRequest.put("uris", List.of("spotify:track:" + song.getId()));
                playAtRefrainRequest.put("position_ms", estimatedRefrain);

                restTemplate.put(playUrl, playAtRefrainRequest);

                System.out.println("Playing estimated refrain for: " + song.getName() + " at " + estimatedRefrain + "ms");
                Thread.sleep(10000); // Lecture du refrain

                // Pause pour révélation
                String pauseUrl = "http://localhost:8080/spotify/player/pause";
                restTemplate.put(pauseUrl, null);
                System.out.println("Revealing: " + song.getName() + " by " + song.getArtist());
                Thread.sleep(10000);
            }

            return ResponseEntity.ok("Blind test sequence completed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during the blind test sequence: " + e.getMessage());
        }
    }

}
