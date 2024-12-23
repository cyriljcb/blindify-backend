package com.blintest.blintest_backend.Service;

import org.springframework.stereotype.Component;

@Component
public class SpotifyTokenManager {

    private String accessToken;

    public String getAccessToken() {
        if (accessToken == null) {
            throw new IllegalStateException("Access token is not set. Please authenticate first.");
        }
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
