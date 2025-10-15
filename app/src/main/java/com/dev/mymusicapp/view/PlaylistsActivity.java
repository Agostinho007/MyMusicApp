package com.dev.mymusicapp.view;

// Imports de componentes do Android e bibliotecas
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PlaylistsActivity é a tela responsável por exibir e gerir as playlists do utilizador.
 * Ela implementa listeners para cliques no adapter e para os resultados da ACRCloud.
 */
public class PlaylistsActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener, IACRCloudListener, PlaylistAdapter.OnPlaylistLongClickListener {

    // --- Variáveis de Membro ---
    private ActivityPlaylistsBinding binding; // Objeto de ViewBinding para a UI.
    private AppDatabase db; // Instância da base de dados.
    private PlaylistAdapter adapter; // Adapter para o RecyclerView que mostra as playlists.
    private ExecutorService executorService; // Para executar tarefas de base de dados em background.

    // Flag para controlar o estado (aberto/fechado) do menu FAB.
    private boolean isFabMenuOpen = false;

    // Variáveis para a funcionalidade de reconhecimento de música.
    private ACRCloudClient acrCloudClient;
    private ACRCloudConfig acrCloudConfig;
    private boolean isRecognizing = false;
    private AlertDialog recognitionDialog;


    /**
     * Método principal do ciclo de vida, chamado quando a Activity é criada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaylistsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configuração da Toolbar.
        setSupportActionBar(binding.toolbarPlaylists);
        binding.toolbarPlaylists.setNavigationOnClickListener(v -> onBackPressed());

        // Inicialização de componentes para tarefas assíncronas e acesso a dados.
        executorService = Executors.newSingleThreadExecutor();
        db = AppDatabase.getDatabase(getApplicationContext());

        // Configuração da UI e carregamento dos dados iniciais.
        setupRecyclerView();
        loadPlaylists();
        setupFabMenu();
        setupAcrCloud();
    }

    /**
     * Configura o RecyclerView e o seu adapter.
     */
    private void setupRecyclerView() {
        adapter = new PlaylistAdapter(this, this);
        binding.recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewPlaylists.setAdapter(adapter);
    }

    /**
     * Apaga uma playlist e as suas referências no banco de dados.
     * A operação é executada numa thread de background.
     */
    private void deletePlaylist(Playlist playlist) {
        executorService.execute(() -> {
            // É importante apagar primeiro as referências na tabela de junção.
            db.playlistDao().deleteCrossRefsByPlaylistId(playlist.playlistId);
            // Depois, apaga a playlist da tabela principal.
            db.playlistDao().deletePlaylistById(playlist.playlistId);
            // Recarrega a lista de playlists para atualizar a UI.
            loadPlaylists();
            // Mostra uma confirmação ao utilizador na thread principal.
            runOnUiThread(() -> Toast.makeText(this, "Playlist apagada.", Toast.LENGTH_SHORT).show());
        });
    }

    // --- LÓGICA DO MULTI-FAB ---

    /**
     * Configura os listeners de clique para o menu de Floating Action Buttons.
     */
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

    /**
     * Anima a abertura do menu FAB.
     */
    private void openFabMenu() {
        isFabMenuOpen = true;
        binding.fabAddNewPlaylist.setVisibility(View.VISIBLE);
        binding.fabRecognizePlaylist.setVisibility(View.VISIBLE);
        binding.fabMainPlaylists.animate().rotation(45f);
        // Anima o botão para a posição final definida em dimens.xml.
        binding.fabAddNewPlaylist.animate().translationY(-getResources().getDimension(R.dimen.fab_playlists_translation_y)).alpha(1f);
        binding.fabRecognizePlaylist.animate().translationY(-getResources().getDimension(R.dimen.fab_recognize_translation_y)).alpha(1f);
    }

    /**
     * Anima o fecho do menu FAB.
     */
    private void closeFabMenu() {
        isFabMenuOpen = false;
        binding.fabMainPlaylists.animate().rotation(0f);
        // Anima os botões de volta à sua posição original (translationY = 0) e esconde-os.
        binding.fabAddNewPlaylist.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabAddNewPlaylist.setVisibility(View.INVISIBLE));
        binding.fabRecognizePlaylist.animate().translationY(0f).alpha(0f).withEndAction(() -> binding.fabRecognizePlaylist.setVisibility(View.INVISIBLE));
    }

    // --- LÓGICA DA ACRCloud ---

    /**
     * Configura o cliente da ACRCloud com as credenciais da API.
     */
    private void setupAcrCloud() {
        acrCloudConfig = new ACRCloudConfig();
        acrCloudConfig.context = this;
        acrCloudConfig.host = "identify-eu-west-1.acrcloud.com";
        acrCloudConfig.accessKey = "ebdbf94f27585e9638cd1a46a50dd2cd";
        acrCloudConfig.accessSecret = "PLGnkKGkWNJWNkkeSfHut28SG5me338O4jcBmvwR";
        acrCloudConfig.acrcloudListener = this;
        // acrCloudConfig.recMode = ACRCloudConfig.RecorderType.REC_MODE_REMOTE; // Linha de configuração explícita.

        acrCloudClient = new ACRCloudClient();
        acrCloudClient.initWithConfig(acrCloudConfig);
    }

    /**
     * Verifica se a permissão para gravar áudio foi concedida. Se sim, inicia o reconhecimento.
     * Se não, pede a permissão ao utilizador.
     */
    private void checkAndStartRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognition();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 103);
        }
    }

    /**
     * Inicia o processo de reconhecimento de áudio e mostra o diálogo "A ouvir...".
     */
    private void startRecognition() {
        if (isRecognizing) return;
        isRecognizing = acrCloudClient.startRecognize();
        if (isRecognizing) showListeningDialog();
    }

    /**
     * Para o processo de reconhecimento. (Atualmente, a chamada acrCloudClient.stopRecognize() está comentada).
     */
    private void stopRecognition() {
        // if (isRecognizing && acrCloudClient != null) acrCloudClient.stopRecognize(); // O cancel() em onResult já trata disto.
        isRecognizing = false;
        if (recognitionDialog != null && recognitionDialog.isShowing()) recognitionDialog.dismiss();
    }

    /**
     * Callback do SDK da ACRCloud. Não é usado, pois os resultados vêm no outro 'onResult'.
     */
    @Override
    public void onVolumeChanged(double volume) {}

    /**
     * Carrega a lista de playlists do banco de dados e atualiza o adapter.
     * A operação é executada numa thread de background.
     */
    private void loadPlaylists() {
        executorService.execute(() -> {
            List<Playlist> playlists = db.playlistDao().getAllPlaylists();
            runOnUiThread(() -> adapter.setPlaylists(playlists));
        });
    }

    /**
     * Mostra o diálogo de sucesso após a identificação de uma música.
     */
    private void showSuccessDialog(String title, String artist) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Neutral)
                .setTitle("Música Encontrada")
                .setIcon(R.drawable.ic_check_circle)
                .setMessage("Título: " + title + "\nArtista: " + artist)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Mostra o diálogo de erro se a identificação falhar.
     */
    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Neutral)
                .setTitle("Não foi possível identificar")
                .setIcon(R.drawable.ic_error)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Callback principal da ACRCloud com o resultado do reconhecimento.
     * Este método é chamado numa thread de background pelo SDK.
     */
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

    /**
     * Callback do PlaylistAdapter quando uma playlist é clicada.
     * Abre a tela de detalhes da playlist.
     */
    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("PLAYLIST_ID", playlist.playlistId);
        intent.putExtra("PLAYLIST_NAME", playlist.name);
        startActivity(intent);
    }

    /**
     * Chamado quando a Activity é destruída. Liberta recursos.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (acrCloudClient != null) {
            acrCloudClient.release();
            acrCloudClient = null;
        }
    }

    /**
     * Callback do PlaylistAdapter quando uma playlist sofre um clique longo.
     * Mostra o diálogo de confirmação para apagar.
     */
    @Override
    public void onPlaylistLongClick(Playlist playlist) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Destructive)
                .setTitle("Apagar Playlist")
                .setMessage("Tem a certeza que quer apagar a playlist '" + playlist.name + "'? Esta ação não pode ser desfeita.")
                .setIcon(R.drawable.ic_delete_warning)
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Sim, apagar", (dialog, which) -> deletePlaylist(playlist))
                .show();
    }

    /**
     * Mostra o diálogo para criar uma nova playlist, com um campo de texto personalizado.
     */
    private void showCreatePlaylistDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Input);
        builder.setTitle("Nova Playlist");
        builder.setIcon(R.drawable.ic_playlist_add);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist, null);
        final TextInputEditText input = dialogView.findViewById(R.id.edit_text_playlist_name);
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

    /**
     * Mostra o diálogo personalizado "A ouvir...".
     */
    private void showListeningDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_AlertDialog_Input);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_listening, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        builder.setNegativeButton("Cancelar", (dialog, which) -> stopRecognition());
        recognitionDialog = builder.create();
        recognitionDialog.show();
    }
}