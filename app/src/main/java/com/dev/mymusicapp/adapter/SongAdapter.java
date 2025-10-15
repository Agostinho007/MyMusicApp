package com.dev.mymusicapp.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.mymusicapp.R;
import com.dev.mymusicapp.databinding.ListItemSongBinding;
import com.dev.mymusicapp.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * SongAdapter é a classe responsável por adaptar uma lista de objetos 'Song' para ser exibida
 * num RecyclerView. Ele gere a criação das views para cada música, a vinculação dos dados
 * e a interação do utilizador (cliques).
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    // A lista de músicas que o adapter está a exibir no momento.
    private List<Song> songs = new ArrayList<>();

    // Listeners para comunicar eventos de clique de volta para a Activity/Fragment.
    private final OnSongClickListener clickListener;
    private final OnSongLongClickListener longClickListener;

    // Armazena o ID (caminho do ficheiro) da música que está a tocar atualmente, para fins de destaque.
    private String currentPlayingSongPath = null;

    /**
     * Interface para o clique normal (curto). Passa o objeto Song e a View da capa do álbum,
     * que é usada para a animação de transição (Shared Element Transition).
     */
    public interface OnSongClickListener {
        void onSongClick(Song song, View albumArtView);
    }

    /**
     * Interface para o clique longo.
     */
    public interface OnSongLongClickListener {
        void onSongLongClick(Song song);
    }

    /**
     * Construtor do adapter que recebe as implementações dos listeners.
     */
    public SongAdapter(OnSongClickListener clickListener, OnSongLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    /**
     * Metodo chamado pela Activity/Presenter para informar ao adapter qual música está a tocar.
     * @param songPath O dataPath da música atual.
     */
    public void setCurrentPlayingSong(String songPath) {
        this.currentPlayingSongPath = songPath;
        // Notifica o adapter que os dados mudaram, para que ele possa redesenhar a lista e aplicar o destaque.
        notifyDataSetChanged();
    }

    /**
     * Chamado pelo RecyclerView quando precisa de uma nova View para um item.
     */
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout 'list_item_song.xml' usando ViewBinding.
        ListItemSongBinding binding = ListItemSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SongViewHolder(binding);
    }

    /**
     * Chamado pelo RecyclerView para associar os dados de uma música a uma View específica.
     */
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        // Passa o objeto Song, os listeners e o ID da música atual para o ViewHolder.
        holder.bind(songs.get(position), clickListener, longClickListener, currentPlayingSongPath);
    }

    /**
     * Retorna o número total de itens na lista.
     */
    @Override
    public int getItemCount() {
        // A verificação 'songs != null' é uma salvaguarda para evitar crashes.
        return songs != null ? songs.size() : 0;
    }

    /**
     * Atualiza a lista de músicas do adapter.
     */
    public void setSongs(List<Song> songs) {
        // Garante que a lista interna nunca seja nula.
        if (songs == null) {
            this.songs = new ArrayList<>();
        } else {
            this.songs = songs;
        }
        notifyDataSetChanged();
    }

    /**
     * Retorna a lista de músicas que o adapter está a usar no momento.
     */
    public List<Song> getSongs() {
        return songs;
    }

    /**
     * A classe ViewHolder que representa a view de um único item na lista.
     */
    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ListItemSongBinding binding;

        public SongViewHolder(ListItemSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Preenche a UI do item com os dados da música e configura os listeners.
         */
        public void bind(final Song song, final OnSongClickListener clickListener, final OnSongLongClickListener longClickListener, String currentPlayingSongPath) {
            // Define o texto do título e do artista.
            binding.textViewTitle.setText(song.getTitle());
            binding.textViewArtist.setText(song.getArtist());

            // Configura o listener de clique curto.
            // Passa a referência da ImageView para permitir a animação de transição.
            itemView.setOnClickListener(v -> clickListener.onSongClick(song, binding.albumArtImageViewItem));

            // Configura o listener de clique longo.
            itemView.setOnLongClickListener(v -> {
                // Só chama o listener se ele tiver sido fornecido (não for nulo).
                if (longClickListener != null) {
                    longClickListener.onSongLongClick(song);
                    return true; // Retorna 'true' para indicar que o evento foi tratado.
                }
                return false;
            });

            // Usa a biblioteca Glide para carregar a imagem da capa do álbum.
            Glide.with(itemView.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note) // Imagem a ser mostrada enquanto carrega.
                    .error(R.drawable.ic_music_note)       // Imagem a ser mostrada se houver erro ou não houver capa.
                    .circleCrop()                          // Aplica um corte circular à imagem.
                    .into(binding.albumArtImageViewItem);  // A ImageView onde a imagem será exibida.

            // Lógica para destacar a música que está a tocar.
            if (song.getDataPath().equals(currentPlayingSongPath)) {
                // Se o ID desta música é o mesmo da que está a tocar, muda a cor do texto.
                binding.textViewTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.teal_200));
            } else {
                // Senão, usa a cor padrão (branco, para o tema escuro).
                binding.textViewTitle.setTextColor(Color.WHITE);
            }
        }
    }
}