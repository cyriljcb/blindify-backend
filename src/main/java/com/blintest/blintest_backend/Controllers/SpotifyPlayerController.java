package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Model.Song;
import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spotify/player")
public class SpotifyPlayerController {

    private final SpotifyTokenManager tokenManager;
    private final RestTemplate restTemplate;

    public SpotifyPlayerController(SpotifyTokenManager tokenManager) {
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
}
