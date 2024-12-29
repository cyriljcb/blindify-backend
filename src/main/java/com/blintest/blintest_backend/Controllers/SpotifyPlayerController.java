package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Service.SpotifyService;
import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import com.blintest.blintest_backend.dto.PlaylistDTO;
import com.blintest.blintest_backend.dto.SongDTO;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);

    public SpotifyPlayerController(SpotifyService spotifyService, SpotifyTokenManager tokenManager, SimpMessagingTemplate messagingTemplate) {
        this.spotifyService = spotifyService;
        this.tokenManager = tokenManager;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = new RestTemplate();
    }

    // Blind test - Démarrer la séquence
    @GetMapping("/blindtest/start")
    public ResponseEntity<Map<String, String>> startBlindTest(@RequestParam("playlistId") String playlistId) {
        try {
            System.out.println("Démarrage du blind test pour la playlist : " + playlistId);

            PlaylistDTO playlist = spotifyService.getPlaylist(playlistId);
            System.out.println("Playlist récupérée : " + playlist);

            List<SongDTO> songs = playlist.getSongs();
            if (songs == null || songs.isEmpty()) {
                System.out.println("Aucune chanson trouvée dans la playlist.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "No songs found in the playlist."));
            }

            Collections.shuffle(songs);
            System.out.println("Chansons mélangées : " + songs);

            messagingTemplate.convertAndSend("/topic/playlist", songs);
            System.out.println("Chansons envoyées via WebSocket.");

            // Renvoyer une réponse JSON au frontend
            Map<String, String> response = new HashMap<>();
            response.put("message", "Blind test playlist sent to the frontend.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error retrieving playlist: " + e.getMessage()));
        }
    }

    // Blind test - Pause
    @PutMapping("/blindtest/pause")
    public ResponseEntity<?> pauseBlindTest() {
        isPaused.set(true);
        return ResponseEntity.ok("Blind test paused.");
    }

    // Blind test - Reprise
    @PutMapping("/blindtest/resume")
    public ResponseEntity<?> resumeBlindTest() {
        synchronized (isPaused) {
            isPaused.set(false);
            isPaused.notifyAll();
        }
        return ResponseEntity.ok("Blind test resumed.");
    }

    // Blind test - Stop
    @PutMapping("/blindtest/stop")
    public ResponseEntity<?> stopBlindTest() {
        isStopped.set(true);
        return ResponseEntity.ok("Blind test stopped.");
    }

    private void playTrack(String trackId) {
        Map<String, Object> playRequest = Map.of("uris", List.of("spotify:track:" + trackId));
        startPlayback(playRequest);
    }

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
                        Map<String, Object> tracksData = (Map<String, Object>) playlistData.get("tracks");
                        int totalTracks = tracksData != null ? (int) tracksData.get("total") : 0;

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
    @MessageMapping("/blindtest/action")
    public void handleBlindTestAction(Map<String, String> action) {
        String command = action.get("command");
        String songId = action.get("songId");

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
                System.out.println("Commande inconnue : " + command);
        }
    }

    private void handlePlayAtRefrain(String songId) {
        try {
            // Appeler l'API Spotify pour obtenir les détails de la chanson
            String songDetailsEndpoint = "https://api.spotify.com/v1/tracks/" + songId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(songDetailsEndpoint, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extraire la durée de la chanson en millisecondes
                Integer durationMs = (Integer) response.getBody().get("duration_ms");

                if (durationMs != null && durationMs > 0) {
                    // Calculer la position du refrain à 70% de la durée totale
                    int refrainPositionMs = (int) (durationMs * 0.55);

                    // Préparer et envoyer la requête de lecture
                    Map<String, Object> playAtRefrainRequest = Map.of(
                            "uris", List.of("spotify:track:" + songId),
                            "position_ms", refrainPositionMs
                    );

                    startPlayback(playAtRefrainRequest);
                    System.out.println("Lecture du refrain à " + refrainPositionMs + " ms pour la chanson " + songId);
                } else {
                    System.out.println("Durée de la chanson non valide ou non trouvée.");
                }
            } else {
                System.out.println("Impossible de récupérer les détails de la chanson : " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur lors de la récupération des détails de la chanson : " + e.getMessage());
        }
    }


}
