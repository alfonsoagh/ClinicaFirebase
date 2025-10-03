package com.example.clinicasx.ui.lista;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import android.widget.ImageView;
import android.widget.TextView;

import com.example.clinicasx.R;

import java.io.File;
import java.util.List;

public class PacienteListAdapter extends BaseAdapter {

    public static class PacienteItem {
        public final int id;
        public final String nombre;
        public final String area;
        public final String doctor;
        public final String sexo;
        public final String edad;
        public final String fecha;
        public final String estatura;
        public final String peso;
        public final String imgPath;

        public PacienteItem(int id, String nombre, String area, String doctor, String sexo,
                            String edad, String fecha, String estatura, String peso, String imgPath) {
            this.id = id;
            this.nombre = nombre;
            this.area = area;
            this.doctor = doctor;
            this.sexo = sexo;
            this.edad = edad;
            this.fecha = fecha;
            this.estatura = estatura;
            this.peso = peso;
            this.imgPath = imgPath;
        }
    }

    private final Context ctx;
    private final List<PacienteItem> data;
    private final LayoutInflater inflater;

    public PacienteListAdapter(Context ctx, List<PacienteItem> data) {
        this.ctx = ctx;
        this.data = data;
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override public int getCount() { return data.size(); }
    @Override public PacienteItem getItem(int position) { return data.get(position); }
    @Override public long getItemId(int position) { return data.get(position).id; }

    static class VH {
        ImageView img;
        TextView titulo;
        TextView subtitulo;
        TextView lineaExtra;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VH vh;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_paciente, parent, false);
            vh = new VH();
            vh.img = convertView.findViewById(R.id.itemImg);
            vh.titulo = convertView.findViewById(R.id.itemTitulo);
            vh.subtitulo = convertView.findViewById(R.id.itemSubtitulo);
            vh.lineaExtra = convertView.findViewById(R.id.itemLineaExtra);
            convertView.setTag(vh);
        } else {
            vh = (VH) convertView.getTag();
        }

        PacienteItem it = getItem(position);

        vh.titulo.setText(it.nombre + "  (ID: " + it.id + ")");
        vh.subtitulo.setText(it.area + " • " + it.doctor);
        vh.lineaExtra.setText(it.sexo + " • " + it.edad + " años • " + it.fecha);

        if (!TextUtils.isEmpty(it.imgPath) && new File(it.imgPath).exists()) {
            Bitmap bm = decodeSampledBitmapFromFile(it.imgPath, 300, 300);
            vh.img.setImageBitmap(bm != null ? bm : BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_person));
        } else {
            vh.img.setImageResource(R.drawable.ic_person);
        }

        return convertView;
    }

    // Utils para miniaturas
    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        opt.inSampleSize = calculateInSampleSize(opt, reqWidth, reqHeight);
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, opt);
    }
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int h = options.outHeight, w = options.outWidth, s = 1;
        if (h > reqHeight || w > reqWidth) {
            int halfH = h / 2, halfW = w / 2;
            while ((halfH / s) >= reqHeight && (halfW / s) >= reqWidth) s *= 2;
        }
        return s;
    }
}
