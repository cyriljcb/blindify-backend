package com.blintest.blintest_backend.Service;

import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.Model.Song;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private final SpotifyTokenManager tokenManager;

    @Autowired
    public SpotifyService(SpotifyTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public Playlist getPlaylist(String playlistId) {
        String playlistInfoUrl = "https://api.spotify.com/v1/playlists/" + playlistId;
        String playlistTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenManager.getAccessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Récupérer les informations de la playlist
        ResponseEntity<Map> playlistInfoResponse = restTemplate.exchange(playlistInfoUrl, HttpMethod.GET, request, Map.class);
        Map<String, Object> playlistInfo = playlistInfoResponse.getBody();

        if (playlistInfo == null || !playlistInfo.containsKey("name")) {
            throw new RuntimeException("Failed to fetch playlist information");
        }

        String playlistName = (String) playlistInfo.get("name");

        // Récupérer les morceaux de la playlist
        ResponseEntity<Map> tracksResponse = restTemplate.exchange(playlistTracksUrl, HttpMethod.GET, request, Map.class);
        Map<String, Object> responseBody = tracksResponse.getBody();

        if (responseBody == null || !responseBody.containsKey("items")) {
            throw new RuntimeException("Failed to fetch playlist tracks");
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");
        List<Song> songs = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Map<String, Object> track = (Map<String, Object>) item.get("track");
            if (track == null) continue;

            String id = (String) track.get("id");
            String name = (String) track.get("name");
            String previewUrl = (String) track.get("preview_url");
            Integer durationMs = (Integer) track.get("duration_ms");

            if (id == null || name == null) continue;

            List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
            String artistName = artists != null && !artists.isEmpty() ? artists.get(0).get("name").toString() : "Unknown Artist";

            songs.add(new Song(id, name, artistName, previewUrl, durationMs != null ? durationMs : 0));
        }

        return new Playlist(playlistId, playlistName, songs);
    }

}
