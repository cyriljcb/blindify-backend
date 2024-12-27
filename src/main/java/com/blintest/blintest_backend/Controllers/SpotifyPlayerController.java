package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.dto.PlaylistDTO;
import com.blintest.blintest_backend.Mapper.DtoMapper;
import com.blintest.blintest_backend.Service.SpotifyService;
import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
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
                    .body("An error occurred while starting playback: " + e.getMessage());
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
                    .body("An error occurred while pausing playback: " + e.getMessage());
        }
    }

    @GetMapping("/playlists")
    public ResponseEntity<List<PlaylistDTO>> getUserPlaylists() {
        try {
            // Récupérer les playlists depuis le service Spotify
            List<Map<String, Object>> playlistsData = spotifyService.getUserPlaylists();

            // Transformer les données en DTO
            List<PlaylistDTO> playlists = playlistsData.stream()
                    .map(playlistData -> {
                        String id = (String) playlistData.get("id");
                        String name = (String) playlistData.get("name");
                        String banner = null;
                        List<Map<String, Object>> images = (List<Map<String, Object>>) playlistData.get("images");
                        if (images != null && !images.isEmpty()) {
                            banner = (String) images.get(0).get("url");
                        }
                        return new PlaylistDTO(id, name, banner, List.of()); // Playlist sans chansons
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(playlists);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/playlists/{id}/songs")
    public ResponseEntity<PlaylistDTO> getPlaylistWithSongs(@PathVariable String id) {
        try {
            // Récupérer la playlist complète avec chansons depuis le service
            Playlist playlist = spotifyService.getPlaylist(id);

            // Mapper la playlist en DTO
            PlaylistDTO playlistDTO = DtoMapper.mapToPlaylistDTO(playlist);

            return ResponseEntity.ok(playlistDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/player/current")
    public ResponseEntity<?> getCurrentTrack() {
        try {
            String endpoint = "https://api.spotify.com/v1/me/player/currently-playing";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(endpoint, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> currentTrackData = response.getBody();

                // Extraire les informations
                Map<String, Object> item = (Map<String, Object>) currentTrackData.get("item");
                String songName = (String) item.get("name");
                List<Map<String, Object>> artists = (List<Map<String, Object>>) item.get("artists");
                String artistName = artists.stream()
                        .map(artist -> (String) artist.get("name"))
                        .collect(Collectors.joining(", "));

                // Créer une réponse avec les champs nécessaires
                Map<String, Object> trackDetails = Map.of(
                        "songName", songName,
                        "artistName", artistName
                );

                return ResponseEntity.ok(trackDetails);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No track currently playing.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching the current track: " + e.getMessage());
        }
    }


}
