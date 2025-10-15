package com.dev.mymusicapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dev.mymusicapp.databinding.PlaylistItemBinding;
import com.dev.mymusicapp.model.Playlist;
import java.util.ArrayList;
import java.util.List;

/**
 * PlaylistAdapter é uma classe que "adapta" uma lista de objetos 'Playlist' para ser exibida
 * num RecyclerView. Ele é o responsável por criar e gerir as Views de cada item da lista.
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    // A lista de dados (playlists) que o adapter vai exibir.
    // É inicializada como uma lista vazia para evitar NullPointerExceptions.
    private List<Playlist> playlists = new ArrayList<>();

    // Interfaces para comunicar eventos de clique de volta para a Activity/Fragment.
    // Este é o padrão "listener" ou "callback".
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistLongClickListener longClickListener;

    /**
     * Interface para ser implementada pela Activity que quer ouvir por cliques normais (curtos).
     */
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    /**
     * Interface para ser implementada pela Activity que quer ouvir por cliques longos.
     */
    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }

    /**
     * Construtor do adapter. Ele recebe as implementações dos listeners
     * da Activity que o está a usar.
     * @param clickListener O listener para cliques curtos.
     * @param longClickListener O listener para cliques longos.
     */
    public PlaylistAdapter(OnPlaylistClickListener clickListener, OnPlaylistLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    /**
     * Este metodo é chamado pelo RecyclerView quando ele precisa criar uma nova View para um item.
     * @return Um novo PlaylistViewHolder que contém a View para cada item.
     */
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla (cria) o layout do item da lista (playlist_item.xml) usando ViewBinding.
        PlaylistItemBinding binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        // Cria e retorna uma instância do ViewHolder com o layout inflado.
        return new PlaylistViewHolder(binding);
    }

    /**
     * Este metodo é chamado pelo RecyclerView para exibir os dados numa posição específica.
     * Ele reutiliza as Views que já foram criadas.
     * holder O ViewHolder que deve ser atualizado.
     * position A posição do item na lista de dados.
     */
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        // Obtém o objeto Playlist da nossa lista de dados na posição correta.
        Playlist currentPlaylist = playlists.get(position);
        // Chama o metodo 'bind' do ViewHolder para preencher a View com os dados da playlist.
        holder.bind(currentPlaylist, clickListener, longClickListener);
    }

    /**
     * Retorna o número total de itens na lista de dados.
     * O RecyclerView usa este metodo para saber quantos itens precisa de desenhar.
     */
    @Override
    public int getItemCount() {
        return playlists.size();
    }

    /**
     * Metodo público para atualizar a lista de playlists dentro do adapter.
     * @param playlists A nova lista de playlists a ser exibida.
     */
    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        // Notifica o RecyclerView que os dados mudaram, para que ele possa redesenhar a lista.
        notifyDataSetChanged();
    }

    /**
     * PlaylistViewHolder é uma classe interna que representa a View de um único item na lista.
     * Ele guarda as referências para os componentes da UI (ex: TextViews) para evitar
     * chamadas repetitivas a 'findViewById'.
     */
    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        // Objeto de ViewBinding que contém as referências para as Views do layout do item.
        private final PlaylistItemBinding binding;

        public PlaylistViewHolder(PlaylistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Preenche os componentes da View com os dados de um objeto Playlist
         * e configura os listeners de clique.
         */
        public void bind(final Playlist playlist, final OnPlaylistClickListener clickListener, final OnPlaylistLongClickListener longClickListener) {
            // Define o nome da playlist no TextView correspondente.
            binding.playlistName.setText(playlist.name);

            // Configura o listener para um clique curto em toda a View do item.
            itemView.setOnClickListener(v -> clickListener.onPlaylistClick(playlist));

            // Configura o listener para um clique longo.
            itemView.setOnLongClickListener(v -> {
                // Verifica se a Activity forneceu um listener para o clique longo.
                if (longClickListener != null) {
                    // Chama o metodo do listener, passando a playlist que foi clicada.
                    longClickListener.onPlaylistLongClick(playlist);
                    // Retorna 'true' para indicar que o evento de clique longo foi "consumido"
                    // e não deve acionar um clique curto a seguir.
                    return true;
                }
                return false;
            });
        }
    }
}