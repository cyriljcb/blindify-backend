package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
public class SpotifyManualAuthController {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    private final SpotifyTokenManager tokenManager;

    public SpotifyManualAuthController(SpotifyTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @PostConstruct
    public void logEnvironmentVariables() {
        System.out.println("SPOTIFY_CLIENT_ID: " + clientId);
        System.out.println("SPOTIFY_CLIENT_SECRET: " + clientSecret);
        System.out.println("SPOTIFY_REDIRECT_URI: " + redirectUri);
    }

    @GetMapping("/spotify/login")
    public ResponseEntity<Void> login() {
        String authorizationUrl = UriComponentsBuilder.fromHttpUrl("https://accounts.spotify.com/authorize")
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "user-read-private playlist-read-private user-modify-playback-state user-read-playback-state")
                .toUriString();
        System.out.println("Endpoint /spotify/login reached");
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, authorizationUrl).build();
    }

    @GetMapping("/spotify/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) {
        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret); // Encodage client_id:client_secret

        // Construction du corps de la requête
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");

                // Stocker l'access token dans SpotifyTokenManager
                tokenManager.setAccessToken(accessToken);

                return ResponseEntity.ok("Access token retrieved and stored successfully.");
            } else {
                return ResponseEntity.status(response.getStatusCode())
                        .body("Error retrieving access token: " + response.getBody());
            }
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body("Error: " + e.getResponseBodyAsString());
        }
    }

    @GetMapping("/spotify/playlists")
    public ResponseEntity<Map> getPlaylists() {
        String accessToken = tokenManager.getAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String playlistsUrl = "https://api.spotify.com/v1/me/playlists";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(playlistsUrl, HttpMethod.GET, request, Map.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/spotify/playlist/{playlistId}/tracks")
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

    @GetMapping("/spotify/playlist/{playlistId}/first-track")
    public ResponseEntity<?> getFirstTrack(@PathVariable String playlistId) {
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
                List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");

                if (items != null && !items.isEmpty()) {
                    Map<String, Object> firstTrack = (Map<String, Object>) items.get(0).get("track");
                    String trackName = (String) firstTrack.get("name");
                    List<Map<String, Object>> artists = (List<Map<String, Object>>) firstTrack.get("artists");
                    String mainArtist = artists.get(0).get("name").toString();
                    return ResponseEntity.ok(Map.of("track", trackName, "artist", mainArtist));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No tracks found in playlist"));
                }
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
