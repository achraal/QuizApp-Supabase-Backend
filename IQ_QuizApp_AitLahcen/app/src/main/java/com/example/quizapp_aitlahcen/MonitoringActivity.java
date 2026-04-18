package com.example.quizapp_aitlahcen;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MonitoringActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String studentId, studentName, adminToken;
    private TextView tvStudentTitle, tvAddress;
    private RecyclerView rvPhotos;
    private PhotoAdapter photoAdapter;
    private List<String> photoUrls = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private List<String> audioUrls = new ArrayList<>();
    private AudioAdapter audioAdapter; // À créer (voir plus bas)
    private RecyclerView rvAudios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        studentId = getIntent().getStringExtra("STUDENT_ID");
        studentName = getIntent().getStringExtra("STUDENT_NAME");
        adminToken = getIntent().getStringExtra("TOKEN");

        tvStudentTitle = findViewById(R.id.tvStudentTitle);
        tvAddress = findViewById(R.id.tvAddress);
        tvStudentTitle.setText("Suivi de : " + studentName);

        rvPhotos = findViewById(R.id.rvPhotos);
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // On initialise l'adapter (tu devras créer une petite classe PhotoAdapter)
        //photoAdapter = new PhotoAdapter(photoUrls);
        photoAdapter = new PhotoAdapter(photoUrls, adminToken);
        rvPhotos.setAdapter(photoAdapter);

        rvAudios = findViewById(R.id.rvAudios);
        rvAudios.setLayoutManager(new LinearLayoutManager(this));

        audioAdapter = new AudioAdapter(audioUrls, url -> {
            // Ici, on appelle la méthode de lecture qu'on a codée avant
            playAudio(url);
        });
        rvAudios.setAdapter(audioAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        loadMonitoringData();
        loadPhotosFromStorage();
        fetchUserAudios();
    }

    private void loadMonitoringData() {
        String url = SupabaseConfig.BASE_URL + "/rest/v1/monitoring?user_id=eq." + studentId + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer " + adminToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    MonitoringRecord[] records = new Gson().fromJson(json, MonitoringRecord[].class);

                    if (records != null && records.length > 0) {
                        runOnUiThread(() -> {
                            // ON NE TOUCHE PAS à photoUrls ici, on s'occupe juste de la carte
                            for (MonitoringRecord r : records) {
                                updateMap(r.latitude, r.longitude);
                            }

                            // Zoom sur le dernier point connu
                            MonitoringRecord last = records[records.length - 1];
                            tvAddress.setText("Dernière adresse : " + last.address);
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(last.latitude, last.longitude), 15f));
                        });
                    }
                }
            }
            @Override
            public void onFailure(Call call, IOException e) { Log.e("Map", "Erreur", e); }
        });
    }
    private void loadPhotosFromStorage() {
        // 1. L'URL pour LISTER (Toujours celle-ci pour le POST)
        String listUrl = SupabaseConfig.BASE_URL + "/storage/v1/object/list/monitoring";

        String jsonBody = "{\"prefix\": \"" + studentId + "/photos/\", \"limit\": 100, \"offset\": 0}";
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"), jsonBody);

        Request request = new Request.Builder()
                .url(listUrl) // <--- On utilise l'URL de liste
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer " + adminToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    StorageFile[] files = new Gson().fromJson(json, StorageFile[].class);

                    if (files != null) {
                        photoUrls.clear();
                        // 2. L'URL pour l'AFFICHAGE (Authenticated car bucket privé)
                        String displayBaseUrl = SupabaseConfig.BASE_URL + "/storage/v1/object/authenticated/monitoring/" + studentId + "/photos/";

                        for (StorageFile file : files) {
                            if (!file.name.equals(".emptyFolderPlaceholder")) {
                                photoUrls.add(displayBaseUrl + file.name);
                            }
                        }
                        runOnUiThread(() -> photoAdapter.notifyDataSetChanged());
                    }
                } else {
                    Log.e("STORAGE_ERROR", response.code() + " : " + response.message());
                }
            }
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }
        });
    }

    private void updateMap(double lat, double lng) {
        if (mMap != null) {
            LatLng pos = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions().position(pos).title(studentName));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
    private void fetchUserAudios() {
        String listUrl = SupabaseConfig.BASE_URL + "/storage/v1/object/list/monitoring";
        String jsonBody = "{\"prefix\": \"" + studentId + "/audios/\", \"limit\": 100, \"offset\": 0}";

        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json"), jsonBody);

        Request request = new Request.Builder()
                .url(listUrl)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer " + adminToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    StorageFile[] files = new Gson().fromJson(json, StorageFile[].class);

                    if (files != null) {
                        audioUrls.clear();
                        // Utilisation de l'endpoint AUTHENTICATED
                        String displayBaseUrl = SupabaseConfig.BASE_URL + "/storage/v1/object/authenticated/monitoring/" + studentId + "/audios/";

                        for (StorageFile file : files) {
                            if (!file.name.equals(".emptyFolderPlaceholder")) {
                                audioUrls.add(displayBaseUrl + file.name);
                            }
                        }
                        runOnUiThread(() -> audioAdapter.notifyDataSetChanged());
                    }
                }
            }
            @Override
            public void onFailure(Call call, IOException e) { Log.e("AUDIO_ADMIN", "Erreur liste", e); }
        });
    }
    private void playAudio(String authenticatedUrl) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        // Création des headers pour l'authentification
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("apikey", SupabaseConfig.API_KEY);
        headers.put("Authorization", "Bearer " + adminToken);

        try {
            // Android permet de passer des headers via un Uri
            android.net.Uri uri = android.net.Uri.parse(authenticatedUrl);
            mediaPlayer.setDataSource(this, uri, headers);

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(this, "Lecture sécurisée...", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
            });

        } catch (IOException e) {
            Log.e("ADMIN_PLAY", "Erreur lecture sécurisée: " + e.getMessage());
        }
    }
}