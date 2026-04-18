package com.example.quizapp_aitlahcen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private List<String> photoUrls;
    private String adminToken;

    public PhotoAdapter(List<String> photoUrls, String adminToken) { // <--- Modifier le constructeur
        this.photoUrls = photoUrls;
        this.adminToken = adminToken;
    }

    public PhotoAdapter(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    // Dans PhotoAdapter.java
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = photoUrls.get(position);

        // On crée une URL sécurisée avec les clés d'accès
        GlideUrl secureUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer " + adminToken) // Le token de l'admin
                .build());

        /*Glide.with(holder.itemView.getContext())
                .load(secureUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.ivPhoto);*/
        Glide.with(holder.itemView.getContext())
                .load(secureUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache l'image originale ET la version redimensionnée
                .override(300, 300) // Redimensionne l'image AVANT de l'afficher (gain de RAM énorme)
                .thumbnail(0.1f) // Charge une version très basse résolution en attendant la vraie
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);
    }

    @Override
    public int getItemCount() { return photoUrls.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhotoItem);
        }
    }
}