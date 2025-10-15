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

/**
 * MusicService é um Service do Android responsável por gerir a reprodução de música.
 * Ele corre em background (segundo plano), permitindo que a música continue a tocar
 * mesmo que o utilizador saia da aplicação.
 */
public class MusicService extends Service {

    // O 'Binder' é um objeto que permite que as Activities se "conectem" a este serviço
    // para poderem chamar os seus métodos públicos (ex: play, pause).
    private final IBinder binder = new MusicBinder();

    // A instância principal do player. O serviço é o único "dono" desta instância.
    private ExoPlayer exoPlayer;

    // A lista de músicas que está a ser reproduzida atualmente.
    private List<Song> songList;

    // MediaSession integra o nosso player com o sistema Android (notificações, controlos de ecrã de bloqueio, etc.).
    private MediaSession mediaSession;

    // Constantes para a notificação do serviço em primeiro plano.
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    /**
     * Classe interna que estende Binder. A sua única função é fornecer um método
     * para que os clientes (Activities) obtenham uma referência direta a este MusicService.
     */
    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Chamado quando o serviço é criado pela primeira vez.
     * Ideal para inicializar componentes que só precisam de ser criados uma vez.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayer = new ExoPlayer.Builder(this).build();
        mediaSession = new MediaSession.Builder(this, exoPlayer).build();
        createNotificationChannel();

        // Adiciona um listener ao ExoPlayer para reagir a eventos importantes.
        exoPlayer.addListener(new Player.Listener() {
            /**
             * Chamado sempre que a música muda (para a próxima ou anterior).
             */
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                // Quando a música muda, atualizamos a notificação para mostrar os novos dados.
                updateNotification();
            }

            /**
             * Chamado quando o estado de reprodução muda (play/pause).
             */
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                // Atualizamos a notificação para mostrar o ícone correto de play ou pause nos controlos.
                updateNotification();
            }
        });
    }

    /**
     * Chamado quando uma Activity se conecta ao serviço (bindService).
     * Retorna o nosso objeto 'binder' para que a comunicação possa ser estabelecida.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Chamado quando o serviço é iniciado (startService).
     * É aqui que recebemos a lista de músicas e a posição inicial, e onde
     * promovemos o serviço para um "Foreground Service".
     */
    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            ArrayList<Song> receivedSongs = (ArrayList<Song>) intent.getSerializableExtra("SONG_LIST");
            int position = intent.getIntExtra("CURRENT_POSITION", 0);

            if (receivedSongs != null) {
                this.songList = receivedSongs;
                preparePlaylist(position);

                // Transforma este serviço num serviço em primeiro plano, associando-o a uma notificação.
                // Isto impede o sistema de o encerrar para poupar bateria.
                startForeground(NOTIFICATION_ID, createNotification());
            }
        }
        // START_STICKY: Se o sistema encerrar o serviço, ele tentará recriá-lo mais tarde.
        return START_STICKY;
    }

    /**
     * Prepara o ExoPlayer com a lista de músicas e a posição inicial.
     */
    private void preparePlaylist(int startPosition) {
        List<MediaItem> mediaItems = new ArrayList<>();
        // Converte a nossa lista de 'Song' para uma lista de 'MediaItem', que o ExoPlayer entende.
        for (Song song : songList) {
            mediaItems.add(MediaItem.fromUri(song.getDataPath()));
        }
        // Define a lista de itens e a posição inicial no player.
        exoPlayer.setMediaItems(mediaItems, startPosition, 0);
        // Prepara o player para a reprodução (carrega os metadados, etc.).
        exoPlayer.prepare();
        // Inicia a reprodução.
        exoPlayer.play();
    }

    /**
     * Força a atualização da notificação em primeiro plano.
     */
    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    /**
     * Constrói e retorna a notificação com os controlos de média.
     */
    @OptIn(markerClass = UnstableApi.class)
    private Notification createNotification() {
        int currentSongIndex = exoPlayer.getCurrentMediaItemIndex();
        Song currentSong = songList.get(currentSongIndex);

        // Intent que será disparado se o utilizador clicar na notificação (abre a PlayerActivity).
        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Usa o helper do Media3 para criar uma notificação com estilo de média e controlos.
        MediaStyleNotificationHelper.MediaStyle mediaStyle = new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0, 1, 2); // Define quais botões aparecem na notificação compacta.

        // Constrói a notificação.
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtist())
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent) // Ação ao clicar na notificação.
                .setStyle(mediaStyle) // Aplica o estilo de média.
                .setOnlyAlertOnce(true) // Evita que a notificação faça som/vibre a cada atualização.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visível no ecrã de bloqueio.
                .build();
    }

    /**
     * Cria o canal de notificação, obrigatório para Android 8.0 (Oreo) e superior.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player Channel",
                    NotificationManager.IMPORTANCE_LOW // Prioridade baixa para que não emita som de notificação.
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    // --- MÉTODOS PÚBLICOS DE CONTROLO (API do Serviço) ---
    // Estes métodos são chamados pelas Activities que estão conectadas a este serviço.

    public void play() {
        if (exoPlayer != null) exoPlayer.play();
    }
    public void pause() {
        if (exoPlayer != null) exoPlayer.pause();
    }
    public void seekToNext() {
        if (exoPlayer != null) exoPlayer.seekToNextMediaItem();
    }
    public void seekToPrevious() {
        if (exoPlayer != null) exoPlayer.seekToPreviousMediaItem();
    }
    public void seekTo(long position) {
        if (exoPlayer != null) exoPlayer.seekTo(position);
    }
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }
    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }
    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }
    public int getCurrentSongIndex() {
        return exoPlayer != null ? exoPlayer.getCurrentMediaItemIndex() : 0;
    }
    public void addListener(Player.Listener listener) {
        if (exoPlayer != null) exoPlayer.addListener(listener);
    }
    public void removeListener(Player.Listener listener) {
        if (exoPlayer != null) exoPlayer.removeListener(listener);
    }

    public void toggleShuffleMode() {
        if (exoPlayer != null) {
            exoPlayer.setShuffleModeEnabled(!exoPlayer.getShuffleModeEnabled());
        }
    }

    public void seekToSongInPlaylist(int position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position, 0);
            exoPlayer.play();
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
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            } else if (currentMode == Player.REPEAT_MODE_ONE) {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            } else {
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }
    }

    public boolean isShuffleModeEnabled() {
        return exoPlayer != null && exoPlayer.getShuffleModeEnabled();
    }

    public int getRepeatMode() {
        return exoPlayer != null ? exoPlayer.getRepeatMode() : Player.REPEAT_MODE_OFF;
    }

    /**
     * Chamado quando o serviço está a ser destruído.
     * É crucial libertar os recursos do player e da media session aqui.
     */
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