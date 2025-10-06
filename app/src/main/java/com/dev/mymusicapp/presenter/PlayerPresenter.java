package com.dev.mymusicapp.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;

import com.dev.mymusicapp.data.AppDatabase;
import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.PlaylistSongCrossRef;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.service.MusicService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerPresenter implements PlayerContract.Presenter {

    private PlayerContract.View view;
    private MusicService musicService;
    private boolean isBound = false;
    private AppDatabase db;
    private ExecutorService executorService;
    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private boolean isDetailsViewVisible = false;

    public PlayerPresenter(Context context) {
        this.db = AppDatabase.getDatabase(context.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void attachView(PlayerContract.View view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        this.view = null; // Evita memory leaks
        handler.removeCallbacks(updateSeekBarRunnable);
        if (musicService != null) {

        }
    }

    @Override
    public void onServiceConnected(MusicService service) {
        musicService = service;
        isBound = true;

        // Configura o listener para eventos do player
        musicService.addListener(playerListener);

        // Inicia a UI
        updateFullUI();

        // Inicia o runnable da seekbar
        initializeSeekBarRunnable();
        handler.post(updateSeekBarRunnable);
    }

    @Override
    public void onServiceDisconnected() {
        isBound = false;
        musicService = null;
    }

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (view == null) return;

            if (isPlaying) {
                view.showPauseIcon();
                handler.post(updateSeekBarRunnable);
            } else {
                view.showPlayIcon();
                handler.removeCallbacks(updateSeekBarRunnable);
            }
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateFullUI();
        }
    };

    private void initializeSeekBarRunnable() {
        updateSeekBarRunnable = () -> {
            if (isBound && musicService.isPlaying() && view != null) {
                view.updateProgress(musicService.getCurrentPosition(), musicService.getDuration());
                handler.postDelayed(updateSeekBarRunnable, 1000);
            }
        };
    }

    @Override
    public void onPlayPauseClicked() {
        if (isBound) {
            if (musicService.isPlaying()) musicService.pause();
            else musicService.play();
        }
    }
    @Override public void onNextClicked() { if (isBound) musicService.seekToNext(); }
    @Override public void onPrevClicked() { if (isBound) musicService.seekToPrevious(); }
    @Override public void onShuffleClicked() {
        if (isBound) {
            musicService.toggleShuffleMode();
            view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
        }
    }
    @Override public void onRepeatClicked() {
        if (isBound) {
            musicService.toggleRepeatMode();
            view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
        }
    }
    @Override public void onSeekBarChanged(int progress) { if (isBound) musicService.seekTo(progress); }
    @Override public void onSongClickedInQueue(int position) { if (isBound) musicService.seekToSongInPlaylist(position); }

    @Override
    public void onAddToPlaylistClicked() {
        executorService.execute(() -> {
            List<Playlist> playlists = db.playlistDao().getAllPlaylists();
            if (view != null) {
                new Handler(Looper.getMainLooper()).post(() -> view.showPlaylistsDialog(playlists));
            }
        });
    }

    private void updateFullUI() {
        if (isBound && view != null) {
            int currentPosition = musicService.getCurrentSongIndex();
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                view.showSongDetails(currentSong);
                view.updateProgress(musicService.getCurrentPosition(), currentSong.getDuration());
                view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
                view.updatePlaylist(musicService.getCurrentSongList());

                // NOVO: Linha dedicada para atualizar o destaque
                view.updateAdapterHighlight(currentSong.getDataPath());

                view.scrollToCurrentSong(currentPosition);

                if (musicService.isPlaying()) {
                    view.showPauseIcon();
                } else {
                    view.showPlayIcon();
                }

                view.updateDetailsMenuTitle(isDetailsViewVisible);
            }
        }
    }

    @Override
    public void onSongDetailsClicked() {
        if (isBound && view != null) {
            isDetailsViewVisible = !isDetailsViewVisible;
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                view.toggleDetailsView(isDetailsViewVisible, currentSong);
                view.updateDetailsMenuTitle(isDetailsViewVisible);
            }
        }
    }

    @Override
    public void onPlaylistSelected(Playlist playlist, Song songToIgnore) {
        if (isBound) {
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong == null) return;

            executorService.execute(() -> {
                // NOVO: Verifica se a música já existe
                int count = db.playlistDao().countSongInPlaylist(playlist.playlistId, currentSong.getDataPath());

                if (count > 0) {
                    // Se já existe, mostra um aviso
                    if (view != null) {
                        new Handler(Looper.getMainLooper()).post(() -> view.showToast("A música já está nesta playlist."));
                    }
                } else {
                    // Se não existe, adiciona
                    db.playlistDao().insertSong(currentSong);
                    PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
                    crossRef.playlistId = playlist.playlistId;
                    crossRef.dataPath = currentSong.getDataPath();
                    db.playlistDao().insertPlaylistSongCrossRef(crossRef);
                    if (view != null) {
                        new Handler(Looper.getMainLooper()).post(() -> view.showToast("Música adicionada a '" + playlist.name + "'"));
                    }
                }
            });
        }
    }
}
