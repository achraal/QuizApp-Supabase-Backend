package com.example.quizapp_aitlahcen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.AudioViewHolder> {

    private List<String> audioUrls;
    private OnAudioClickListener listener;

    // Interface pour envoyer l'événement de clic à l'Activity
    public interface OnAudioClickListener {
        void onPlayClick(String url);
    }

    public AudioAdapter(List<String> audioUrls, OnAudioClickListener listener) {
        this.audioUrls = audioUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        String url = audioUrls.get(position);

        // Extraction du nom du fichier pour l'affichage
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        holder.tvAudioName.setText("Séquence : " + fileName);

        holder.btnPlay.setOnClickListener(v -> listener.onPlayClick(url));
    }

    @Override
    public int getItemCount() {
        return audioUrls.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName;
        ImageButton btnPlay;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAudioName = itemView.findViewById(R.id.tvAudioName);
            btnPlay = itemView.findViewById(R.id.btnPlayAudio);
        }
    }
}