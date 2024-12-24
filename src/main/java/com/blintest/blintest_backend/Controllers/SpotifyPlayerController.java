package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.Service.SpotifyService;

import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spotify/player")
public class SpotifyPlayerController {
    private final SpotifyService spotifyService;
    private final SpotifyTokenManager tokenManager;
    private final RestTemplate restTemplate;

    public SpotifyPlayerController(SpotifyService spotifyService,SpotifyTokenManager tokenManager) {
        this.spotifyService = spotifyService;
        this.tokenManager = tokenManager;
        this.restTemplate = new RestTemplate();
    }
    @PutMapping("/play")
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
                    .body("An error occurred while starting playback: " + e.getMessage());
        }
    }

    @PutMapping("/pause")
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
                    .body("An error occurred while pausing playback: " + e.getMessage());
        }
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<Playlist>> getUserPlaylists() {
        try {
            List<Map<String, Object>> playlistsData = spotifyService.getUserPlaylists();
            List<Playlist> playlists = new ArrayList<>();

            for (Map<String, Object> playlistData : playlistsData) {
                String id = (String) playlistData.get("id");
                String name = (String) playlistData.get("name");

                String banner = null;
                List<Map<String, Object>> images = (List<Map<String, Object>>) playlistData.get("images");
                if (images != null && !images.isEmpty()) {
                    banner = (String) images.get(0).get("url");
                }
                // Ajouter une playlist vide (sans chansons)
                playlists.add(new Playlist(id, name, new ArrayList<>()));
                playlists.get(playlists.size() - 1).setBanner(banner);
            }
            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/playlists/{id}/songs")
    public ResponseEntity<Playlist> getPlaylistWithSongs(@PathVariable String id) {
        try {
            Playlist playlist = spotifyService.getPlaylist(id); // Récupère les détails et les chansons de la playlist
            return ResponseEntity.ok(playlist);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/playlist/{playlistId}/tracks")
    public ResponseEntity<?> getPlaylistTracks(@PathVariable String playlistId) {
        String accessToken = tokenManager.getAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String playlistTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(playlistTracksUrl, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                return ResponseEntity.ok(responseBody);
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body(Map.of("error", "Failed to fetch playlist tracks"));
            }
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getResponseBodyAsString()));
        }
    }
}

