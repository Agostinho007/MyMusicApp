package com.dev.mymusicapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.view.LayoutInflater;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.dev.mymusicapp.R;
import com.dev.mymusicapp.adapter.PlaylistAdapter;
import com.dev.mymusicapp.data.AppDatabase;
import com.dev.mymusicapp.databinding.ActivityPlaylistsBinding;
import com.dev.mymusicapp.model.Playlist;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.dev.mymusicapp.adapter.PlaylistAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener, IACRCloudListener, PlaylistAdapter.OnPlaylistLongClickListener {

    private ActivityPlaylistsBinding binding;
    private AppDatabase db;
    private PlaylistAdapter adapter;
    private ExecutorService executorService;

    // Variáveis para o Multi-FAB
    private boolean isFabMenuOpen = false;

    // Variáveis da ACRCloud
    private ACRCloudClient acrCloudClient;
    private ACRCloudConfig acrCloudConfig;
    private boolean isRecognizing = false;
    private AlertDialog recognitionDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarPlaylists);
        binding.toolbarPlaylists.setNavigationOnClickListener(v -> onBackPressed());

        executorService = Executors.newSingleThreadExecutor();
        db = AppDatabase.getDatabase(getApplicationContext());

        setupRecyclerView();
        loadPlaylists();

        // Configura os novos menus
        setupFabMenu();
        setupAcrCloud();
    }

    private void setupRecyclerView() {
        adapter = new PlaylistAdapter(this, this);
        binding.recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewPlaylists.setAdapter(adapter);
    }

    private void deletePlaylist(Playlist playlist) {
        executorService.execute(() -> {
            // Apaga primeiro as referências, depois a playlist
            db.playlistDao().deleteCrossRefsByPlaylistId(playlist.playlistId);
            db.playlistDao().deletePlaylistById(playlist.playlistId);
            // Recarrega a lista
            loadPlaylists();
            runOnUiThread(() -> Toast.makeText(this, "Playlist apagada.", Toast.LENGTH_SHORT).show());
        });
    }

    //LÓGICA DO MULTI-FAB
    private void setupFabMenu() {
        binding.fabMainPlaylists.setOnClickListener(v -> {
            if (isFabMenuOpen) closeFabMenu();
            else openFabMenu();
        });

        binding.fabAddNewPlaylist.setOnClickListener(v -> {
            showCreatePlaylistDialog();
            closeFabMenu();
        });

        binding.fabRecognizePlaylist.setOnClickListener(v -> {
            checkAndStartRecognition();
            closeFabMenu();
        });
    }

    private void openFabMenu() {
        isFabMenuOpen = true;
        binding.fabAddNewPlaylist.setVisibility(View.VISIBLE);
        binding.fabRecognizePlaylist.setVisibility(View.VISIBLE);
        binding.fabMainPlaylists.animate().rotation(45f);
        binding.fabAddNewPlaylist.animate().translationY(-getResources().getDimension(R.dimen.fab_playlists_translation_y)).alpha(1f);
        // Move o botão de reconhecimento para cima uma distância menor
        binding.fabRecognizePlaylist.animate().translationY(-getResources().getDimension(R.dimen.fab_recognize_translation_y)).alpha(1f);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        binding.fabMainPlaylists.animate().rotation(0f);
        binding.fabAddNewPlaylist.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabAddNewPlaylist.setVisibility(View.INVISIBLE));
        binding.fabRecognizePlaylist.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabRecognizePlaylist.setVisibility(View.INVISIBLE));
    }

    // --- LÓGICA DA ACRCloud
    private void setupAcrCloud() {
        acrCloudConfig = new ACRCloudConfig();
        acrCloudConfig.context = this;
        acrCloudConfig.host = "identify-eu-west-1.acrcloud.com";
        acrCloudConfig.accessKey = "ebdbf94f27585e9638cd1a46a50dd2cd";
        acrCloudConfig.accessSecret = "PLGnkKGkWNJWNkkeSfHut28SG5me338O4jcBmvwR";
        acrCloudConfig.acrcloudListener = this;
        //acrCloudConfig.recMode = ACRCloudConfig.RecorderType.REC_MODE_REMOTE;

        acrCloudClient = new ACRCloudClient();
        acrCloudClient.initWithConfig(acrCloudConfig);
    }

    private void checkAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognition();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 103);
        }
    }

    private void startRecognition() {
        if (isRecognizing) return;
        isRecognizing = acrCloudClient.startRecognize();
        if (isRecognizing) showListeningDialog();
    }

    private void stopRecognition() {
        //if (isRecognizing && acrCloudClient != null) acrCloudClient.stopRecognize();
        isRecognizing = false;
        if (recognitionDialog != null && recognitionDialog.isShowing()) recognitionDialog.dismiss();
    }

    @Override
    public void onVolumeChanged(double volume) {}

    private void showResultDialog(String title, String artist) {
        new AlertDialog.Builder(this)
                .setTitle("Música Encontrada")
                .setMessage("Título: " + title + "\nArtista: " + artist)
                .setPositiveButton("OK", null)
                .show();
    }

    private void loadPlaylists() {
        executorService.execute(() -> {
            List<Playlist> playlists = db.playlistDao().getAllPlaylists();
            runOnUiThread(() -> adapter.setPlaylists(playlists));
        });
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

    // Em MainActivity.java e PlaylistsActivity.java

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

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("PLAYLIST_ID", playlist.playlistId);
        intent.putExtra("PLAYLIST_NAME", playlist.name);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acrCloudClient != null) {
            acrCloudClient.release();
            acrCloudClient = null;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void onPlaylistLongClick(Playlist playlist) {
        // ATUALIZADO: Passamos o nosso novo estilo aqui
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Destructive)
                .setTitle("Apagar Playlist")
                .setMessage("Tem a certeza que quer apagar a playlist '" + playlist.name + "'? Esta ação não pode ser desfeita.")
                .setIcon(R.drawable.ic_delete_warning)
                .setNegativeButton("Não", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton("Sim, apagar", (dialog, which) -> {
                    deletePlaylist(playlist);
                })
                .show();
    }

    private void showCreatePlaylistDialog() {
        // Usamos o Material Builder para um estilo consistente
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Input);
        builder.setTitle("Nova Playlist");
        builder.setIcon(R.drawable.ic_playlist_add); // Reutiliza o ícone do FAB

        // Infla (carrega) o nosso layout XML personalizado
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist, null);
        // Encontra o campo de texto dentro do nosso layout inflado
        final TextInputEditText input = dialogView.findViewById(R.id.edit_text_playlist_name);

        // Define o nosso layout personalizado como o conteúdo do diálogo
        builder.setView(dialogView);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                Playlist newPlaylist = new Playlist();
                newPlaylist.name = playlistName;
                executorService.execute(() -> {
                    db.playlistDao().insertPlaylist(newPlaylist);
                    loadPlaylists();
                });
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
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
}