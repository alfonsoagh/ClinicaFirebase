package com.example.clinicasx.core;

import android.app.Application;
import android.content.Context;
<<<<<<< HEAD
=======
import android.content.res.AssetManager;
>>>>>>> b13913cb9112d657361d7d7debe8e152d43fb196
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.clinicasx.sync.SyncWorker;
import com.google.firebase.FirebaseApp;
<<<<<<< HEAD

=======
import com.google.firebase.FirebaseOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
>>>>>>> b13913cb9112d657361d7d7debe8e152d43fb196
import java.util.concurrent.TimeUnit;

public class ClinicaApp extends Application {
    private static final String TAG = "ClinicaApp";
    private static final String PERIODIC_WORK_NAME = "sync_pacientes_periodic";

    @Override
    public void onCreate() {
        super.onCreate();
<<<<<<< HEAD
        ensureFirebaseInitialized(this);
=======
        initFirebaseFromAssets(this);
>>>>>>> b13913cb9112d657361d7d7debe8e152d43fb196
        schedulePeriodicSync();
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                6, TimeUnit.HOURS
        ).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        );
    }

<<<<<<< HEAD
    // Inicializa Firebase usando la configuración generada por google-services.json
    private void ensureFirebaseInitialized(@NonNull Context ctx) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseApp.initializeApp(ctx);
                Log.i(TAG, "Firebase inicializado con google-services.json");
            } else {
                Log.d(TAG, "Firebase ya estaba inicializado");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error inicializando Firebase: " + e.getMessage());
=======
    private void initFirebaseFromAssets(@NonNull Context ctx) {
        try {
            if (!FirebaseApp.getApps(ctx).isEmpty()) {
                return; // ya inicializado
            }
            AssetManager am = ctx.getAssets();
            InputStream is = am.open("firebase_config.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject json = new JSONObject(sb.toString());

            String appId = json.optString("applicationId");
            String apiKey = json.optString("apiKey");
            String projectId = json.optString("projectId");
            String storageBucket = json.optString("storageBucket");

            FirebaseOptions.Builder builder = new FirebaseOptions.Builder()
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setProjectId(projectId);
            if (!storageBucket.isEmpty()) builder.setStorageBucket(storageBucket);

            FirebaseOptions options = builder.build();
            FirebaseApp.initializeApp(ctx, options);
            Log.i(TAG, "Firebase inicializado desde assets");
        } catch (Exception e) {
            Log.w(TAG, "No se pudo inicializar Firebase desde assets: " + e.getMessage());
>>>>>>> b13913cb9112d657361d7d7debe8e152d43fb196
        }
    }
}
