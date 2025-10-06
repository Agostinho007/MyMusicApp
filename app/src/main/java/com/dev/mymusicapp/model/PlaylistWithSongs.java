package com.dev.mymusicapp.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class PlaylistWithSongs {
    @Embedded public Playlist playlist;
    @Relation(
            parentColumn = "playlistId",
            entityColumn = "dataPath",
            associateBy = @Junction(PlaylistSongCrossRef.class)
    )
    public List<Song> songs;
}