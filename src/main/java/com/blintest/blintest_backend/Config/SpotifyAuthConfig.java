package com.blintest.blintest_backend.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpotifyAuthConfig {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String redirectUri;

    @PostConstruct
    public void logEnvironmentVariables() {
        System.out.println("SPOTIFY_CLIENT_ID: " + clientId);
        System.out.println("SPOTIFY_CLIENT_SECRET: " + clientSecret);
        System.out.println("SPOTIFY_REDIRECT_URI: " + redirectUri);
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
