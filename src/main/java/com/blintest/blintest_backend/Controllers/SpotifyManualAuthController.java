package com.blintest.blintest_backend.Controllers;

import com.blintest.blintest_backend.Service.SpotifyTokenManager;
import com.blintest.blintest_backend.Config.SpotifyAuthConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RestController
public class SpotifyManualAuthController {

    private final SpotifyAuthConfig spotifyAuthConfig;
    private final SpotifyTokenManager tokenManager;

    public SpotifyManualAuthController(SpotifyAuthConfig spotifyAuthConfig, SpotifyTokenManager tokenManager) {
        this.spotifyAuthConfig = spotifyAuthConfig;
        this.tokenManager = tokenManager;
    }

    @GetMapping("/spotify/login")
    public ResponseEntity<Void> login() {
        String authorizationUrl = UriComponentsBuilder.fromHttpUrl("https://accounts.spotify.com/authorize")
                .queryParam("client_id", spotifyAuthConfig.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", spotifyAuthConfig.getRedirectUri())
                .queryParam("scope", "user-read-private playlist-read-private user-modify-playback-state user-read-playback-state")
                .toUriString();
        System.out.println("Endpoint /spotify/login reached");
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, authorizationUrl).build();
    }

    @GetMapping("/spotify/callback")
    public void callback(@RequestParam("code") String code, HttpServletResponse response) {
        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl = "https://accounts.spotify.com/api/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(spotifyAuthConfig.getClientId(), spotifyAuthConfig.getClientSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", spotifyAuthConfig.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = responseEntity.getBody();
                String accessToken = (String) responseBody.get("access_token");

                // Stocker l'access token dans SpotifyTokenManager
                tokenManager.setAccessToken(accessToken);

                // Rediriger vers Angular après un succès
                response.sendRedirect("http://localhost:4200/home"); // Redirige vers la route "/home" ou autre
            } else {
                // Rediriger vers une page d'erreur en cas d'échec
                response.sendRedirect("http://localhost:4200/error?message=" + responseEntity.getBody());
            }
        } catch (HttpClientErrorException e) {
            try {
                // Rediriger vers une page d'erreur en cas d'exception
                response.sendRedirect("http://localhost:4200/error?message=" + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
