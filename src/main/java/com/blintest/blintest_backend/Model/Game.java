package com.blintest.blintest_backend.Model;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Game {
    private Playlist playlist;
    private int currentSongIndex = 0;
    private Timer timer;

    public Game(Playlist playlist) {
        this.playlist = playlist;
    }

    public void startGame() {
        timer = new Timer();
        playNextSong();
    }

    private void playNextSong() {
        if (currentSongIndex >= playlist.getSongs().size()) {
            System.out.println("Game over!");
            timer.cancel();
            return;
        }

        Song currentSong = playlist.getSongs().get(currentSongIndex);

        // Lecture de l'extrait audio (20 secondes simulé ici)
        System.out.println("Playing: " + currentSong.getName() + " by " + currentSong.getArtist());
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                revealAnswer(currentSong);
            }
        }, 20000); // 20 secondes
    }

    private void revealAnswer(Song song) {
        System.out.println("Reveal: " + song.getName() + " by " + song.getArtist());
        currentSongIndex++;
        playNextSong();
    }
}
