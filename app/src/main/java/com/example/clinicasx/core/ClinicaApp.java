package com.example.clinicasx.core;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.clinicasx.sync.SyncWorker;
import com.google.firebase.FirebaseApp;

import java.util.concurrent.TimeUnit;

public class ClinicaApp extends Application {
    private static final String TAG = "ClinicaApp";
    private static final String PERIODIC_WORK_NAME = "sync_pacientes_periodic";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureFirebaseInitialized(this);
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

    // Inicializa Firebase usando la configuración generada por google-services.json
    private void ensureFirebaseInitialized(@NonNull Context ctx) {
        try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                FirebaseApp.initializeApp(ctx);
                Log.i(TAG, "Firebase inicializado con google-services.json");
            } else {
                Log.d(TAG, "Firebase ya estaba inicializado");
            }
            Log.i(TAG, "Firebase inicializado desde assets");
        } catch (Exception e) {
            Log.w(TAG, "No se pudo inicializar Firebase desde assets: " + e.getMessage());
        }
    }
}
