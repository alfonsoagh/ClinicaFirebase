package com.example.clinicasx.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String PREFS = "sync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync";

    public static void enqueueNow(Context ctx){
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(ctx).enqueue(req);
    }

    public static String getLastSync(Context ctx, String uid){
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SYNC + "_" + uid, "1970-01-01T00:00:00Z");
    }

    public static void setLastSync(Context ctx, String uid, String iso){
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_SYNC + "_" + uid, iso).apply();
    }
}
