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

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private final OnSongClickListener clickListener;
    private final OnSongLongClickListener longClickListener;
    private String currentPlayingSongPath = null;

    // Interface para clique normal
    public interface OnSongClickListener {
        void onSongClick(Song song, View albumArtView);
    }

    // Interface para clique longo
    public interface OnSongLongClickListener {
        void onSongLongClick(Song song);
    }

    public SongAdapter(OnSongClickListener clickListener, OnSongLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setCurrentPlayingSong(String songPath) {
        this.currentPlayingSongPath = songPath;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemSongBinding binding = ListItemSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SongViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        holder.bind(songs.get(position), clickListener, longClickListener, currentPlayingSongPath);
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void setSongs(List<Song> songs) {
        if (songs == null) {
            this.songs = new ArrayList<>();
        } else {
            this.songs = songs;
        }
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return songs;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ListItemSongBinding binding;

        public SongViewHolder(ListItemSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Song song, final OnSongClickListener clickListener, final OnSongLongClickListener longClickListener, String currentPlayingSongPath) {
            binding.textViewTitle.setText(song.getTitle());
            binding.textViewArtist.setText(song.getArtist());

            // Configura o clique normal
            itemView.setOnClickListener(v -> clickListener.onSongClick(song, binding.albumArtImageViewItem));

            // Configura o clique longo
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onSongLongClick(song);
                    return true; // Retorna true para indicar que o evento foi consumido
                }
                return false;
            });

            Glide.with(itemView.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .circleCrop()
                    .into(binding.albumArtImageViewItem);

            if (song.getDataPath().equals(currentPlayingSongPath)) {
                binding.textViewTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.teal_200));
            } else {
                binding.textViewTitle.setTextColor(Color.WHITE);
            }
        }
    }
}