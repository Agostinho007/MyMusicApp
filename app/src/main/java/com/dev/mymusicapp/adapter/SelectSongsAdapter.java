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

public class SelectSongsAdapter extends RecyclerView.Adapter<SelectSongsAdapter.SelectSongViewHolder> {

    private final List<Song> allSongs = new ArrayList<>();
    private final Set<String> selectedSongPaths = new HashSet<>();

    public void setSongs(List<Song> songs) {
        allSongs.clear();
        allSongs.addAll(songs);
        notifyDataSetChanged();
    }

    public ArrayList<Song> getSelectedSongs() {
        ArrayList<Song> selectedSongs = new ArrayList<>();
        for (Song song : allSongs) {
            if (selectedSongPaths.contains(song.getDataPath())) {
                selectedSongs.add(song);
            }
        }
        return selectedSongs;
    }

    @NonNull
    @Override
    public SelectSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SelectSongItemBinding binding = SelectSongItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SelectSongViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectSongViewHolder holder, int position) {
        Song currentSong = allSongs.get(position);
        holder.bind(currentSong, selectedSongPaths.contains(currentSong.getDataPath()));

        // Listener de clique para a linha inteira
        holder.itemView.setOnClickListener(v -> {
            // Inverte o estado de seleção da música
            if (selectedSongPaths.contains(currentSong.getDataPath())) {
                selectedSongPaths.remove(currentSong.getDataPath());
            } else {
                selectedSongPaths.add(currentSong.getDataPath());
            }
            // Notifica o adapter que este item mudou, para redesenhar a checkbox
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return allSongs.size();
    }

    static class SelectSongViewHolder extends RecyclerView.ViewHolder {
        private final SelectSongItemBinding binding;

        public SelectSongViewHolder(SelectSongItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Song song, boolean isSelected) {
            binding.textViewTitle.setText(song.getTitle());
            binding.textViewArtist.setText(song.getArtist());

            // Define o estado da checkbox com base na nossa lista de selecionados
            binding.checkboxSelectSong.setChecked(isSelected);

            Glide.with(itemView.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .circleCrop()
                    .into(binding.albumArtImageViewItem);
        }
    }
}
