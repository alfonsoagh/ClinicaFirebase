package com.example.clinicasx.core;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.clinicasx.sync.SyncWorker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ClinicaApp extends Application {
    private static final String TAG = "ClinicaApp";
    private static final String PERIODIC_WORK_NAME = "sync_pacientes_periodic";

    @Override
    public void onCreate() {
        super.onCreate();
        initFirebaseFromAssets(this);
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
        }
    }
}
