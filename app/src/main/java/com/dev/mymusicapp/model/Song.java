package com.dev.mymusicapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "songs")
public class Song implements Serializable {

    @PrimaryKey
    @NonNull
    private final String dataPath;

    private final long id;
    private final String title;
    private final String artist;
    private final long duration;
    private final String albumArtUri;

    public Song(long id, @NonNull String title, @NonNull String artist, @NonNull String dataPath, long duration, String albumArtUri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.dataPath = dataPath;
        this.duration = duration;
        this.albumArtUri = albumArtUri;
    }

    public long getId() {
        return id;
    }

    @NonNull public String getTitle() {
        return title;
    }

    @NonNull public String getArtist() {
        return artist;
    }

    @NonNull public String getDataPath() {
        return dataPath;
    }

    public long getDuration() {
        return duration;
    }
    public String getAlbumArtUri() {
        return albumArtUri;
    }
}