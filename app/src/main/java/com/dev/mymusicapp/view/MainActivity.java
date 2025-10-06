package com.dev.mymusicapp.view;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.transition.TransitionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;

import com.dev.mymusicapp.R;
import com.dev.mymusicapp.adapter.SongAdapter;
import com.dev.mymusicapp.data.SongRepository;
import com.dev.mymusicapp.databinding.ActivityMainBinding;
import com.dev.mymusicapp.model.Song;

import com.dev.mymusicapp.service.MusicService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.media3.common.Player;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener, IACRCloudListener {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private static final int PERMISSION_RECORD_AUDIO_CODE = 103;

    private static final int ALL_PERMISSIONS_REQUEST_CODE = 105;
    private ActivityMainBinding binding;
    private SongRepository songRepository;
    private SongAdapter songAdapter;
    private List<Song> fullSongList = new ArrayList<>();

    // --- VARIÁVEIS DA ACRCloud ---
    private ACRCloudClient acrCloudClient;
    private ACRCloudConfig acrCloudConfig;
    private boolean isRecognizing = false;
    private AlertDialog recognitionDialog;
    private boolean isFabMenuOpen = false;
    private MusicService musicService;
    private boolean isBound = false;
    private Player.Listener playerListener;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {

                int positionInMainList = findSongPositionByPath(currentSong.getDataPath());

                if (positionInMainList != -1) {
                    binding.recyclerViewSongs.post(() -> {
                        binding.recyclerViewSongs.scrollToPosition(positionInMainList);
                    });
                }

                songAdapter.setCurrentPlayingSong(currentSong.getDataPath());
            }

            musicService.addListener(playerListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            if (musicService != null) {
                musicService.removeListener(playerListener);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarMain);

        songRepository = new SongRepository();
        setupRecyclerView();
        checkAndRequestPermissions();

        playerListener = new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateHighlight();
            }
        };
        setupAcrCloud();
        setupFabMenu();

    }

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

    private void updateHighlight() {
        if (isBound && musicService != null && songAdapter != null) {
            Song currentSong = musicService.getCurrentPlayingSong();
            if (currentSong != null) {
                songAdapter.setCurrentPlayingSong(currentSong.getDataPath());
            }
        }
    }

    private int findSongPositionByPath(String dataPath) {
        if (dataPath == null || fullSongList == null) {
            return -1;
        }
        for (int i = 0; i < fullSongList.size(); i++) {
            if (fullSongList.get(i).getDataPath().equals(dataPath)) {
                return i;
            }
        }
        return -1;
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        String storagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(storagePermission);
        }

        // Permissão de Notificação (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Permissão de Microfone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionsToRequest.isEmpty()) {
            // Pede todas as permissões necessárias de uma só vez
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), ALL_PERMISSIONS_REQUEST_CODE);
        } else {
            // Se todas as permissões já estiverem concedidas, carrega as músicas
            loadSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            // Em vez de percorrer os resultados, simplesmente verificamos se a permissão
            // essencial (armazenamento/música) está agora concedida.

            String storagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_AUDIO
                    : Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED) {
                // Se temos permissão para ler músicas, carregamo-las.
                loadSongs();
            } else {
                // Se, após tudo, ainda não temos a permissão essencial, mostramos o aviso.
                Toast.makeText(this, "A permissão de acesso a músicas é necessária para a app funcionar.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showListeningDialog() {
        // Usamos o tema de Input que já tem os botões cinzentos
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Input);

        // Inflamos o nosso layout personalizado
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_listening, null);
        builder.setView(dialogView);

        // Desativamos a possibilidade de o cancelar tocando fora
        builder.setCancelable(false);

        builder.setNegativeButton("Cancelar", (dialog, which) -> stopRecognition());

        recognitionDialog = builder.create();
        recognitionDialog.show();
    }


    private void setupFabMenu() {

        binding.fabMain.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        // Adiciona os listeners aos botões secundários
        binding.fabPlaylists.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PlaylistsActivity.class);
            startActivity(intent);
            closeFabMenu(); // Fecha o menu após a ação
        });

        binding.fabRecognize.setOnClickListener(v -> {
            checkAndStartRecognition();
            closeFabMenu(); // Fecha o menu após a ação
        });
    }




    private void openFabMenu() {
        isFabMenuOpen = true;

        // Torna os botões visíveis antes de animar
        binding.fabPlaylists.setVisibility(View.VISIBLE);
        binding.fabRecognize.setVisibility(View.VISIBLE);

        // Gira o botão principal
        binding.fabMain.animate().rotation(45f);

        // Anima os botões secundários para a sua posição final
        // Move o botão de playlists para cima uma certa distância
        binding.fabPlaylists.animate().translationY(-getResources().getDimension(R.dimen.fab_playlists_translation_y)).alpha(1f);
        // Move o botão de reconhecimento para cima uma distância menor
        binding.fabRecognize.animate().translationY(-getResources().getDimension(R.dimen.fab_recognize_translation_y)).alpha(1f);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;

        // Gira o botão principal de volta
        binding.fabMain.animate().rotation(0f);

        // Anima os botões secundários de volta para a posição inicial (translationY = 0)
        binding.fabPlaylists.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabPlaylists.setVisibility(View.INVISIBLE));
        binding.fabRecognize.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabRecognize.setVisibility(View.INVISIBLE));
    }

    //LÓGICA DA ACRCloud

    private void setupAcrCloud() {
        acrCloudConfig = new ACRCloudConfig();
        acrCloudConfig.context = this;
        acrCloudConfig.host = "identify-eu-west-1.acrcloud.com";
        acrCloudConfig.accessKey = "ebdbf94f27585e9638cd1a46a50dd2cd";
        acrCloudConfig.accessSecret = "PLGnkKGkWNJWNkkeSfHut28SG5me338O4jcBmvwR";
        acrCloudConfig.acrcloudListener = this;


        Log.d("ACRCloud", "Host: " + acrCloudConfig.host);
        Log.d("ACRCloud", "Access Key: " + acrCloudConfig.accessKey);

        acrCloudClient = new ACRCloudClient();
        boolean success = acrCloudClient.initWithConfig(acrCloudConfig);

        //Verifica se a inicialização foi bem-sucedida
        Log.d("ACRCloud", "SDK Init Success: " + success);
    }

    private void checkAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognition();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO_CODE);
        }
    }

    private void startRecognition() {
        if (isRecognizing) return;

        isRecognizing = acrCloudClient.startRecognize();
        if (isRecognizing) {
            showListeningDialog();
        } else {
            Toast.makeText(this, "Erro ao iniciar o reconhecimento.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecognition() {
        if (isRecognizing && acrCloudClient != null) {
            //acrCloudClient.stopRecognize();
        }
        isRecognizing = false;
        if (recognitionDialog != null && recognitionDialog.isShowing()) {
            recognitionDialog.dismiss();
        }
    }

    @Override
    public void onVolumeChanged(double volume) {
        // Não precisamos de implementar isto
    }

    @Override
    public void onSongClick(Song song, View albumArtView) {
        Intent intent = new Intent(this, PlayerActivity.class);

        if (isBound && musicService != null && musicService.isSongPlaying(song.getDataPath())) {
            // Lógica para não reiniciar a música.
        } else {
            int position = fullSongList.indexOf(song);
            intent.putExtra("SONG_LIST", (ArrayList<Song>) fullSongList);
            intent.putExtra("CURRENT_POSITION", position);
        }

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                albumArtView,
                ViewCompat.getTransitionName(albumArtView)
        );

        startActivity(intent, options.toBundle());
    }

    @Override
    public void onResult(ACRCloudResult acrCloudResult) {
        String result = acrCloudResult.getResult();
        if (acrCloudClient != null) acrCloudClient.cancel();
        isRecognizing = false;
        if (recognitionDialog != null && recognitionDialog.isShowing()) recognitionDialog.dismiss();

        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject status = jsonObject.getJSONObject("status");
            int code = status.getInt("code");

            if (code == 0) { // Código 0 = Sucesso
                JSONObject metadata = jsonObject.getJSONObject("metadata");
                if (metadata.has("music")) {
                    JSONArray music = metadata.getJSONArray("music");
                    JSONObject songData = music.getJSONObject(0);
                    String title = songData.getString("title");
                    String artist = songData.getJSONArray("artists").getJSONObject(0).getString("name");
                    // Chama o novo diálogo de SUCESSO
                    new android.os.Handler(Looper.getMainLooper()).post(() -> showSuccessDialog(title, artist));
                } else {
                    // Caso raro: sucesso mas sem dados de música
                    new android.os.Handler(Looper.getMainLooper()).post(() -> showErrorDialog("Nenhum resultado encontrado."));
                }
            } else {
                // Qualquer outro código = Falha (Música não encontrada, Erro de Conexão, etc.)
                String msg = status.getString("msg");
                // Chama o novo diálogo de ERRO
                new android.os.Handler(Looper.getMainLooper()).post(() -> showErrorDialog(msg));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // Erro ao processar a resposta
            new android.os.Handler(Looper.getMainLooper()).post(() -> showErrorDialog("Ocorreu um erro ao processar a resposta."));
        }
    }

    private void showSuccessDialog(String title, String artist) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Neutral)
                .setTitle("Música Encontrada")
                .setIcon(R.drawable.ic_check_circle)
                .setMessage("Título: " + title + "\nArtista: " + artist)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Neutral)
                .setTitle("Não foi possível identificar")
                .setIcon(R.drawable.ic_error)
                .setMessage(message) // Mostra a mensagem de erro do servidor
                .setPositiveButton("OK", null)
                .show();
    }


    private void showResultDialog(String title, String artist) {
        new AlertDialog.Builder(this)
                .setTitle("Música Encontrada")
                .setMessage("Título: " + title + "\nArtista: " + artist)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Buscar por título ou artista...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filterSongs(newText);
                return true;
            }
        });
        return true;
    }


    private void filterSongs(String text) {
        List<Song> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(fullSongList);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (Song song : fullSongList) {
                if (song.getTitle().toLowerCase().contains(filterPattern) ||
                        song.getArtist().toLowerCase().contains(filterPattern)) {
                    filteredList.add(song);
                }
            }
        }
        songAdapter.setSongs(filteredList);
    }

    private void setupRecyclerView() {
        // Em MainActivity.java, dentro de setupRecyclerView()
        songAdapter = new SongAdapter(this, null); // Passa null para o longClickListener
        binding.recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSongs.setAdapter(songAdapter);
    }

    private void loadSongs() {
        binding.progressBar.setVisibility(View.VISIBLE);
        this.fullSongList = songRepository.getSongs(this);
        songAdapter.setSongs(this.fullSongList);
        binding.progressBar.setVisibility(View.GONE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acrCloudClient != null) {
            acrCloudClient.release();
            acrCloudClient = null;
        }
    }
}