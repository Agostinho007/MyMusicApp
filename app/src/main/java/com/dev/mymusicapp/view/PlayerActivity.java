package com.dev.mymusicapp.view;

// Imports de componentes do Android e bibliotecas
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

/**
 * PlayerActivity é a "View" para a tela do player de música.
 * A sua responsabilidade é apenas exibir a interface e reportar as interações do utilizador ao Presenter.
 * Ela implementa PlayerContract.View para obedecer aos comandos do Presenter,
 * e SongAdapter.OnSongClickListener para reagir a cliques na sua lista de músicas interna.
 */
public class PlayerActivity extends AppCompatActivity implements PlayerContract.View, SongAdapter.OnSongClickListener {

    // --- Variáveis de Membro ---
    private ActivityPlayerBinding binding; // Objeto de ViewBinding para aceder às Views do layout de forma segura.
    private PlayerContract.Presenter presenter; // Referência ao Presenter que contém a lógica de negócio.
    private SongAdapter playerSongAdapter; // Adapter para o RecyclerView que mostra a lista "A Seguir".
    private ObjectAnimator rotationAnimator; // Animador para a rotação da capa do álbum.
    private boolean isBound = false; // Flag para controlar o estado da conexão com o MusicService.
    private Menu optionsMenu; // Referência ao menu da Toolbar para poder alterá-lo dinamicamente.

    /**
     * Objeto anónimo que gere a conexão (bind) com o MusicService.
     */
    private final ServiceConnection connection = new ServiceConnection() {
        /**
         * Chamado quando a conexão com o serviço é estabelecida.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            // Notifica o Presenter que a conexão foi bem-sucedida, passando a instância do serviço.
            presenter.onServiceConnected(binder.getService());
            isBound = true;
        }

        /**
         * Chamado quando a conexão com o serviço é perdida inesperadamente.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            presenter.onServiceDisconnected();
            isBound = false;
        }
    };

    /**
     * Metodo principal do ciclo de vida da Activity, chamado quando ela é criada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Infla o layout e configura o ViewBinding.
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Define a nossa Toolbar como a ActionBar da atividade.
        setSupportActionBar(binding.toolbar);

        // Cria uma instância do Presenter e anexa esta View (Activity) a ele.
        presenter = new PlayerPresenter(this);
        presenter.attachView(this);

        // Chama métodos de configuração da UI.
        setupUI();
        // Inicia ou conecta-se ao serviço de música.
        startMusicService();
    }

    /**
     * Agrupa as chamadas de configuração inicial da UI.
     */
    private void setupUI() {
        setupPlayerRecyclerView();
        setupClickListeners();
        setupRotationAnimation();
        // Configura o botão de "voltar" na Toolbar.
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    /**
     * Verifica se a Activity foi iniciada com uma nova lista de reprodução ou se deve
     * apenas conectar-se a uma sessão de reprodução já existente.
     */
    private void startMusicService() {
        Intent intent = getIntent();
        Intent serviceIntent = new Intent(this, MusicService.class);
        boolean startNewPlayback = intent != null && intent.hasExtra("SONG_LIST");

        if (startNewPlayback) {
            // Se recebemos uma nova lista, passamos os dados para o serviço e iniciamo-lo.
            ArrayList<Song> songList = (ArrayList<Song>) intent.getSerializableExtra("SONG_LIST");
            int currentPosition = intent.getIntExtra("CURRENT_POSITION", 0);
            serviceIntent.putExtra("SONG_LIST", songList);
            serviceIntent.putExtra("CURRENT_POSITION", currentPosition);
            startService(serviceIntent);
        }
        // Em ambos os casos (novo playback ou não), conectamo-nos ao serviço.
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Configura o RecyclerView interno da PlayerActivity.
     */
    private void setupPlayerRecyclerView() {
        playerSongAdapter = new SongAdapter(this, null); // Passa 'this' como listener de clique e 'null' para o clique longo.
        binding.playerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.playerRecyclerView.setAdapter(playerSongAdapter);
    }

    /**
     * Configura todos os listeners de clique para os botões.
     * Cada clique apenas notifica o Presenter sobre o evento. A lógica não está aqui.
     */
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
                // Notifica o Presenter quando o utilizador solta a SeekBar.
                presenter.onSeekBarChanged(seekBar.getProgress());
            }
        });
    }

    /**
     * Callback do SongAdapter quando uma música na lista "A Seguir" é clicada.
     */
    @Override
    public void onSongClick(Song song, View albumArtView) {
        int position = playerSongAdapter.getSongs().indexOf(song);
        presenter.onSongClickedInQueue(position);
    }

    /**
     * Cria e infla o menu de opções na Toolbar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        this.optionsMenu = menu; // Guarda a referência do menu para poder alterá-lo mais tarde.
        return true;
    }

    /**
     * Reage a cliques nos itens do menu da Toolbar.
     */
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

    // --- MÉTODOS DA INTERFACE PlayerContract.View ---
    // Estes métodos são chamados pelo Presenter para comandar a Activity a fazer algo na UI.

    @Override
    public void updateDetailsMenuTitle(boolean areDetailsVisible) {
        if (optionsMenu != null) {
            MenuItem detailsItem = optionsMenu.findItem(R.id.action_details);
            if (detailsItem != null) {
                detailsItem.setTitle(areDetailsVisible ? "Ver Músicas" : "Detalhes da Música");
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
                .error(R.drawable.ic_music_note) // Mostra a nota musical se não houver capa.
                .circleCrop()
                .into(binding.albumArtImageView);
    }

    /**
     * Anima a troca entre a lista de músicas e o painel de detalhes.
     */
    @Override
    public void toggleDetailsView(boolean show, Song song) {
        // Prepara a animação no contentor raiz da Activity.
        TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());

        if (show) {
            // Mostra os detalhes e esconde a playlist.
            binding.playlistContainer.setVisibility(View.GONE);
            binding.detailsContainer.setVisibility(View.VISIBLE);

            // Preenche o texto com os detalhes da música.
            String details = "Título: " + song.getTitle() + "\n\n" +
                    "Artista: " + song.getArtist() + "\n\n" +
                    "Duração: " + formatTime(song.getDuration()) + "\n\n" +
                    "Caminho: " + song.getDataPath();
            binding.detailsText.setText(details);
        } else {
            // Esconde os detalhes e mostra a playlist.
            binding.detailsContainer.setVisibility(View.GONE);
            binding.playlistContainer.setVisibility(View.VISIBLE);
        }
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
        if (rotationAnimator.isRunning()) rotationAnimator.pause();
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
        } else { // REPEAT_MODE_ALL
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
            binding.playerRecyclerView.post(() -> binding.playerRecyclerView.scrollToPosition(position));
        }
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
                .setIcon(R.drawable.ic_playlist_add)
                .setItems(playlistNames, (dialog, which) -> presenter.onPlaylistSelected(playlists.get(which), null))
                .show();
    }

    @Override
    public void showSongInfoDialog(Song song) {

    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updatePlaylist(List<Song> songs) {
        if (playerSongAdapter != null) {
            playerSongAdapter.setSongs(songs);
        }
    }

    /**
     * Metodo auxiliar para formatar o tempo de milissegundos para o formato "mm:ss".
     */
    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Configura a animação de rotação da capa do álbum.
     */
    private void setupRotationAnimation() {
        rotationAnimator = ObjectAnimator.ofFloat(binding.albumArtImageView, "rotation", 0f, 360f);
        rotationAnimator.setDuration(30000); // 30 segundos por rotação completa
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator()); // Velocidade constante
    }

    /**
     * Chamado quando a Activity está a ser destruída.
     * É crucial libertar recursos aqui para evitar memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Avisa o Presenter para se desanexar da View.
        presenter.detachView();
        // Se a Activity estiver conectada ao serviço, desconecta-se.
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        // Cancela a animação para libertar os seus recursos.
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
        }
    }
}