package com.dev.mymusicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.dev.mymusicapp.R;
import com.dev.mymusicapp.databinding.SelectSongItemBinding;
import com.dev.mymusicapp.model.Song;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Este adapter é responsável por exibir uma lista de músicas onde o utilizador pode selecionar
 * múltiplas faixas através de checkboxes.
 */
public class SelectSongsAdapter extends RecyclerView.Adapter<SelectSongsAdapter.SelectSongViewHolder> {

    // A lista completa de todas as músicas disponíveis para seleção.
    private final List<Song> allSongs = new ArrayList<>();

    // Um 'Set' é usado para armazenar os identificadores únicos (dataPath) das músicas selecionadas.
    // Usar um Set é muito eficiente para adicionar, remover e verificar se um item existe.
    private final Set<String> selectedSongPaths = new HashSet<>();

    /**
     * Metodo público para a Activity popular o adapter com a lista de todas as músicas.
     * @param songs A lista de músicas a ser exibida.
     */
    public void setSongs(List<Song> songs) {
        allSongs.clear(); // Limpa a lista antiga
        allSongs.addAll(songs); // Adiciona todas as novas músicas
        notifyDataSetChanged(); // Notifica o RecyclerView para se redesenhar
    }

    /**
     * Retorna uma lista contendo apenas os objetos Song que foram selecionados pelo utilizador.
     * Este metodo é chamado pela Activity quando o utilizador confirma a sua seleção.
     * @return Uma ArrayList de Songs selecionadas.
     */
    public ArrayList<Song> getSelectedSongs() {
        ArrayList<Song> selectedSongs = new ArrayList<>();
        // Percorre todas as músicas
        for (Song song : allSongs) {
            // Se o caminho da música estiver no nosso Set de selecionados, adiciona-a à lista de retorno.
            if (selectedSongPaths.contains(song.getDataPath())) {
                selectedSongs.add(song);
            }
        }
        return selectedSongs;
    }

    /**
     * Cria uma nova View para um item da lista quando o RecyclerView precisa de uma.
     */
    @NonNull
    @Override
    public SelectSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o layout 'select_song_item.xml' usando ViewBinding.
        SelectSongItemBinding binding = SelectSongItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SelectSongViewHolder(binding);
    }

    /**
     * Vincula os dados de uma música específica a uma View (ViewHolder).
     */
    @Override
    public void onBindViewHolder(@NonNull SelectSongViewHolder holder, int position) {
        // Obtém a música para a posição atual.
        Song currentSong = allSongs.get(position);

        // Chama o metodo 'bind' do ViewHolder, passando a música e indicando se ela está selecionada.
        holder.bind(currentSong, selectedSongPaths.contains(currentSong.getDataPath()));

        // Configura o listener de clique para a linha inteira do item.
        holder.itemView.setOnClickListener(v -> {
            // Inverte o estado de seleção da música.
            if (selectedSongPaths.contains(currentSong.getDataPath())) {
                // Se já estava selecionada, remove do Set.
                selectedSongPaths.remove(currentSong.getDataPath());
            } else {
                // Se não estava selecionada, adiciona ao Set.
                selectedSongPaths.add(currentSong.getDataPath());
            }
            // Notifica o adapter que APENAS este item mudou. É mais eficiente que 'notifyDataSetChanged()',
            // pois redesenha apenas esta linha, permitindo uma animação suave da checkbox.
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    /**
     * Retorna o número total de músicas na lista.
     */
    @Override
    public int getItemCount() {
        return allSongs.size();
    }

    /**
     * ViewHolder para a nossa lista de seleção. Ele armazena as referências das Views de cada item.
     */
    static class SelectSongViewHolder extends RecyclerView.ViewHolder {
        private final SelectSongItemBinding binding;

        public SelectSongViewHolder(SelectSongItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Preenche a UI do item com os dados da música.
         * @param song O objeto Song a ser exibido.
         * @param isSelected True se o item deve ser mostrado como selecionado (checkbox marcada).
         */
        public void bind(Song song, boolean isSelected) {
            // Define o texto do título e do artista.
            binding.textViewTitle.setText(song.getTitle());
            binding.textViewArtist.setText(song.getArtist());

            // Define o estado da checkbox (marcada ou desmarcada) com base no parâmetro 'isSelected'.
            binding.checkboxSelectSong.setChecked(isSelected);

            // Usa o Glide para carregar a capa do álbum.
            Glide.with(itemView.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note) // Imagem padrão enquanto carrega
                    .error(R.drawable.ic_music_note)       // Imagem padrão em caso de erro
                    .circleCrop()                          // Torna a imagem circular
                    .into(binding.albumArtImageViewItem);
        }
    }
}
