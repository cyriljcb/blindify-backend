package com.blintest.blintest_backend.Service;

import com.blintest.blintest_backend.dto.PlaylistDTO;
import com.blintest.blintest_backend.dto.SongDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {

    private final SpotifyTokenManager tokenManager;
    private final RestTemplate restTemplate;

    @Autowired
    public SpotifyService(SpotifyTokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.restTemplate = new RestTemplate();
    }

    public PlaylistDTO getPlaylist(String playlistId) {
        String playlistInfoUrl = "https://api.spotify.com/v1/playlists/" + playlistId;
        String playlistTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

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
        List<Map<String, Object>> images = (List<Map<String, Object>>) playlistInfo.get("images");
        String playlistBanner = (images != null && !images.isEmpty()) ? (String) images.get(0).get("url") : ""; // Par défaut si aucune image

        // Pagination pour récupérer tous les morceaux
        List<SongDTO> songs = new ArrayList<>();
        int limit = 100; // Nombre maximum de morceaux par requête
        int offset = 0;

        while (true) {
            String paginatedUrl = playlistTracksUrl + "?limit=" + limit + "&offset=" + offset;

            ResponseEntity<Map> tracksResponse = restTemplate.exchange(paginatedUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> responseBody = tracksResponse.getBody();

            if (responseBody == null || !responseBody.containsKey("items")) {
                break;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");
            if (items == null || items.isEmpty()) {
                break; // Aucun autre morceau à récupérer
            }

            for (Map<String, Object> item : items) {
                Map<String, Object> track = (Map<String, Object>) item.get("track");
                if (track == null) continue;

                String id = (String) track.get("id");
                String name = (String) track.get("name");
                String previewUrl = (String) track.get("preview_url");
                Integer durationMs = (Integer) track.get("duration_ms");

                if (id == null || name == null) continue;

                // Récupération de tous les artistes
                List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                List<String> artistNames = new ArrayList<>();
                if (artists != null) {
                    for (Map<String, Object> artist : artists) {
                        artistNames.add((String) artist.get("name"));
                    }
                }

                songs.add(new SongDTO(id, name, artistNames, previewUrl, durationMs != null ? durationMs : 0));
            }

            offset += limit; // Passer à la page suivante
        }

        return new PlaylistDTO(playlistId, playlistName, playlistBanner, songs);
    }

    public List<Map<String, Object>> getUserPlaylists() {
        String url = "https://api.spotify.com/v1/me/playlists";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenManager.getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("items")) {
            return (List<Map<String, Object>>) body.get("items");
        }

        return List.of(); // Retourne une liste vide si aucune playlist n'est trouvée
    }
}
