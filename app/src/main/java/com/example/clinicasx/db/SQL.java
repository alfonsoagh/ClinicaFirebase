package com.example.clinicasx.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQL extends SQLiteOpenHelper {
    private static final String database = "pacientes";
    private static final int VRS = 2; // <- sube versión para aplicar cambio

    // ID manual: sin AUTOINCREMENT
    // Sugerencia: agrega UNIQUE(ID) para reforzar unicidad (PK ya la garantiza)
    private static final String tPacientes = "CREATE TABLE PACIENTES("+
            "ID INTEGER PRIMARY KEY NOT NULL, " +   // <- SIN AUTOINCREMENT
            "AREA TEXT NOT NULL, "+
            "DOCTOR TEXT NOT NULL, "+
            "NOMBRE TEXT NOT NULL, "+
            "SEXO TEXT NOT NULL, "+
            "FECHA_INGRESO TEXT NOT NULL, "+
            "EDAD TEXT NOT NULL, "+
            "ESTATURA TEXT NOT NULL, "+
            "PESO TEXT NOT NULL, "+
            "IMAGEN TEXT NOT NULL"+
            ");";

    public SQL(Context context){
        super(context, database, null, VRS);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(tPacientes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        if (newVersion > oldVersion){
            // Opción simple: drop & recreate (pierde datos)
            db.execSQL("DROP TABLE IF EXISTS PACIENTES");
            db.execSQL(tPacientes);

            // Si quieres migrar sin perder datos: te paso un script al final.
        }
    }
}
