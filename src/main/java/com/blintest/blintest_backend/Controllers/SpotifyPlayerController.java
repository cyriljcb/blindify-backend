package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Service.SpotifyService;
import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import com.blintest.blintest_backend.dto.PlaylistDTO;
import com.blintest.blintest_backend.dto.SongDTO;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/spotify")
public class SpotifyPlayerController {

    private final SpotifyService spotifyService;
    private final SpotifyTokenManager tokenManager;
    private final RestTemplate restTemplate;

    public SpotifyPlayerController(SpotifyService spotifyService, SpotifyTokenManager tokenManager) {
        this.spotifyService = spotifyService;
        this.tokenManager = tokenManager;
        this.restTemplate = new RestTemplate();
    }

    // Démarrer le blind test
    @GetMapping("/blindtest/start")
    public ResponseEntity<?> startBlindTest(@RequestParam("playlistId") String playlistId) {
        try {
            PlaylistDTO playlist = spotifyService.getPlaylist(playlistId);
            List<SongDTO> songs = playlist.getSongs();

            if (songs == null || songs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "No songs found in the playlist."));
            }

            Collections.shuffle(songs); // Mélange des chansons
            return ResponseEntity.ok(Collections.singletonMap("playlist", songs));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error retrieving playlist: " + e.getMessage()));
        }
    }

    // Envoyer des commandes liées au blind test
    @PostMapping("/blindtest/action")
    public ResponseEntity<?> handleBlindTestAction(@RequestBody Map<String, String> action) {
        String command = action.get("command");
        String songId = action.get("songId");

        try {
            switch (command) {
                case "play":
                    playTrack(songId);
                    break;
                case "playAtRefrain":
                    handlePlayAtRefrain(songId);
                    break;
                case "stop":
                    pausePlayback();
                    break;
                case "resume":
                    resumePlayback();
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Collections.singletonMap("error", "Unknown command: " + command));
            }
            return ResponseEntity.ok(Collections.singletonMap("message", "Command executed successfully."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error executing command: " + e.getMessage()));
        }
    }

    // Lecture d'une chanson
    private void playTrack(String trackId) {
        Map<String, Object> playRequest = Map.of("uris", List.of("spotify:track:" + trackId));
        startPlayback(playRequest);
    }

    // Lecture au refrain
    private void handlePlayAtRefrain(String songId) {
        try {
            String songDetailsEndpoint = "https://api.spotify.com/v1/tracks/" + songId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(songDetailsEndpoint, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Integer durationMs = (Integer) response.getBody().get("duration_ms");
                if (durationMs != null && durationMs > 0) {
                    int refrainPositionMs = (int) (durationMs * 0.55);
                    Map<String, Object> playAtRefrainRequest = Map.of(
                            "uris", List.of("spotify:track:" + songId),
                            "position_ms", refrainPositionMs
                    );
                    startPlayback(playAtRefrainRequest);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gestion de la lecture
    @PutMapping("/player/play")
    public ResponseEntity<?> startPlayback(@RequestBody Map<String, Object> playRequest) {
        try {
            String endpoint = "https://api.spotify.com/v1/me/player/play";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(playRequest, headers);
            restTemplate.exchange(endpoint, HttpMethod.PUT, request, Void.class);

            return ResponseEntity.ok("Playback started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error starting playback: " + e.getMessage());
        }
    }

    @PutMapping("/player/pause")
    public ResponseEntity<?> pausePlayback() {
        try {
            String endpoint = "https://api.spotify.com/v1/me/player/pause";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(endpoint, HttpMethod.PUT, request, Void.class);

            return ResponseEntity.ok("Playback paused.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error pausing playback: " + e.getMessage());
        }
    }

    @PutMapping("/player/resume")
    public ResponseEntity<?> resumePlayback() {
        try {
            String endpoint = "https://api.spotify.com/v1/me/player/resume";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(endpoint, HttpMethod.PUT, request, Void.class);

            return ResponseEntity.ok("Playback resumed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resuming playback: " + e.getMessage());
        }
    }

    // Récupération des playlists
    @GetMapping("/playlists")
    public ResponseEntity<List<PlaylistDTO>> getAllPlaylists() {
        try {
            List<Map<String, Object>> playlistsData = spotifyService.getUserPlaylists();

            List<PlaylistDTO> playlists = playlistsData.stream()
                    .map(playlistData -> {
                        String id = (String) playlistData.get("id");
                        String name = (String) playlistData.get("name");

                        String banner = null;
                        List<Map<String, Object>> images = (List<Map<String, Object>>) playlistData.get("images");
                        if (images != null && !images.isEmpty()) {
                            banner = (String) images.get(0).get("url");
                        }

                        return new PlaylistDTO(id, name, banner, List.of());
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
