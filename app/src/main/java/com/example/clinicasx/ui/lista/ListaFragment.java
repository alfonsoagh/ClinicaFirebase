package com.example.clinicasx.ui.lista;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.clinicasx.R;
import com.example.clinicasx.databinding.FragmentListaBinding;
import com.example.clinicasx.db.SQLite;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ListaFragment extends Fragment {

    private FragmentListaBinding binding;
    private SQLite sqLite;
    private PacienteListAdapter adapter;
    private ListView listView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        ListaViewModel listaViewModel = new ViewModelProvider(this).get(ListaViewModel.class);

        binding = FragmentListaBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Título
        listaViewModel.getText().observe(getViewLifecycleOwner(), binding.textListar::setText);

        // ListView
        listView = binding.ListLViewPac != null ? binding.ListLViewPac : root.findViewById(R.id.ListLViewPac);

        // DB
        sqLite = new SQLite(requireContext());

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        sqLite.abrir();
        cargarPacientes();
    }

    @Override
    public void onStop() {
        sqLite.cerrar();
        super.onStop();
    }

    private void cargarPacientes() {
        Cursor c = sqLite.getRegistros();
        List<PacienteListAdapter.PacienteItem> items = new ArrayList<>();

        if (c != null && c.moveToFirst()) {
            do {
                // 0 ID, 1 AREA, 2 DOCTOR, 3 NOMBRE, 4 SEXO, 5 FECHA_INGRESO,
                // 6 EDAD, 7 ESTATURA, 8 PESO, 9 IMAGEN
                int id = c.getInt(0);
                String area = s(c, 1);
                String doctor = s(c, 2);
                String nombre = s(c, 3);
                String sexo = s(c, 4);
                String fecha = s(c, 5);
                String edad = s(c, 6);
                String est = s(c, 7);
                String peso = s(c, 8);
                String img = s(c, 9);

                items.add(new PacienteListAdapter.PacienteItem(
                        id, nombre, area, doctor, sexo, edad, fecha, est, peso, img
                ));
            } while (c.moveToNext());
            c.close();
        }

        if (items.isEmpty()) {
            if (adapter != null) adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "No hay pacientes registrados", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new PacienteListAdapter(requireContext(), items);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            PacienteListAdapter.PacienteItem it = adapter.getItem(position);
            mostrarDialogoDetalle(it);
        });
    }

    private void mostrarDialogoDetalle(PacienteListAdapter.PacienteItem it) {
        View dlg = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_paciente_detalle, null, false);

        ImageView img = dlg.findViewById(R.id.dlgImg);
        TextView titulo = dlg.findViewById(R.id.dlgTitulo);
        TextView l1 = dlg.findViewById(R.id.dlgLinea1);
        TextView l2 = dlg.findViewById(R.id.dlgLinea2);
        TextView l3 = dlg.findViewById(R.id.dlgLinea3);

        titulo.setText(it.nombre + "  (ID: " + it.id + ")");
        l1.setText(it.area + " • " + it.doctor);
        l2.setText(it.sexo + " • " + it.edad + " años • " + it.fecha);
        l3.setText(getString(R.string.estatura) + ": " + it.estatura + " • " + getString(R.string.peso) + ": " + it.peso);

        String src = it.imgPath;
        if (TextUtils.isEmpty(src) || "N/A".equalsIgnoreCase(src)) {
            img.setImageResource(R.drawable.ic_person);
        } else if (src.startsWith("http")) {
            Glide.with(this).load(src).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(img);
        } else if (src.startsWith("content://")) {
            Glide.with(this).load(android.net.Uri.parse(src)).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(img);
        } else {
            Glide.with(this).load(new File(src)).placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(img);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dlg)
                .setPositiveButton("Cerrar", (d, w) -> d.dismiss())
                .show();
    }

    private static String s(Cursor c, int i) { return c.isNull(i) ? "" : c.getString(i); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
