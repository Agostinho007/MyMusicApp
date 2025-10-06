package com.dev.mymusicapp.view;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.IBinder;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.dev.mymusicapp.R;
import com.dev.mymusicapp.adapter.SongAdapter;
import com.dev.mymusicapp.databinding.ActivityPlayerBinding;
import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.presenter.PlayerContract;
import com.dev.mymusicapp.presenter.PlayerPresenter;
import com.dev.mymusicapp.service.MusicService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity implements PlayerContract.View, SongAdapter.OnSongClickListener {

    private ActivityPlayerBinding binding;
    private PlayerContract.Presenter presenter;
    private SongAdapter playerSongAdapter;
    private ObjectAnimator rotationAnimator;
    private boolean isBound = false;
    private Menu optionsMenu;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            presenter.onServiceConnected(binder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            presenter.onServiceDisconnected();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        presenter = new PlayerPresenter(this);
        presenter.attachView(this);

        setupUI();
        startMusicService();
    }

    private void setupUI() {
        setupPlayerRecyclerView();
        setupClickListeners();
        setupRotationAnimation();
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void startMusicService() {
        Intent intent = getIntent();
        Intent serviceIntent = new Intent(this, MusicService.class);
        boolean startNewPlayback = intent != null && intent.hasExtra("SONG_LIST");

        if (startNewPlayback) {
            ArrayList<Song> songList = (ArrayList<Song>) intent.getSerializableExtra("SONG_LIST");
            int currentPosition = intent.getIntExtra("CURRENT_POSITION", 0);
            serviceIntent.putExtra("SONG_LIST", songList);
            serviceIntent.putExtra("CURRENT_POSITION", currentPosition);
            startService(serviceIntent);
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private void setupPlayerRecyclerView() {
        playerSongAdapter = new SongAdapter(this, null); // Passa null para o longClickListener
        binding.playerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.playerRecyclerView.setAdapter(playerSongAdapter);
    }

    private void setupClickListeners() {
        binding.playPauseButton.setOnClickListener(v -> presenter.onPlayPauseClicked());
        binding.nextButton.setOnClickListener(v -> presenter.onNextClicked());
        binding.prevButton.setOnClickListener(v -> presenter.onPrevClicked());
        binding.shuffleButton.setOnClickListener(v -> presenter.onShuffleClicked());
        binding.repeatButton.setOnClickListener(v -> presenter.onRepeatClicked());

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                presenter.onSeekBarChanged(seekBar.getProgress());
            }
        });
    }

    @Override
    public void onSongClick(Song song, View albumArtView) {
        int position = playerSongAdapter.getSongs().indexOf(song);
        presenter.onSongClickedInQueue(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        this.optionsMenu = menu; // Guarda a referência do menu
        return true;
    }

    @Override
    public void updateDetailsMenuTitle(boolean areDetailsVisible) {
        if (optionsMenu != null) {
            MenuItem detailsItem = optionsMenu.findItem(R.id.action_details);
            if (detailsItem != null) {
                if (areDetailsVisible) {
                    detailsItem.setTitle("Ver Músicas"); // Texto quando os detalhes estão visíveis
                } else {
                    detailsItem.setTitle("Detalhes da Música"); // Texto padrão
                }
            }
        }
    }

    @Override
    public void showSongDetails(Song song) {
        binding.titleTextView.setText(song.getTitle());
        binding.artistTextView.setText(song.getArtist());

        Glide.with(this)
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.album_art_placeholder)
                .error(R.drawable.ic_music_note)
                .circleCrop()
                .into(binding.albumArtImageView);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add_to_playlist) {
            presenter.onAddToPlaylistClicked();
            return true;
        } else if (itemId == R.id.action_details) {
            presenter.onSongDetailsClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void toggleDetailsView(boolean show, Song song) {
        // 1. Prepara a animação no contentor raiz da nossa Activity
        TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());

        // 2. Muda a visibilidade dos contentores
        if (show) {
            // Mostra os detalhes e esconde a playlist
            binding.playlistContainer.setVisibility(View.GONE);
            binding.detailsContainer.setVisibility(View.VISIBLE);

            // Preenche os detalhes da música
            String details = "Título: " + song.getTitle() + "\n\n" +
                    "Artista: " + song.getArtist() + "\n\n" +
                    "Duração: " + formatTime(song.getDuration()) + "\n\n" +
                    "Caminho: " + song.getDataPath();
            binding.detailsText.setText(details);
        } else {
            // Esconde os detalhes e mostra a playlist
            binding.detailsContainer.setVisibility(View.GONE);
            binding.playlistContainer.setVisibility(View.VISIBLE);
        }
        // 3. O TransitionManager anima automaticamente a mudança entre GONE e VISIBLE
    }

    @Override
    public void updateProgress(long currentPosition, long duration) {
        binding.seekBar.setMax((int) duration);
        binding.seekBar.setProgress((int) currentPosition);
        binding.currentTimeTextView.setText(formatTime(currentPosition));
        binding.totalTimeTextView.setText(formatTime(duration));
    }

    @Override
    public void showPlayIcon() {
        binding.playPauseButton.setImageResource(R.drawable.ic_play);
        rotationAnimator.pause();
    }

    @Override
    public void showPauseIcon() {
        binding.playPauseButton.setImageResource(R.drawable.ic_pause);
        if (rotationAnimator.isPaused()) rotationAnimator.resume();
        else rotationAnimator.start();
    }

    @Override
    public void updateShuffleRepeatUI(boolean isShuffleOn, int repeatMode) {
        binding.shuffleButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, isShuffleOn ? R.color.teal_200 : android.R.color.white)));
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat);
            binding.repeatButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        } else if (repeatMode == Player.REPEAT_MODE_ONE) {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat_one);
            binding.repeatButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_200)));
        } else {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat);
            binding.repeatButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_200)));
        }
    }

    @Override
    public void updateAdapterHighlight(String songPath) {
        if (playerSongAdapter != null) {
            playerSongAdapter.setCurrentPlayingSong(songPath);
        }
    }

    @Override
    public void scrollToCurrentSong(int position) {
        if (position >= 0 && binding.playerRecyclerView != null) {
            // Usamos post para garantir que o scroll acontece depois do layout
            binding.playerRecyclerView.post(() -> {
                binding.playerRecyclerView.scrollToPosition(position);
            });
        }
    }

    @Override
    public void showSongInfoDialog(Song song) {
        String details = "Título: " + song.getTitle() + "\n\n" +
                "Artista: " + song.getArtist() + "\n\n" +
                "Duração: " + formatTime(song.getDuration()) + "\n\n" +
                "Caminho: " + song.getDataPath();

        //Usamos o MaterialAlertDialogBuilder
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Neutral)
                .setTitle("Detalhes da Música")
                .setMessage(details)
                .setIcon(R.drawable.ic_info) // NOVO: Adiciona o nosso ícone
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void showPlaylistsDialog(List<Playlist> playlists) {
        if (playlists.isEmpty()) {
            showToast("Nenhuma playlist criada.");
            return;
        }
        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).name;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Adicionar à Playlist")
                .setIcon(R.drawable.ic_playlist_add) // Adicionamos um ícone para clareza
                .setItems(playlistNames, (dialog, which) -> {
                    // A lógica de notificar o Presenter continua a mesma
                    presenter.onPlaylistSelected(playlists.get(which), null);
                })
                .show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updatePlaylist(List<Song> songs) {
        playerSongAdapter.setSongs(songs);
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(binding.albumArtImageView, "rotation", 0f, 360f);
        rotationAnimator.setDuration(30000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.detachView();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }
}