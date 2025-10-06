package com.dev.mymusicapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.PlaylistSongCrossRef;
import com.dev.mymusicapp.model.Song;

@Database(entities = {Playlist.class, Song.class, PlaylistSongCrossRef.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PlaylistDao playlistDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "music_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}