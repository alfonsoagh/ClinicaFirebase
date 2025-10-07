package com.example.clinicasx.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class SQLite {
    private SQL sql;
    private SQLiteDatabase db;
    private String userId = ""; // UID actual

    public SQLite(Context context){
        sql = new SQL(context);
    }

    public void setUserId(String uid){
        this.userId = (uid == null ? "" : uid);
    }

    public void abrir() {
        Log.i("SQLite", "Se abre conexion DB " + sql.getDatabaseName());
        db = sql.getWritableDatabase();
        if (userId == null || userId.isEmpty()) {
            try {
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                if (u != null) userId = u.getUid();
            } catch (Exception ignored) {}
        }
    }

    public void cerrar() {
        Log.i("SQLite", "Se cierra conexion DB " + sql.getDatabaseName());
        sql.close();
    }

    private static String nowIso() {
        return java.time.Instant.now().toString();
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

        // flags de sync
        cv.put("UPDATED_AT", nowIso());
        cv.put("DIRTY", 1);
        cv.put("DELETED", 0);
        cv.put("USER_ID", userId);

        long rowId = db.insert("PACIENTES", null, cv);
        Log.i("SQLite", "Insert rowId=" + rowId);
        return rowId != -1;
    }

    public Cursor getRegistros(){
        return db.rawQuery("SELECT * FROM PACIENTES WHERE DELETED = 0 AND USER_ID = ?", new String[]{ userId });
    }

    public Cursor getValor(int id){
        return db.rawQuery("SELECT * FROM PACIENTES WHERE ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });
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
        // flags de sync
        cv.put("UPDATED_AT", nowIso());
        cv.put("DIRTY", 1);

        int valor = db.update("PACIENTES", cv, "ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });

        if (valor == 1){
            return "Paciente actualizado";
        } else if (valor > 1){
            return "Se actualizaron " + valor + " registros";
        } else {
            return "No se actualizó ningún registro";
        }
    }

    public int Eliminar(Editable id){
        // Borrado lógico
        ContentValues cv = new ContentValues();
        cv.put("DELETED", 1);
        cv.put("DIRTY", 1);
        cv.put("UPDATED_AT", nowIso());
        return db.update("PACIENTES", cv, "ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });
    }

    // En com.example.clinicasx.db.SQLite
    public int eliminarPorId(int id){
        // Borrado lógico
        ContentValues cv = new ContentValues();
        cv.put("DELETED", 1);
        cv.put("DIRTY", 1);
        cv.put("UPDATED_AT", nowIso());
        return db.update("PACIENTES", cv, "ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });
    }

    // Registros pendientes de sincronizar (insert/update/delete)
    public Cursor getDirty(){
        // Incluir registros migrados sin USER_ID para adjudicarlos al primer sync del usuario actual
        return db.rawQuery("SELECT * FROM PACIENTES WHERE DIRTY = 1 AND (USER_ID = ? OR USER_ID = '')", new String[]{ userId });
    }

    public void markSynced(int id, String updatedAt){
        ContentValues cv = new ContentValues();
        cv.put("DIRTY", 0);
        if (updatedAt != null) cv.put("UPDATED_AT", updatedAt);
        cv.put("USER_ID", userId);
        db.update("PACIENTES", cv, "ID = ?", new String[]{ String.valueOf(id) });
    }

    public void upsertFromRemote(int id, String area, String doc, String name, String sex, String age,
                                 String height, String weight, String image, String date,
                                 String updatedAt, boolean deleted) {
        Cursor c = db.rawQuery("SELECT ID FROM PACIENTES WHERE ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });
        boolean exists = c.moveToFirst();
        c.close();
        ContentValues cv = new ContentValues();
        cv.put("AREA", area);
        cv.put("DOCTOR", doc);
        cv.put("NOMBRE", name);
        cv.put("SEXO", sex);
        cv.put("FECHA_INGRESO", date);
        cv.put("EDAD", age);
        cv.put("ESTATURA", height);
        cv.put("PESO", weight);
        cv.put("IMAGEN", image);
        cv.put("UPDATED_AT", updatedAt);
        cv.put("DIRTY", 0);
        cv.put("DELETED", deleted ? 1 : 0);
        cv.put("USER_ID", userId);
        if (exists) {
            db.update("PACIENTES", cv, "ID = ? AND USER_ID = ?", new String[]{ String.valueOf(id), userId });
        } else {
            cv.put("ID", id);
            db.insert("PACIENTES", null, cv);
        }
    }

    public void clearForUidSwitch(String uid){
        // Elimina todo lo que no pertenezca al UID actual (incluye huérfanos con USER_ID diferente)
        int rows = db.delete("PACIENTES", "USER_ID <> ?", new String[]{ uid == null ? "" : uid });
        Log.i("SQLite", "clearForUidSwitch borró filas=" + rows);
    }
}
