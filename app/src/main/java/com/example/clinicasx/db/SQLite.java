package com.example.clinicasx.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.util.Log;

import java.util.ArrayList;

public class SQLite {
    private SQL sql;
    private SQLiteDatabase db;

    public SQLite(Context context){
        sql = new SQL(context);
    }

    public void abrir() {
        Log.i("SQLite", "Se abre conexion DB " + sql.getDatabaseName());
        db = sql.getWritableDatabase();
    }

    public void cerrar() {
        Log.i("SQLite", "Se cierra conexion DB " + sql.getDatabaseName());
        sql.close();
    }

    // INSERT (ID manual)
    public boolean addRegistroPaciente(int id, String area, String doc, String name, String sex, String age,
                                       String height, String weight, String image, String date) {
        ContentValues cv = new ContentValues();
        cv.put("ID", id); // <- ID manual
        cv.put("AREA", area);
        cv.put("DOCTOR", doc);
        cv.put("NOMBRE", name);
        cv.put("SEXO", sex);
        cv.put("FECHA_INGRESO", date);
        cv.put("EDAD", age);
        cv.put("ESTATURA", height);
        cv.put("PESO", weight);

        if (image == null || image.trim().isEmpty()) image = "N/A";
        cv.put("IMAGEN", image);

        long rowId = db.insert("PACIENTES", null, cv);
        Log.i("SQLite", "Insert rowId=" + rowId);
        return rowId != -1;
    }

    public Cursor getRegistros(){
        return db.rawQuery("SELECT * FROM PACIENTES", null);
    }

    public Cursor getValor(int id){
        return db.rawQuery("SELECT * FROM PACIENTES WHERE ID = ?", new String[]{ String.valueOf(id) });
    }

    public ArrayList<String> getPacientes(Cursor cursor) {
        ArrayList<String> ListData = new ArrayList<>();
        String items;
        if (cursor.moveToFirst()){
            do{
                items  = "ID: [" + cursor.getString(0) + "]\r\n";
                items += "AREA: [" + cursor.getString(1) + "]\r\n";
                items += "DOCTOR: [" + cursor.getString(2) + "]\r\n";
                items += "NOMBRE: [" + cursor.getString(3) + "]\r\n";
                items += "SEXO: [" + cursor.getString(4) + "]\r\n";
                items += "FECHA_INGRESO: [" + cursor.getString(5) + "]\r\n";
                items += "EDAD: [" + cursor.getString(6) + "]\r\n";
                items += "ESTATURA: [" + cursor.getString(7) + "]\r\n";
                items += "PESO: [" + cursor.getString(8) + "]\r\n";
                ListData.add(items);
            } while (cursor.moveToNext());
        }
        return ListData;
    }

    public ArrayList<String> getImagenes(Cursor cursor) {
        ArrayList<String> ListData = new ArrayList<>();
        if (cursor.moveToFirst()){
            do{
                ListData.add(cursor.getString(9)); // IMAGEN
            } while (cursor.moveToNext());
        }
        return ListData;
    }

    public ArrayList<String> getId(Cursor cursor) {
        ArrayList<String> ListData = new ArrayList<>();
        if (cursor.moveToFirst()){
            do{
                ListData.add(cursor.getString(0)); // ID
            } while (cursor.moveToNext());
        }
        return ListData;
    }

    public String updateRegistroPaciente(int id, String area, String doc, String name, String sex, String age,
                                         String height, String weight, String image, String date) {
        ContentValues cv = new ContentValues();
        cv.put("AREA", area);
        cv.put("DOCTOR", doc);
        cv.put("NOMBRE", name);
        cv.put("SEXO", sex);
        cv.put("FECHA_INGRESO", date);
        cv.put("EDAD", age);
        cv.put("ESTATURA", height);
        cv.put("PESO", weight);
        if (image == null || image.trim().isEmpty()) image = "N/A";
        cv.put("IMAGEN", image);

        int valor = db.update("PACIENTES", cv, "ID = ?", new String[]{ String.valueOf(id) });

        if (valor == 1){
            return "Paciente actualizado";
        } else if (valor > 1){
            return "Se actualizaron " + valor + " registros";
        } else {
            return "No se actualizó ningún registro";
        }
    }

    public int Eliminar(Editable id){
        return db.delete("PACIENTES", "ID = ?", new String[]{ String.valueOf(id) });
    }

    // En com.example.clinicasx.db.SQLite
    public int eliminarPorId(int id){
        return db.delete("PACIENTES", "ID = ?", new String[]{ String.valueOf(id) });
    }

}
