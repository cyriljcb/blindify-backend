package com.blintest.blintest_backend.Mapper;

import com.blintest.blintest_backend.dto.PlaylistDTO;
import com.blintest.blintest_backend.dto.SongDTO;
import com.blintest.blintest_backend.Model.Playlist;
import com.blintest.blintest_backend.Model.Song;

import java.util.List;
import java.util.stream.Collectors;

public class DtoMapper {

    public static PlaylistDTO mapToPlaylistDTO(Playlist playlist) {
        List<SongDTO> songs = playlist.getSongs().stream()
                .map(DtoMapper::mapToSongDTO)
                .collect(Collectors.toList());

        return new PlaylistDTO(playlist.getId(), playlist.getName(),playlist.getBanner() ,songs);
    }

    public static SongDTO mapToSongDTO(Song song) {
        return new SongDTO(song.getId(), song.getName(), song.getArtistNames(), song.getPreviewUrl(), song.getDurationMs());
    }
}
