package com.example.clinicasx.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQL extends SQLiteOpenHelper {
    private static final String database = "pacientes";
    private static final int VRS = 4; // nueva versión: añade USER_ID

    private static final String tPacientes = "CREATE TABLE IF NOT EXISTS PACIENTES(" +
            "ID INTEGER PRIMARY KEY NOT NULL, " +
            "AREA TEXT NOT NULL, " +
            "DOCTOR TEXT NOT NULL, " +
            "NOMBRE TEXT NOT NULL, " +
            "SEXO TEXT NOT NULL, " +
            "FECHA_INGRESO TEXT NOT NULL, " +
            "EDAD TEXT NOT NULL, " +
            "ESTATURA TEXT NOT NULL, " +
            "PESO TEXT NOT NULL, " +
            "IMAGEN TEXT NOT NULL, " +
            // columnas de sincronización
            "UPDATED_AT TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z', " +
            "DIRTY INTEGER NOT NULL DEFAULT 0, " +
            "DELETED INTEGER NOT NULL DEFAULT 0, " +
            // scoping por usuario
            "USER_ID TEXT NOT NULL DEFAULT ''" +
            ");";

    public SQL(Context context) {
        super(context, database, null, VRS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(tPacientes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE PACIENTES ADD COLUMN UPDATED_AT TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'");
            db.execSQL("ALTER TABLE PACIENTES ADD COLUMN DIRTY INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE PACIENTES ADD COLUMN DELETED INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE PACIENTES ADD COLUMN USER_ID TEXT NOT NULL DEFAULT ''");
            // Opcional: índice para acelerar filtros por usuario
            try { db.execSQL("CREATE INDEX IF NOT EXISTS IDX_PACIENTES_USER ON PACIENTES(USER_ID)"); } catch (Exception ignored) {}
        }
    }
}
