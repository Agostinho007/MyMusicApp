package com.dev.mymusicapp.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.dev.mymusicapp.R;
import com.dev.mymusicapp.adapter.SelectSongsAdapter;
import com.dev.mymusicapp.data.SongRepository;
import com.dev.mymusicapp.databinding.ActivitySelectSongsBinding;
import com.dev.mymusicapp.model.Song;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelectSongsActivity extends AppCompatActivity {

    private ActivitySelectSongsBinding binding;
    private SelectSongsAdapter adapter;
    private SongRepository songRepository;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectSongsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configura a Toolbar
        setSupportActionBar(binding.toolbarSelectSongs);
        // Faz o ícone de 'X' (fechar) funcionar
        binding.toolbarSelectSongs.setNavigationOnClickListener(v -> finish());

        // Inicializa os componentes
        songRepository = new SongRepository();
        executorService = Executors.newSingleThreadExecutor();
        setupRecyclerView();
        loadAllSongs();
    }

    private void setupRecyclerView() {
        adapter = new SelectSongsAdapter();
        binding.recyclerViewSelectSongs.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSelectSongs.setAdapter(adapter);
    }

    private void loadAllSongs() {
        // Busca todas as músicas do dispositivo em background
        executorService.execute(() -> {
            List<Song> allSongs = songRepository.getSongs(this);
            // Atualiza o adapter na thread principal
            runOnUiThread(() -> {
                adapter.setSongs(allSongs);
            });
        });
    }

    // --- Lógica do Menu ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_songs_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_done_selecting) {
            returnSelectedSongs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Método para finalizar a activity e devolver os dados
    private void returnSelectedSongs() {
        // Pede ao adapter a lista de músicas selecionadas
        ArrayList<Song> selectedSongs = adapter.getSelectedSongs();

        // Cria um intent para o resultado
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_SONGS", (Serializable) selectedSongs);

        // Define o resultado como OK e envia os dados
        setResult(Activity.RESULT_OK, resultIntent);

        // Fecha esta tela
        finish();
    }
}