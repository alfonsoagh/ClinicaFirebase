package com.example.clinicasx.util;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.clinicasx.db.SQLite;
import com.example.clinicasx.model.Patient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {
    private static final String TAG = "JsonUtils";
    private static final String FILE_NAME = "pacientes_export.json";

    public static File exportAll(Context ctx) throws Exception {
        SQLite local = new SQLite(ctx);
        local.abrir();
        Cursor c = local.getRegistros();
        List<Patient> list = new ArrayList<>();
        if (c != null && c.moveToFirst()) {
            do { list.add(Patient.fromCursor(c)); } while (c.moveToNext());
            c.close();
        }
        local.cerrar();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(list);
        File dir = ctx.getExternalFilesDir(null);
        if (dir == null) throw new IllegalStateException("No external files dir");
        File out = new File(dir, FILE_NAME);
        try (FileWriter fw = new FileWriter(out)) { fw.write(json); }
        return out;
    }

    public static int importAll(Context ctx) throws Exception {
        File dir = ctx.getExternalFilesDir(null);
        if (dir == null) throw new IllegalStateException("No external files dir");
        File in = new File(dir, FILE_NAME);
        if (!in.exists()) throw new IllegalStateException("Archivo no encontrado: " + in.getAbsolutePath());
        Gson gson = new Gson();
        Patient[] arr;
        try (FileReader fr = new FileReader(in)) {
            arr = gson.fromJson(fr, Patient[].class);
        }
        if (arr == null) return 0;
        SQLite local = new SQLite(ctx);
        local.abrir();
        int count = 0;
        for (Patient p : arr) {
            try {
                local.upsertFromRemote(p.id, p.area, p.doctor, p.nombre, p.sexo, p.edad, p.estatura, p.peso, p.imagen, p.fechaIngreso, p.updatedAt, p.deleted);
                count++;
            } catch (Exception e) {
                Log.w(TAG, "Error importando id=" + p.id + ": " + e.getMessage());
            }
        }
        local.cerrar();
        return count;
    }
}

