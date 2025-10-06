package com.dev.mymusicapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.dev.mymusicapp.databinding.PlaylistItemBinding;
import com.dev.mymusicapp.model.Playlist;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists = new ArrayList<>();
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistLongClickListener longClickListener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    //Interface para clique longo
    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }

    // Construtor
    public PlaylistAdapter(OnPlaylistClickListener clickListener, OnPlaylistLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PlaylistItemBinding binding = PlaylistItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PlaylistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        holder.bind(playlists.get(position), clickListener, longClickListener);
    }

    @Override
    public int getItemCount() { return playlists.size(); }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final PlaylistItemBinding binding;

        public PlaylistViewHolder(PlaylistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final Playlist playlist, final OnPlaylistClickListener clickListener, final OnPlaylistLongClickListener longClickListener) {
            binding.playlistName.setText(playlist.name);
            itemView.setOnClickListener(v -> clickListener.onPlaylistClick(playlist));
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onPlaylistLongClick(playlist);
                    return true;
                }
                return false;
            });
        }
    }
}