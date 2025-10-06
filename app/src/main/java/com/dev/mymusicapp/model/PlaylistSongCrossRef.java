package com.dev.mymusicapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(primaryKeys = {"playlistId", "dataPath"})
public class PlaylistSongCrossRef {
    public int playlistId;
    @NonNull
    public String dataPath;
}