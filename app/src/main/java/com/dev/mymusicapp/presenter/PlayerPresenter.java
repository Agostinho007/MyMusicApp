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

/**
 * PlayerPresenter é o "Apresentador" para a PlayerActivity, seguindo a arquitetura MVP.
 * Ele contém toda a lógica de negócio e de apresentação, agindo como uma ponte entre
 * a View (PlayerActivity) e o Model (MusicService, AppDatabase).
 */
public class PlayerPresenter implements PlayerContract.Presenter {

    // --- Variáveis de Membro ---
    private PlayerContract.View view; // Referência à View (PlayerActivity) que este Presenter controla.
    private MusicService musicService; // A instância do serviço de música, que é a nossa fonte da verdade para o playback.
    private boolean isBound = false; // Flag para saber se estamos conectados ao MusicService.

    private AppDatabase db; // Instância da base de dados para operações de playlist.
    private ExecutorService executorService; // Para executar tarefas de base de dados em background.

    private Handler handler; // Handler para agendar atualizações da UI (SeekBar).
    private Runnable updateSeekBarRunnable; // A tarefa que atualiza a SeekBar.

    private boolean isDetailsViewVisible = false; // Controla o estado de visibilidade do painel de detalhes.

    /**
     * Construtor do Presenter.
     * @param context Contexto da aplicação, usado para obter a instância da base de dados.
     */
    public PlayerPresenter(Context context) {
        this.db = AppDatabase.getDatabase(context.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
    }

    // --- Métodos do Ciclo de Vida do Presenter ---

    @Override
    public void attachView(PlayerContract.View view) {
        // Guarda a referência da View (Activity) para que o Presenter possa comandá-la.
        this.view = view;
    }

    @Override
    public void detachView() {
        // Liberta a referência da View quando a Activity é destruída para evitar memory leaks.
        this.view = null;
        // Remove quaisquer tarefas agendadas para evitar que executem com a View destruída.
        handler.removeCallbacks(updateSeekBarRunnable);
        // Remove o listener do serviço para também evitar leaks.
        if (musicService != null) {
            musicService.removeListener(playerListener);
        }
    }

    // --- Métodos de Conexão com o Serviço ---

    @Override
    public void onServiceConnected(MusicService service) {
        this.musicService = service;
        this.isBound = true;

        // Adiciona um listener para que o Presenter seja notificado sobre eventos do player (mudança de música, play/pause).
        musicService.addListener(playerListener);

        // Agora que a conexão está estabelecida, comanda a View para se atualizar completamente.
        updateFullUI();

        // Inicia a tarefa de atualização contínua da SeekBar.
        initializeSeekBarRunnable();
        handler.post(updateSeekBarRunnable);
    }

    @Override
    public void onServiceDisconnected() {
        this.isBound = false;
        this.musicService = null;
    }

    /**
     * Listener privado que reage a eventos do ExoPlayer que vêm do MusicService.
     */
    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (view == null) return; // Salvaguarda: não faz nada se a View já foi destruída.

            // Comanda a View para atualizar o ícone de play/pause e para iniciar/parar a atualização da SeekBar.
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
            // Quando a música muda, comanda uma atualização completa da UI.
            updateFullUI();
        }
    };

    /**
     * Define a tarefa (Runnable) que será executada repetidamente para atualizar o progresso da SeekBar.
     */
    private void initializeSeekBarRunnable() {
        updateSeekBarRunnable = () -> {
            if (isBound && musicService.isPlaying() && view != null) {
                // Comanda a View para atualizar a SeekBar com a posição e duração atuais.
                view.updateProgress(musicService.getCurrentPosition(), musicService.getDuration());
                // Agenda a próxima execução para daqui a 1 segundo.
                handler.postDelayed(updateSeekBarRunnable, 1000);
            }
        };
    }

    // --- Métodos Chamados pela View (Eventos de UI) ---

    // Os métodos abaixo são implementações da interface PlayerContract.Presenter.
    // Eles são chamados pela PlayerActivity sempre que o utilizador interage com a UI.

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
        if (isBound && view != null) {
            musicService.toggleShuffleMode();
            // Após mudar o estado, comanda a View para atualizar o ícone correspondente.
            view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
        }
    }
    @Override public void onRepeatClicked() {
        if (isBound && view != null) {
            musicService.toggleRepeatMode();
            view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
        }
    }
    @Override public void onSeekBarChanged(int progress) { if (isBound) musicService.seekTo(progress); }
    @Override public void onSongClickedInQueue(int position) { if (isBound) musicService.seekToSongInPlaylist(position); }

    @Override
    public void onAddToPlaylistClicked() {
        // Executa a busca de playlists numa thread de background para não bloquear a UI.
        executorService.execute(() -> {
            List<Playlist> playlists = db.playlistDao().getAllPlaylists();
            if (view != null) {
                // De volta à thread principal, comanda a View para mostrar o diálogo.
                new Handler(Looper.getMainLooper()).post(() -> view.showPlaylistsDialog(playlists));
            }
        });
    }

    /**
     * Metodo central que comanda a View para se atualizar com todas as informações mais recentes do MusicService.
     */
    private void updateFullUI() {
        if (isBound && view != null) {
            int currentPosition = musicService.getCurrentSongIndex();
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                view.showSongDetails(currentSong);
                view.updateProgress(musicService.getCurrentPosition(), currentSong.getDuration());
                view.updateShuffleRepeatUI(musicService.isShuffleModeEnabled(), musicService.getRepeatMode());
                view.updatePlaylist(musicService.getCurrentSongList());
                view.updateAdapterHighlight(currentSong.getDataPath());
                view.scrollToCurrentSong(currentPosition);
                view.updateDetailsMenuTitle(isDetailsViewVisible);
                if (musicService.isPlaying()) {
                    view.showPauseIcon();
                } else {
                    view.showPlayIcon();
                }
            }
        }
    }

    @Override
    public void onSongDetailsClicked() {
        if (isBound && view != null) {
            isDetailsViewVisible = !isDetailsViewVisible; // Inverte o estado de visibilidade
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                // Comanda a View para animar a troca de painéis e para atualizar o texto do menu.
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
                // Verifica se a música já existe na playlist antes de a adicionar.
                int count = db.playlistDao().countSongInPlaylist(playlist.playlistId, currentSong.getDataPath());

                if (count > 0) {
                    // Se já existe, comanda a View para mostrar um aviso.
                    if (view != null) {
                        new Handler(Looper.getMainLooper()).post(() -> view.showToast("A música já está nesta playlist."));
                    }
                } else {
                    // Se não existe, executa a inserção no banco de dados.
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