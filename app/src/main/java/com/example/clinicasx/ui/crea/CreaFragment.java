package com.example.clinicasx.ui.crea;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.clinicasx.R;
import com.example.clinicasx.db.SQLite;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreaFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener {

    private CreaViewModel mViewModel;

    private MaterialButton btnLimpiar, btnGuardar, btnCalendario, btnFotoCamara, btnFotoGaleria;
    private TextInputEditText etId, etNombre, etEdad, etEstatura, etPeso, etFIngreso;
    private Spinner spnArea, spnDr, spnGenero;
    private ImageView ivFoto;

    private DatePickerDialog dpd;
    private Calendar c;
    private static int anio, mes, dia;

    // selección actual
    public static String img = "", a, d, sex;

    // Foto
    private String currentPhotoPath = "";
    private Uri photoUri;

    // DB
    public SQLite sqLite;

    private ArrayAdapter<CharSequence> areaAdapter, drAdapter, generoAdapter;

    // Launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;  // cámara
    private ActivityResultLauncher<Uri> takePictureLauncher;          // tomar foto
    private ActivityResultLauncher<String> pickImageLauncher;         // galería

    public static CreaFragment newInstance() { return new CreaFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crea, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CreaViewModel.class);

        // Launchers
        inicializarLaunchers();

        // UI
        componentes(root);
        configurarAdapters();
        registrarListeners();

        // DB
        sqLite = new SQLite(requireContext());

        // Fecha por defecto = hoy
        c = Calendar.getInstance();
        anio = c.get(Calendar.YEAR);
        mes = c.get(Calendar.MONTH);
        dia = c.get(Calendar.DAY_OF_MONTH);
        etFIngreso.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia));
    }

    @Override public void onStart() { super.onStart(); if (sqLite != null) sqLite.abrir(); }
    @Override public void onStop()  { if (sqLite != null) sqLite.cerrar(); super.onStop(); }

    // ==================== LAUNCHERS ====================
    private void inicializarLaunchers() {
        // Permiso de cámara
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) abrirCamara();
                    else toast(getString(R.string.permiso_camara_denegado));
                }
        );

        // Tomar foto
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        mostrarFotoEnImageView(currentPhotoPath);
                        img = currentPhotoPath; // ruta para la BD
                        toast(getString(R.string.foto_capturada));
                    } else {
                        toast(getString(R.string.error_capturar_foto));
                    }
                }
        );

        // Galería (GetContent no necesita permiso de almacenamiento)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String saved = copyPickedImageToAppStorage(uri);
                        if (!TextUtils.isEmpty(saved)) {
                            img = saved;
                            mostrarFotoEnImageView(img);
                            toast(getString(R.string.imagen_seleccionada));
                        } else {
                            toast(getString(R.string.error_copiar_imagen));
                        }
                    }
                }
        );
    }

    // ==================== UI ====================
    private void componentes(View root) {
        etId = root.findViewById(R.id.CreaTextEdIdPac);
        etNombre = root.findViewById(R.id.CreaTextEdNombrePac);
        etEdad = root.findViewById(R.id.CreaTextEdEdadPac);
        etPeso = root.findViewById(R.id.CreaTextEdPesoPac);
        etFIngreso = root.findViewById(R.id.CreaTextEdIngPac);
        etEstatura = root.findViewById(R.id.CreaTextEdEstaturaPac);

        btnLimpiar = root.findViewById(R.id.CreaButtLimp);
        btnGuardar = root.findViewById(R.id.CreaButtGuar);
        btnCalendario = root.findViewById(R.id.CreaImgButFechaIng);
        btnFotoCamara = root.findViewById(R.id.btnFotoCamara);
        btnFotoGaleria = root.findViewById(R.id.btnFotoGaleria);

        ivFoto = root.findViewById(R.id.CreaImgVFPac);

        spnArea = root.findViewById(R.id.CreaSpinAreaPac);
        spnDr = root.findViewById(R.id.CreaSpin);
        spnGenero = root.findViewById(R.id.CreaSpinGeneroPac);
    }

    private void configurarAdapters() {
        areaAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.opciones, android.R.layout.simple_spinner_item);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnArea.setAdapter(areaAdapter);

        // doctores inicial -> "o0" (No se ha seleccionado área)
        drAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.o0, android.R.layout.simple_spinner_item);
        drAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDr.setAdapter(drAdapter);

        generoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sx, android.R.layout.simple_spinner_item);
        generoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGenero.setAdapter(generoAdapter);
    }

    private void registrarListeners() {
        btnLimpiar.setOnClickListener(this);
        btnGuardar.setOnClickListener(this);
        btnCalendario.setOnClickListener(this);
        // OJO: ya NO asignamos click a la imagen
        btnFotoCamara.setOnClickListener(this);
        btnFotoGaleria.setOnClickListener(this);

        spnArea.setOnItemSelectedListener(this);
        spnDr.setOnItemSelectedListener(this);
        spnGenero.setOnItemSelectedListener(this);
    }

    // ==================== ONCLICK ====================
    @Override
    public void onClick(View view) {
        int vid = view.getId();

        if (vid == R.id.CreaButtLimp) {
            limpiarCampos();

        } else if (vid == R.id.CreaButtGuar) {
            guardar();

        } else if (vid == R.id.CreaImgButFechaIng) {
            mostrarDatePicker();

        } else if (vid == R.id.btnFotoCamara) {
            verificarPermisoYAbrirCamara();

        } else if (vid == R.id.btnFotoGaleria) {
            pickImageLauncher.launch("image/*");
        }
    }

    // ==================== CÁMARA / GALERÍA ====================
    private void verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File photoFile = crearArchivoImagen();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.clinicasx.fileprovider",
                        photoFile
                );
                takePictureLauncher.launch(photoUri);
            } else {
                toast(getString(R.string.error_archivo_foto));
            }
        } catch (IOException ex) {
            toast(getString(R.string.error_archivo_foto));
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = new File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "ClinicaFotos"
        );
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio de imágenes");
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void mostrarFotoEnImageView(String path) {
        if (!TextUtils.isEmpty(path)) {
            Bitmap bitmap = decodeSampledBitmapFromFile(path, 480, 480);
            if (bitmap != null) {
                ivFoto.setImageBitmap(bitmap);
                ivFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                ivFoto.setImageResource(R.drawable.ic_person);
            }
        } else {
            ivFoto.setImageResource(R.drawable.ic_person);
        }
    }

    /** Copia la imagen de la galería a la carpeta privada de la app y devuelve la ruta absoluta. */
    private String copyPickedImageToAppStorage(@NonNull Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_PICK_" + timeStamp + ".jpg";
            File storageDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ClinicaFotos");
            if (!storageDir.exists()) storageDir.mkdirs();
            File outFile = new File(storageDir, fileName);

            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 java.io.OutputStream out = new java.io.FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== SPINNERS ====================
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int pid = parent.getId();

        if (pid == R.id.CreaSpinAreaPac) {
            actualizarDoctoresPorArea(position);

        } else if (pid == R.id.CreaSpin) {
            d = (String) spnDr.getSelectedItem();

        } else if (pid == R.id.CreaSpinGeneroPac) {
            sex = (String) spnGenero.getSelectedItem();
        }
    }
    @Override public void onNothingSelected(AdapterView<?> parent) { }

    private void actualizarDoctoresPorArea(int areaPos) {
        // 0 -> o0 | 1 -> o1 | 2 -> o2 | 3 -> o3 | 4 -> o4
        int arrayResId;
        switch (areaPos) {
            case 1: arrayResId = R.array.o1; break;
            case 2: arrayResId = R.array.o2; break;
            case 3: arrayResId = R.array.o3; break;
            case 4: arrayResId = R.array.o4; break;
            case 0:
            default: arrayResId = R.array.o0; break;
        }

        drAdapter = ArrayAdapter.createFromResource(
                requireContext(), arrayResId, android.R.layout.simple_spinner_item);
        drAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDr.setAdapter(drAdapter);
        spnDr.setSelection(0);
    }

    // ==================== DATE PICKER ====================
    private void mostrarDatePicker() {
        if (dpd == null) dpd = new DatePickerDialog(requireContext(), this, anio, mes, dia);
        dpd.updateDate(anio, mes, dia);
        dpd.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        anio = year; mes = month; dia = dayOfMonth;
        String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, month + 1, dia);
        etFIngreso.setText(fecha);
    }

    // ==================== LÓGICA ====================
    private void limpiarCampos() {
        etId.setText("");
        etNombre.setText("");
        etEdad.setText("");
        etEstatura.setText("");
        etPeso.setText("");
        etFIngreso.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, mes + 1, dia));

        spnArea.setSelection(0);
        // reset doctores a o0
        drAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.o0, android.R.layout.simple_spinner_item);
        drAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDr.setAdapter(drAdapter);
        spnDr.setSelection(0);

        spnGenero.setSelection(0);

        ivFoto.setImageResource(R.drawable.ic_person);
        currentPhotoPath = "";
        img = "";

        toast(getString(R.string.form_limpio));
    }

    private void guardar() {
        String idTxt = safeText(etId);
        String nombreTxt = safeText(etNombre);
        String edadTxt = safeText(etEdad);
        String estTxt = safeText(etEstatura);
        String pesoTxt = safeText(etPeso);
        String fechaTxt = safeText(etFIngreso);

        if (TextUtils.isEmpty(idTxt)) { etId.setError(getString(R.string.requerido)); etId.requestFocus(); return; }
        if (spnArea.getSelectedItemPosition() == 0) { toast(getString(R.string.seleccione_area)); return; }
        if (spnDr.getSelectedItemPosition() == 0) { toast(getString(R.string.seleccione_doctor)); return; }
        if (TextUtils.isEmpty(nombreTxt)) { etNombre.setError(getString(R.string.requerido)); etNombre.requestFocus(); return; }
        if (spnGenero.getSelectedItemPosition() == 0) { toast(getString(R.string.seleccione_genero)); return; }
        if (TextUtils.isEmpty(fechaTxt)) { etFIngreso.setError(getString(R.string.requerido)); etFIngreso.requestFocus(); return; }
        if (TextUtils.isEmpty(edadTxt)) { etEdad.setError(getString(R.string.requerido)); etEdad.requestFocus(); return; }
        if (TextUtils.isEmpty(estTxt)) { etEstatura.setError(getString(R.string.requerido)); etEstatura.requestFocus(); return; }
        if (TextUtils.isEmpty(pesoTxt)) { etPeso.setError(getString(R.string.requerido)); etPeso.requestFocus(); return; }

        a = (String) spnArea.getSelectedItem();
        d = (String) spnDr.getSelectedItem();
        sex = (String) spnGenero.getSelectedItem();

        int idInt;
        try { idInt = Integer.parseInt(idTxt); }
        catch (NumberFormatException e) { etId.setError(getString(R.string.id_invalido)); etId.requestFocus(); return; }

        String imagePath = TextUtils.isEmpty(img) ? "N/A" : img;

        if (sqLite == null) { toast(getString(R.string.db_no_init)); return; }

        boolean ok = sqLite.addRegistroPaciente(
                idInt, a, d, nombreTxt, sex, edadTxt, estTxt, pesoTxt, imagePath, fechaTxt
        );

        if (ok) {
            toast(getString(R.string.paciente_guardado));
            limpiarCampos();
        } else {
            toast(getString(R.string.error_guardar));
        }
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String s) { Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show(); }

    // ==================== Bitmap utils ====================
    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
