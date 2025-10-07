package com.example.clinicasx.model;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

public class Patient {
    public int id;
    public String area;
    public String doctor;
    public String nombre;
    public String sexo;
    public String fechaIngreso;
    public String edad;
    public String estatura;
    public String peso;
    public String imagen; // ruta local o URL
    public String updatedAt;
    public boolean deleted;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("area", area);
        m.put("doctor", doctor);
        m.put("nombre", nombre);
        m.put("sexo", sexo);
        m.put("fechaIngreso", fechaIngreso);
        m.put("edad", edad);
        m.put("estatura", estatura);
        m.put("peso", peso);
        m.put("imagen", imagen);
        m.put("updatedAt", updatedAt);
        m.put("deleted", deleted);
        return m;
    }

    public static Patient fromCursor(Cursor c){
        Patient p = new Patient();
        p.id = c.getInt(c.getColumnIndexOrThrow("ID"));
        p.area = c.getString(c.getColumnIndexOrThrow("AREA"));
        p.doctor = c.getString(c.getColumnIndexOrThrow("DOCTOR"));
        p.nombre = c.getString(c.getColumnIndexOrThrow("NOMBRE"));
        p.sexo = c.getString(c.getColumnIndexOrThrow("SEXO"));
        p.fechaIngreso = c.getString(c.getColumnIndexOrThrow("FECHA_INGRESO"));
        p.edad = c.getString(c.getColumnIndexOrThrow("EDAD"));
        p.estatura = c.getString(c.getColumnIndexOrThrow("ESTATURA"));
        p.peso = c.getString(c.getColumnIndexOrThrow("PESO"));
        p.imagen = c.getString(c.getColumnIndexOrThrow("IMAGEN"));
        p.updatedAt = c.getString(c.getColumnIndexOrThrow("UPDATED_AT"));
        p.deleted = c.getInt(c.getColumnIndexOrThrow("DELETED")) == 1;
        return p;
    }
}

