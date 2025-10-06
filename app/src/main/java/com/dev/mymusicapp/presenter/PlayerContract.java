package com.dev.mymusicapp.presenter;

import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.service.MusicService;

import java.util.List;

public interface PlayerContract {

    // Comandos que o Presenter pode dar à View (Activity)
    interface View {
        void showSongDetails(Song song);
        void updateProgress(long currentPosition, long duration);
        void showPlayIcon();
        void showPauseIcon();
        void updateShuffleRepeatUI(boolean isShuffleOn, int repeatMode);
        void showPlaylistsDialog(List<Playlist> playlists);
        void showSongInfoDialog(Song song);
        void toggleDetailsView(boolean show, Song song);
        void showToast(String message);
        void updatePlaylist(List<Song> songs);
        void updateAdapterHighlight(String songPath);
        void scrollToCurrentSong(int position);
        void updateDetailsMenuTitle(boolean areDetailsVisible);
    }

    interface Presenter {
        void attachView(View view);
        void detachView();
        void onServiceConnected(MusicService service);
        void onServiceDisconnected();
        void onPlayPauseClicked();
        void onNextClicked();
        void onPrevClicked();
        void onShuffleClicked();
        void onRepeatClicked();
        void onSeekBarChanged(int progress);
        void onAddToPlaylistClicked();
        void onSongDetailsClicked();
        void onSongClickedInQueue(int position);
        void onPlaylistSelected(Playlist playlist, Song song);
    }
}
