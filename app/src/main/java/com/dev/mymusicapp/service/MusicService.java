package com.dev.mymusicapp.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.dev.mymusicapp.R;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.view.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private ExoPlayer exoPlayer;
    private List<Song> songList;
    private MediaSession mediaSession;

    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    public class MusicBinder extends Binder {
        public MusicService getService() {

            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayer = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSession.Builder(this, exoPlayer).build();
        createNotificationChannel();

        //Adiciona um listener diretamente no serviço
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                // Quando a música muda, atualizamos a notificação
                updateNotification();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                // Também atualizamos a notificação para mostrar o ícone correto de play/pause
                updateNotification();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            ArrayList<Song> receivedSongs = (ArrayList<Song>) intent.getSerializableExtra("SONG_LIST");
            int position = intent.getIntExtra("CURRENT_POSITION", 0);

            if (receivedSongs != null) {
                this.songList = receivedSongs;
                preparePlaylist(position);
                startForeground(NOTIFICATION_ID, createNotification());
            }
        }
        return START_STICKY;
    }

    private void preparePlaylist(int startPosition) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songList) {
            mediaItems.add(MediaItem.fromUri(song.getDataPath()));
        }
        exoPlayer.setMediaItems(mediaItems, startPosition, 0);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    @OptIn(markerClass = UnstableApi.class)
    private Notification createNotification() {
        int currentSongIndex = exoPlayer.getCurrentMediaItemIndex();
        Song currentSong = songList.get(currentSongIndex);

        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        MediaStyleNotificationHelper.MediaStyle mediaStyle = new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0, 1, 2);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtist())
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setStyle(mediaStyle)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void play() { if (exoPlayer != null) exoPlayer.play(); }
    public void pause() { if (exoPlayer != null) exoPlayer.pause(); }
    public void seekToNext() { if (exoPlayer != null) exoPlayer.seekToNextMediaItem(); }
    public void seekToPrevious() { if (exoPlayer != null) exoPlayer.seekToPreviousMediaItem(); }
    public void seekTo(long position) { if (exoPlayer != null) exoPlayer.seekTo(position); }
    public boolean isPlaying() { return exoPlayer != null && exoPlayer.isPlaying(); }
    public long getCurrentPosition() { return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0; }
    public long getDuration() { return exoPlayer != null ? exoPlayer.getDuration() : 0; }
    public int getCurrentSongIndex() { return exoPlayer != null ? exoPlayer.getCurrentMediaItemIndex() : 0; }
    public void addListener(Player.Listener listener) { if (exoPlayer != null) exoPlayer.addListener(listener); }

    public void toggleShuffleMode() {
        if (exoPlayer != null) {
            exoPlayer.setShuffleModeEnabled(!exoPlayer.getShuffleModeEnabled());
        }
    }

    public void seekToSongInPlaylist(int position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position, 0); // Salta para a música na posição 'position'
            exoPlayer.play(); // Garante que a reprodução começa
        }
    }

    public void removeListener(Player.Listener listener) {
        if (exoPlayer != null) {
            exoPlayer.removeListener(listener);
        }
    }

    public List<Song> getCurrentSongList() {
        return songList;
    }

    public Song getCurrentPlayingSong() {
        if (exoPlayer != null && songList != null && !songList.isEmpty()) {
            return songList.get(exoPlayer.getCurrentMediaItemIndex());
        }
        return null;
    }

    public boolean isSongPlaying(String dataPath) {
        Song currentSong = getCurrentPlayingSong();
        return currentSong != null && currentSong.getDataPath().equals(dataPath);
    }

    public void toggleRepeatMode() {
        if (exoPlayer != null) {
            int currentMode = exoPlayer.getRepeatMode();
            if (currentMode == Player.REPEAT_MODE_OFF) {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE); // Repetir uma
            } else if (currentMode == Player.REPEAT_MODE_ONE) {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL); // Repetir todas
            } else {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); // Desligar
            }
        }
    }

    // Métodos para a UI saber o estado atual
    public boolean isShuffleModeEnabled() {
        return exoPlayer != null && exoPlayer.getShuffleModeEnabled();
    }

    public int getRepeatMode() {
        return exoPlayer != null ? exoPlayer.getRepeatMode() : Player.REPEAT_MODE_OFF;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}