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
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/blindtest")
public class BlindTestController {
    private final SpotifyService spotifyService;
    private final RestTemplate restTemplate;
    private Thread blindTestThread;
    private final AtomicBoolean isPaused;
    private final AtomicBoolean isStopped;

    public BlindTestController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
        this.restTemplate = new RestTemplate();
        this.isPaused = new AtomicBoolean(false);
        this.isStopped = new AtomicBoolean(false);
    }

    @GetMapping("/sequence")
    public ResponseEntity<?> startSequenceWithSpotify(@RequestParam("playlistId") String playlistId) {
        try {
            Playlist playlist = spotifyService.getPlaylist(playlistId);
            List<Song> songs = playlist.getSongs();

            if (songs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No songs found in the playlist.");
            }

            Collections.shuffle(songs);

            blindTestThread = new Thread(() -> {
                try {
                    for (Song song : songs) {
                        if (isStopped.get()) break;

                        synchronized (isPaused) {
                            while (isPaused.get()) {
                                isPaused.wait();
                            }
                        }

                        String playUrl = "http://localhost:8080/spotify/player/play";
                        Map<String, Object> playRequest = new HashMap<>();
                        playRequest.put("uris", List.of("spotify:track:" + song.getId()));

                        restTemplate.put(playUrl, playRequest);

                        System.out.println("Playing: " + song.getName() + " by " + song.getArtist());
                        Thread.sleep(20000);

                        int estimatedRefrain = song.estimateRefrainPosition();
                        Map<String, Object> playAtRefrainRequest = new HashMap<>();
                        playAtRefrainRequest.put("uris", List.of("spotify:track:" + song.getId()));
                        playAtRefrainRequest.put("position_ms", estimatedRefrain);

                        String pauseUrl = "http://localhost:8080/spotify/player/pause";
                        restTemplate.put(pauseUrl, null);
                        Thread.sleep(1000);

                        restTemplate.put(playUrl, playAtRefrainRequest);
                        System.out.println("Playing estimated refrain for: " + song.getName());
                        Thread.sleep(15000);

                        restTemplate.put(pauseUrl, null);
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            isStopped.set(false);
            blindTestThread.start();

            return ResponseEntity.ok("Blind test started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during the blind test sequence: " + e.getMessage());
        }
    }

    //met en pause le blindtest mais fini quand meme la musique en cours
    @PutMapping("/pause")
    public ResponseEntity<?> pauseBlindTest() {
        if (blindTestThread == null || !blindTestThread.isAlive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No blind test in progress to pause.");
        }

        isPaused.set(true);
        return ResponseEntity.ok("Blind test paused.");
    }

    @PutMapping("/resume")
    public ResponseEntity<?> resumeBlindTest() {
        if (blindTestThread == null || !blindTestThread.isAlive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No blind test in progress to resume.");
        }

        synchronized (isPaused) {
            isPaused.set(false);
            isPaused.notifyAll();
        }

        return ResponseEntity.ok("Blind test resumed.");
    }

    @PutMapping("/stop")
    public ResponseEntity<?> stopBlindTest() {
        if (blindTestThread == null || !blindTestThread.isAlive()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No blind test in progress to stop.");
        }

        isStopped.set(true);
        isPaused.set(false); // Ensure no deadlock if paused
        synchronized (isPaused) {
            isPaused.notifyAll();
        }

        System.out.println("Blind test stopped.");
        return ResponseEntity.ok("Blind test stopped.");

    }
}
