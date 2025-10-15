package com.dev.mymusicapp.presenter;

import com.dev.mymusicapp.model.Playlist;
import com.dev.mymusicapp.model.Song;
import com.dev.mymusicapp.service.MusicService;

import java.util.List;

/**
 * A interface PlayerContract define o "contrato" ou as regras de comunicação
 * entre a View (PlayerActivity) e o Presenter (PlayerPresenter).
 * Seguir este padrão garante que a View e o Presenter estejam desacoplados,
 * facilitando a manutenção e os testes.
 */
public interface PlayerContract {

    /**
     * A interface View define todos os métodos (comandos) que o Presenter pode chamar na View.
     * A PlayerActivity deve implementar esta interface. A sua única responsabilidade é
     * executar estas ações na UI, sem qualquer lógica de negócio.
     */
    interface View {
        // Atualiza a UI com os detalhes da música (título, artista, capa).
        void showSongDetails(Song song);

        // Atualiza a SeekBar e os TextViews de tempo.
        void updateProgress(long currentPosition, long duration);

        // Muda o ícone do botão principal para "Play".
        void showPlayIcon();

        // Muda o ícone do botão principal para "Pause".
        void showPauseIcon();

        // Atualiza o estado visual (cores) dos botões de shuffle e repeat.
        void updateShuffleRepeatUI(boolean isShuffleOn, int repeatMode);

        // Mostra o diálogo com a lista de playlists para adicionar a música.
        void showPlaylistsDialog(List<Playlist> playlists);

        // Mostra o diálogo com informações detalhadas da música.
        void showSongInfoDialog(Song song);

        // Anima a transição entre a lista "A Seguir" e o painel de detalhes.
        void toggleDetailsView(boolean show, Song song);

        // Exibe uma mensagem Toast simples.
        void showToast(String message);

        // Atualiza a lista de músicas no RecyclerView do player.
        void updatePlaylist(List<Song> songs);

        // Diz ao adapter para destacar a música que está a tocar.
        void updateAdapterHighlight(String songPath);

        // Comanda o RecyclerView para rolar até à posição da música atual.
        void scrollToCurrentSong(int position);

        // Altera o texto do item de menu ("Detalhes da Música" / "Ver Músicas").
        void updateDetailsMenuTitle(boolean areDetailsVisible);
    }

    /**
     * A interface Presenter define todos os métodos (eventos) que a View pode chamar no Presenter.
     * O PlayerPresenter deve implementar esta interface. Ele recebe os eventos da UI,
     * processa a lógica de negócio e comanda a View sobre o que fazer.
     */
    interface Presenter {
        // Conecta o Presenter à View quando a Activity é criada.
        void attachView(View view);

        // Desconecta o Presenter da View quando a Activity é destruída para evitar memory leaks.
        void detachView();

        // Notifica o Presenter que a conexão com o MusicService foi estabelecida.
        void onServiceConnected(MusicService service);

        // Notifica o Presenter que a conexão com o MusicService foi perdida.
        void onServiceDisconnected();

        // Eventos de clique nos botões de controlo.
        void onPlayPauseClicked();
        void onNextClicked();
        void onPrevClicked();
        void onShuffleClicked();
        void onRepeatClicked();

        // Evento de quando o utilizador arrasta e solta a SeekBar.
        void onSeekBarChanged(int progress);

        // Eventos de clique nos itens de menu.
        void onAddToPlaylistClicked();
        void onSongDetailsClicked();

        // Evento de quando o utilizador clica numa música na lista "A Seguir" dentro do player.
        void onSongClickedInQueue(int position);

        // Evento de quando o utilizador seleciona uma playlist no diálogo "Adicionar à Playlist".
        void onPlaylistSelected(Playlist playlist, Song song);
    }
}