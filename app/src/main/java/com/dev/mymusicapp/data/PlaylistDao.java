package com.dev.mymusicapp.data;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.PlaylistSongCrossRef;
import com.dev.mymusicapp.model.PlaylistWithSongs;
import com.dev.mymusicapp.model.Song;

import java.util.List;

@Dao
public interface PlaylistDao {

    // --- Operações de Playlist ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylist(Playlist playlist);

    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylists();

    // --- Operações de Músicas ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSong(Song song);

    // --- Operações de Relação ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPlaylistSongCrossRef(PlaylistSongCrossRef crossRef);

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    PlaylistWithSongs getPlaylistWithSongs(int playlistId);

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND dataPath = :dataPath")
    void deleteSongFromPlaylist(int playlistId, @NonNull String dataPath);

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    void deletePlaylistById(int playlistId);

    // Apaga todas as referências de músicas para uma dada playlist
    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    void deleteCrossRefsByPlaylistId(int playlistId);

    @Query("SELECT COUNT(*) FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND dataPath = :dataPath")
    int countSongInPlaylist(int playlistId, @NonNull String dataPath);
}