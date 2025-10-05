package com.example.clinicasx.ui.elimina;

import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.clinicasx.R;
import com.example.clinicasx.db.SQLite;
import com.example.clinicasx.sync.SyncManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class EliminaFragment extends Fragment {

    private EliminaViewModel mViewModel;

    private TextInputEditText etId;
    private ImageView ivFoto;

    // Textos de datos
    private android.widget.TextView tvNombre, tvAreaDoc, tvGeneroEdad, tvFecha, tvEstatPeso;

    private MaterialButton btnBuscar, btnLimpiar, btnEliminar;

    private SQLite sqLite;

    // Caché del registro encontrado (para confirmar/borrar)
    private Integer currentId = null;
    private String currentNombre = "";
    private String currentImgPath = "";

    public static EliminaFragment newInstance() {
        return new EliminaFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_elimina, container, false);

        // ViewModel (si lo usas)
        mViewModel = new ViewModelProvider(this).get(EliminaViewModel.class);

        // DB
        sqLite = new SQLite(requireContext());

        // UI
        etId = root.findViewById(R.id.ElimTextEdIdPac);
        ivFoto = root.findViewById(R.id.ElimImgVFPac);

        tvNombre = root.findViewById(R.id.ElimTextVNombre);
        tvAreaDoc = root.findViewById(R.id.ElimTextVAreaDoc);
        tvGeneroEdad = root.findViewById(R.id.ElimTextVGeneroEdad);
        tvFecha = root.findViewById(R.id.ElimTextVFecha);
        tvEstatPeso = root.findViewById(R.id.ElimTextVEstatPeso);

        btnBuscar = root.findViewById(R.id.ElimButtBusc);
        btnLimpiar = root.findViewById(R.id.ElimButtLimp);
        btnEliminar = root.findViewById(R.id.ElimButtEliminar);

        btnBuscar.setOnClickListener(v -> buscar());
        btnLimpiar.setOnClickListener(v -> limpiar());
        btnEliminar.setOnClickListener(v -> confirmarEliminar());

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        sqLite.abrir();
    }

    @Override
    public void onStop() {
        sqLite.cerrar();
        super.onStop();
    }



    private void buscar() {
        String idTxt = etId.getText() == null ? "" : etId.getText().toString().trim();
        if (TextUtils.isEmpty(idTxt)) {
            etId.setError(getString(R.string.requerido));
            etId.requestFocus();
            return;
        }
        int id;
        try {
            id = Integer.parseInt(idTxt);
        } catch (NumberFormatException e) {
            etId.setError(getString(R.string.id_invalido));
            etId.requestFocus();
            return;
        }

        Cursor c = sqLite.getValor(id);
        if (c != null && c.moveToFirst()) {
            // Orden columnas: 0 ID,1 AREA,2 DOCTOR,3 NOMBRE,4 SEXO,5 FECHA_INGRESO,
            // 6 EDAD,7 ESTATURA,8 PESO,9 IMAGEN
            String area = safe(c, 1);
            String doctor = safe(c, 2);
            String nombre = safe(c, 3);
            String sexo = safe(c, 4);
            String fecha = safe(c, 5);
            String edad = safe(c, 6);
            String est = safe(c, 7);
            String peso = safe(c, 8);
            String imgPath = safe(c, 9);

            // Pinta UI
            tvNombre.setText(nombre + "  (ID: " + id + ")");
            tvAreaDoc.setText(area + " • " + doctor);
            tvGeneroEdad.setText(sexo + " • " + edad + " años");
            tvFecha.setText(getString(R.string.fechaIngreso) + ": " + fecha);
            tvEstatPeso.setText(getString(R.string.estatura) + ": " + est + " • " + getString(R.string.peso) + ": " + peso);

            // Foto con Glide (http/content/file)
            if (TextUtils.isEmpty(imgPath) || "N/A".equalsIgnoreCase(imgPath)) {
                ivFoto.setImageResource(R.drawable.ic_person);
            } else if (imgPath.startsWith("http")) {
                Glide.with(this).load(imgPath).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).centerCrop().into(ivFoto);
            } else if (imgPath.startsWith("content://")) {
                Glide.with(this).load(android.net.Uri.parse(imgPath)).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).centerCrop().into(ivFoto);
            } else {
                Glide.with(this).load(new File(imgPath)).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).centerCrop().into(ivFoto);
            }

            // Guarda para confirmación
            currentId = id;
            currentNombre = nombre;
            currentImgPath = imgPath;

            c.close();
        } else {
            Toast.makeText(requireContext(), "No se encontró el paciente con ID " + id, Toast.LENGTH_SHORT).show();
            limpiarDatos();
            if (c != null) c.close();
        }
    }

    private void confirmarEliminar() {
        if (currentId == null) {
            Toast.makeText(requireContext(), "Busca primero un paciente", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar paciente")
                .setMessage("¿Seguro que deseas eliminar a \"" + currentNombre + "\" (ID: " + currentId + ")?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminar())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void eliminar() {
        if (currentId == null) return;

        int rows = sqLite.eliminarPorId(currentId);

        // (Opcional) borrar archivo de foto si existe (solo si era local)
        if (!TextUtils.isEmpty(currentImgPath) && !currentImgPath.startsWith("http")) {
            File f = new File(currentImgPath);
            if (f.exists()) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }

        if (rows > 0) {
            Toast.makeText(requireContext(), "Paciente eliminado", Toast.LENGTH_SHORT).show();
            // Encolar sincronización inmediata tras DELETE
            SyncManager.enqueueNow(requireContext());
            limpiar();
        } else {
            Toast.makeText(requireContext(), "No se pudo eliminar", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiar() {
        etId.setText("");
        limpiarDatos();
        ivFoto.setImageResource(R.drawable.ic_person);
        currentId = null;
        currentNombre = "";
        currentImgPath = "";
    }

    private void limpiarDatos() {
        tvNombre.setText(getString(R.string.nombrePaciente));
        tvAreaDoc.setText("Área • Médico");
        tvGeneroEdad.setText("Género • Edad");
        tvFecha.setText(getString(R.string.fechaIngreso));
        tvEstatPeso.setText(getString(R.string.estatura) + " • " + getString(R.string.peso));
    }

    private static String safe(Cursor c, int idx) {
        return c.isNull(idx) ? "" : c.getString(idx);
    }
}
