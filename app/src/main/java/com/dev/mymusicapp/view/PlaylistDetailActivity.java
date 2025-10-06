package com.dev.mymusicapp.view;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dev.mymusicapp.R;
import com.dev.mymusicapp.adapter.SongAdapter;
import com.dev.mymusicapp.data.AppDatabase;
import com.dev.mymusicapp.databinding.ActivityPlaylistDetailBinding;
import com.dev.mymusicapp.model.PlaylistSongCrossRef;
import com.dev.mymusicapp.model.PlaylistWithSongs;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.service.MusicService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnSongLongClickListener {

    private ActivityPlaylistDetailBinding binding;
    private AppDatabase db;
    private SongAdapter songAdapter;
    private ExecutorService executorService;
    private PlaylistWithSongs currentPlaylist;
    private int playlistId = -1;
    private MusicService musicService;
    private boolean isBound = false;
    private Player.Listener playerListener;

    private final ActivityResultLauncher<Intent> selectSongsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    List<Song> selectedSongs = (List<Song>) result.getData().getSerializableExtra("SELECTED_SONGS");
                    if (selectedSongs != null && !selectedSongs.isEmpty()) {
                        addSongsToPlaylist(selectedSongs);
                    }
                }
            });

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Assim que conectamos, atualizamos o destaque na lista
            updateHighlight();

            // Adiciona o listener para futuras mudanças
            musicService.addListener(playerListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaylistDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarPlaylistDetail);
        binding.toolbarPlaylistDetail.setNavigationOnClickListener(v -> onBackPressed());

        binding.fabAddSongsToPlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectSongsActivity.class);
            selectSongsLauncher.launch(intent);
        });

        playlistId = getIntent().getIntExtra("PLAYLIST_ID", -1); // Guarda o ID
        String playlistName = getIntent().getStringExtra("PLAYLIST_NAME");

        getSupportActionBar().setTitle(playlistName);

        db = AppDatabase.getDatabase(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();

        setupRecyclerView();

        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateHighlight();
            }
        };

        if (playlistId != -1) {
            loadSongsFromPlaylist(playlistId);
        }
    }

    //Ciclo de vida para conectar/desconectar
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            if (musicService != null) {
                musicService.removeListener(playerListener);
            }
            unbindService(connection);
            isBound = false;
        }
    }

    private void addSongsToPlaylist(List<Song> songsToAdd) {
        executorService.execute(() -> {
            int newSongsCount = 0;
            for (Song song : songsToAdd) {
                // Verifica se a música já existe para evitar duplicados
                int count = db.playlistDao().countSongInPlaylist(playlistId, song.getDataPath());
                if (count == 0) {
                    db.playlistDao().insertSong(song);
                    PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
                    crossRef.playlistId = playlistId;
                    crossRef.dataPath = song.getDataPath();
                    db.playlistDao().insertPlaylistSongCrossRef(crossRef);
                    newSongsCount++;
                }
            }
            int finalNewSongsCount = newSongsCount;
            runOnUiThread(() -> {
                loadSongsFromPlaylist(playlistId);
                Toast.makeText(this, finalNewSongsCount + " novas músicas adicionadas.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateHighlight() {
        if (isBound && musicService != null && songAdapter != null) {
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                songAdapter.setCurrentPlayingSong(currentSong.getDataPath());
            }
        }
    }

    private void setupRecyclerView() {
        //Passa 'this' para ambos os listeners
        songAdapter = new SongAdapter(this, this);
        binding.recyclerViewSongsInPlaylist.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSongsInPlaylist.setAdapter(songAdapter);
    }

    private void loadSongsFromPlaylist(int playlistId) {
        executorService.execute(() -> {
            currentPlaylist = db.playlistDao().getPlaylistWithSongs(playlistId);
            runOnUiThread(() -> {
                if (currentPlaylist != null) {
                    songAdapter.setSongs(currentPlaylist.songs);
                    // Depois de carregar as músicas, verifica se alguma precisa de destaque
                    updateHighlight();
                }
            });
        });
    }

    @Override
    public void onSongClick(Song song, View albumArtView) {
        Intent intent = new Intent(this, PlayerActivity.class);

        if (isBound && musicService != null && musicService.isSongPlaying(song.getDataPath())) {
            // Lógica para não reiniciar
        } else {
            if (currentPlaylist != null && !currentPlaylist.songs.isEmpty()) {
                int position = currentPlaylist.songs.indexOf(song);
                intent.putExtra("SONG_LIST", (ArrayList<Song>) currentPlaylist.songs);
                intent.putExtra("CURRENT_POSITION", position);
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, albumArtView, ViewCompat.getTransitionName(albumArtView));

        startActivity(intent, options.toBundle());
    }

    @Override
    public void onSongLongClick(Song song) {
        // ATUALIZADO: Usamos o MaterialAlertDialogBuilder com o nosso estilo personalizado
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Destructive)
                .setTitle("Remover Música")
                .setMessage("Tem a certeza que quer remover '" + song.getTitle() + "' desta playlist?")
                .setIcon(R.drawable.ic_delete_warning) // Reutilizamos o mesmo ícone
                .setNegativeButton("Não", null)
                .setPositiveButton("Sim, remover", (dialog, which) -> {
                    deleteSongFromPlaylist(song);
                })
                .show();
    }

    private void deleteSongFromPlaylist(Song song) {
        executorService.execute(() -> {
            db.playlistDao().deleteSongFromPlaylist(playlistId, song.getDataPath());
            runOnUiThread(() -> {
                loadSongsFromPlaylist(playlistId);
                Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadSongsFromPlaylistAfterUpdate() {
        loadSongsFromPlaylist(playlistId);
        Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}