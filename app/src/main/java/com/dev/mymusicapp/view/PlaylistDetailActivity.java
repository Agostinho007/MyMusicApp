package com.dev.mymusicapp.view;

// Imports de componentes do Android e bibliotecas
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
import androidx.annotation.Nullable;
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

/**
 * PlaylistDetailActivity é a tela que exibe as músicas contidas numa playlist específica.
 * Ela permite ao utilizador ver, tocar, adicionar e remover músicas dessa playlist.
 */
public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, SongAdapter.OnSongLongClickListener {

    // --- Variáveis de Membro ---
    private ActivityPlaylistDetailBinding binding; // Objeto de ViewBinding para a UI.
    private AppDatabase db; // Instância do banco de dados Room.
    private SongAdapter songAdapter; // Adapter para a lista de músicas.
    private ExecutorService executorService; // Para executar tarefas de banco de dados em background.
    private PlaylistWithSongs currentPlaylist; // Objeto que contém a playlist e a sua lista de músicas.
    private int playlistId = -1; // ID da playlist que está a ser exibida.

    // Variáveis para a conexão com o MusicService.
    private MusicService musicService;
    private boolean isBound = false;
    private Player.Listener playerListener;

    /**
     * ActivityResultLauncher é a forma moderna no Android de iniciar uma Activity e receber um resultado de volta.
     * Usamo-lo para abrir a SelectSongsActivity e receber a lista de músicas selecionadas.
     */
    private final ActivityResultLauncher<Intent> selectSongsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Este bloco é executado quando a SelectSongsActivity se fecha.
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Se o resultado for OK, extrai a lista de músicas selecionadas.
                    List<Song> selectedSongs = (List<Song>) result.getData().getSerializableExtra("SELECTED_SONGS");
                    if (selectedSongs != null && !selectedSongs.isEmpty()) {
                        // Chama o metodo para adicionar as músicas à base de dados.
                        addSongsToPlaylist(selectedSongs);
                    }
                }
            });

    /**
     * Objeto que gere a conexão com o MusicService.
     */
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            // Assim que a conexão é estabelecida, atualiza o destaque da música atual na lista.
            updateHighlight();

            // Adiciona um listener para ser notificado de futuras mudanças na música.
            musicService.addListener(playerListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    /**
     * Metodo principal do ciclo de vida, chamado quando a Activity é criada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaylistDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configuração da Toolbar.
        setSupportActionBar(binding.toolbarPlaylistDetail);
        binding.toolbarPlaylistDetail.setNavigationOnClickListener(v -> onBackPressed());

        // Listener para o FloatingActionButton que abre a tela de seleção de músicas.
        binding.fabAddSongsToPlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectSongsActivity.class);
            selectSongsLauncher.launch(intent);
        });

        // Obtém os dados passados pela PlaylistsActivity (ID e nome da playlist).
        playlistId = getIntent().getIntExtra("PLAYLIST_ID", -1);
        String playlistName = getIntent().getStringExtra("PLAYLIST_NAME");

        // Define o título da Toolbar com o nome da playlist.
        getSupportActionBar().setTitle(playlistName);

        // Inicializa os componentes de dados.
        db = AppDatabase.getDatabase(getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();

        setupRecyclerView();

        // Inicializa o listener do player para reagir a mudanças de música.
        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateHighlight();
            }
        };

        // Se recebemos um ID válido, carregamos as músicas da playlist.
        if (playlistId != -1) {
            loadSongsFromPlaylist(playlistId);
        }
    }

    /**
     * Métodos do ciclo de vida para conectar e desconectar do MusicService.
     * Isto garante que a Activity está ciente do estado do player quando está visível.
     */
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

    /**
     * Adiciona uma lista de músicas à playlist atual no banco de dados.
     * A operação é executada numa thread de background.
     */
    private void addSongsToPlaylist(List<Song> songsToAdd) {
        executorService.execute(() -> {
            int newSongsCount = 0;
            for (Song song : songsToAdd) {
                // Verifica se a música já existe para evitar duplicados.
                if (db.playlistDao().countSongInPlaylist(playlistId, song.getDataPath()) == 0) {
                    db.playlistDao().insertSong(song);
                    PlaylistSongCrossRef crossRef = new PlaylistSongCrossRef();
                    crossRef.playlistId = playlistId;
                    crossRef.dataPath = song.getDataPath();
                    db.playlistDao().insertPlaylistSongCrossRef(crossRef);
                    newSongsCount++;
                }
            }
            // Exibe uma mensagem de confirmação e atualiza a UI na thread principal.
            int finalNewSongsCount = newSongsCount;
            runOnUiThread(() -> {
                loadSongsFromPlaylist(playlistId);
                Toast.makeText(this, finalNewSongsCount + " novas músicas adicionadas.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Pede ao adapter para destacar a música que está a tocar no momento.
     */
    private void updateHighlight() {
        if (isBound && musicService != null && songAdapter != null) {
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                songAdapter.setCurrentPlayingSong(currentSong.getDataPath());
            }
        }
    }

    /**
     * Configura o RecyclerView e o seu adapter.
     */
    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this, this);
        binding.recyclerViewSongsInPlaylist.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSongsInPlaylist.setAdapter(songAdapter);
    }

    /**
     * Carrega as músicas da playlist especificada a partir do banco de dados.
     * A operação é executada numa thread de background.
     */
    private void loadSongsFromPlaylist(int playlistId) {
        executorService.execute(() -> {
            currentPlaylist = db.playlistDao().getPlaylistWithSongs(playlistId);
            runOnUiThread(() -> {
                if (currentPlaylist != null) {
                    songAdapter.setSongs(currentPlaylist.songs);
                    // Após carregar a lista, atualiza o destaque.
                    updateHighlight();
                }
            });
        });
    }

    /**
     * Callback do SongAdapter quando uma música é clicada.
     * Inicia a PlayerActivity.
     */
    @Override
    public void onSongClick(Song song, View albumArtView) {
        Intent intent = new Intent(this, PlayerActivity.class);

        // Se a música clicada já estiver a tocar, apenas abre o player sem reiniciar.
        if (isBound && musicService != null && musicService.isSongPlaying(song.getDataPath())) {
            // A intent vai vazia para sinalizar à PlayerActivity para apenas se conectar.
        } else {
            // Se for uma nova música, envia a lista de reprodução atual e a posição.
            if (currentPlaylist != null && !currentPlaylist.songs.isEmpty()) {
                int position = currentPlaylist.songs.indexOf(song);
                intent.putExtra("SONG_LIST", (ArrayList<Song>) currentPlaylist.songs);
                intent.putExtra("CURRENT_POSITION", position);
            }
        }

        // Flag para trazer a PlayerActivity para a frente se ela já estiver aberta.
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        // Prepara e inicia a animação de transição da capa do álbum.
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, albumArtView, ViewCompat.getTransitionName(albumArtView));
        startActivity(intent, options.toBundle());
    }

    /**
     * Callback do SongAdapter quando uma música sofre um clique longo.
     * Mostra um diálogo de confirmação para remover a música.
     */
    @Override
    public void onSongLongClick(Song song) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Destructive)
                .setTitle("Remover Música")
                .setMessage("Tem a certeza que quer remover '" + song.getTitle() + "' desta playlist?")
                .setIcon(R.drawable.ic_delete_warning)
                .setNegativeButton("Não", null)
                .setPositiveButton("Sim, remover", (dialog, which) -> {
                    deleteSongFromPlaylist(song);
                })
                .show();
    }

    /**
     * Remove uma música da playlist atual no banco de dados.
     * A operação é executada numa thread de background.
     */
    private void deleteSongFromPlaylist(Song song) {
        executorService.execute(() -> {
            db.playlistDao().deleteSongFromPlaylist(playlistId, song.getDataPath());
            // Atualiza a UI na thread principal.
            runOnUiThread(() -> {
                loadSongsFromPlaylist(playlistId);
                Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /**
     * Metodo que foi refatorado, mas cuja chamada pode permanecer em algum lugar.
     * A lógica foi movida para dentro do 'deleteSongFromPlaylist' para maior clareza.
     */
    private void loadSongsFromPlaylistAfterUpdate() {
        loadSongsFromPlaylist(playlistId);
        Toast.makeText(this, "Música removida", Toast.LENGTH_SHORT).show();
    }
}