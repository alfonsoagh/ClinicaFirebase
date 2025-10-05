package com.example.clinicasx.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.example.clinicasx.db.SQLite;
import com.example.clinicasx.model.Patient;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.i(TAG, "Sin usuario autenticado; se omite sync");
                return Result.success();
            }
            String uid = user.getUid();

            FirebaseFirestore dbRemote = FirebaseFirestore.getInstance();
            CollectionReference col = dbRemote.collection("users").document(uid).collection("pacientes");
            FirebaseStorage storage = FirebaseStorage.getInstance();

            SQLite local = new SQLite(getApplicationContext());
            local.setUserId(uid);
            local.abrir();

            // PUSH: subir cambios locales sucios del usuario actual
            Cursor dirty = local.getDirty();
            if (dirty != null && dirty.moveToFirst()) {
                do {
                    Patient p = Patient.fromCursor(dirty);
                    String now = Instant.now().toString();
                    p.updatedAt = now; // normalizamos el timestamp antes de mandar

                    // Subir imagen si es ruta local (file:// o content://)
                    if (p.imagen != null && !p.imagen.isEmpty() && !"N/A".equalsIgnoreCase(p.imagen) && !p.imagen.startsWith("http")) {
                        try {
                            StorageReference ref = storage.getReference().child("users/" + uid + "/images/" + p.id + "/");
                            Uri uri = Uri.parse(p.imagen);
                            if ("content".equalsIgnoreCase(uri.getScheme()) || "file".equalsIgnoreCase(uri.getScheme())) {
                                // Usa el nombre original si existe, si no, uno por defecto
                                String name = (uri.getLastPathSegment() == null ? ("img_" + p.id + ".jpg") : uri.getLastPathSegment());
                                StorageReference dst = ref.child(name);
                                Tasks.await(dst.putFile(uri));
                                Uri dl = Tasks.await(dst.getDownloadUrl());
                                p.imagen = dl.toString();
                            } else {
                                File f = new File(p.imagen);
                                if (f.exists()) {
                                    StorageReference dst = ref.child(f.getName());
                                    Uri fileUri = Uri.fromFile(f);
                                    Tasks.await(dst.putFile(fileUri));
                                    Uri dl = Tasks.await(dst.getDownloadUrl());
                                    p.imagen = dl.toString();
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "No se pudo subir imagen: " + e.getMessage());
                        }
                    }

                    Map<String, Object> data = new HashMap<>(p.toMap());
                    Tasks.await(col.document(String.valueOf(p.id)).set(data));

                    // Histórico: guardar snapshot en subcolección
                    try {
                        DocumentReference dr = col.document(String.valueOf(p.id));
                        Tasks.await(dr.collection("history").add(data));
                    } catch (Exception hx) {
                        Log.w(TAG, "No se pudo guardar histórico: " + hx.getMessage());
                    }

                    // Marcamos sincronizado con el mismo updatedAt (y asignamos USER_ID)
                    local.markSynced(p.id, now);
                } while (dirty.moveToNext());
                dirty.close();
            }

            // PULL: traer cambios remotos desde la última sincronización del usuario
            String last = SyncManager.getLastSync(getApplicationContext(), uid);
            String nowPull = Instant.now().toString();
            QuerySnapshot qs = Tasks.await(col.whereGreaterThan("updatedAt", last).get());
            List<DocumentSnapshot> docs = qs.getDocuments();
            for (DocumentSnapshot d : docs) {
                try {
                    int id = Integer.parseInt(String.valueOf(d.get("id")));
                    String area = safeStr(d.get("area"));
                    String doctor = safeStr(d.get("doctor"));
                    String nombre = safeStr(d.get("nombre"));
                    String sexo = safeStr(d.get("sexo"));
                    String fechaIngreso = safeStr(d.get("fechaIngreso"));
                    String edad = safeStr(d.get("edad"));
                    String estatura = safeStr(d.get("estatura"));
                    String peso = safeStr(d.get("peso"));
                    String imagen = safeStr(d.get("imagen"));
                    String updatedAt = safeStr(d.get("updatedAt"));
                    boolean deleted = Boolean.TRUE.equals(d.getBoolean("deleted"));
                    local.upsertFromRemote(id, area, doctor, nombre, sexo, edad, estatura, peso, imagen, fechaIngreso, updatedAt, deleted);

                    // Prefetch a caché de Glide para disponibilidad offline
                    prefetchImageToCache(imagen);
                } catch (Exception ex) {
                    Log.w(TAG, "Error procesando doc remoto: " + ex.getMessage());
                }
            }
            SyncManager.setLastSync(getApplicationContext(), uid, nowPull);

            local.cerrar();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error en SyncWorker", e);
            return Result.retry();
        }
    }

    private static String safeStr(Object o){
        return o == null ? "" : String.valueOf(o);
    }

    private void prefetchImageToCache(String url){
        try {
            if (url != null && url.startsWith("http")) {
                FutureTarget<java.io.File> future = Glide.with(getApplicationContext())
                        .downloadOnly()
                        .load(url)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                // Bloquea hasta descargar a caché de disco
                future.get();
                // Opcional: Glide.with(...).clear(future); // liberar el target
            }
        } catch (Exception e) {
            Log.w(TAG, "Prefetch imagen falló: " + e.getMessage());
        }
    }
}
