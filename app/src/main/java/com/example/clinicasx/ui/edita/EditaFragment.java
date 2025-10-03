package com.example.clinicasx.ui.edita;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ImageView;

import com.example.clinicasx.R;
import com.example.clinicasx.db.SQLite;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditaFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener {

    // ========= DB =========
    private SQLite sqlite;

    // ========= UI =========
    private ImageView ivFoto;
    private TextInputEditText etId, etNombre, etEdad, etEstatura, etPeso, etFIngreso;
    private Spinner spnArea, spnDr, spnGenero;
    private MaterialButton btnLimpiar, btnGuardar, btnBuscar, btnCalendario, btnFotoCamara, btnFotoGaleria;

    // Adapters
    private ArrayAdapter<CharSequence> areaAdapter, drAdapter, generoAdapter;

    // DatePicker
    private DatePickerDialog dpd;
    private Calendar cal;
    private static int anio, mes, dia;

    // Foto
    private String currentImgPath = "N/A";
    private String currentPhotoPath = "";
    private Uri photoUri;

    // Launchers
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    // Flag para evitar eventos mientras configuramos programáticamente
    private boolean updatingUi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edita, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // DB
        sqlite = new SQLite(requireContext());

        // UI
        ivFoto       = root.findViewById(R.id.EditaImgVFPac);
        etId         = root.findViewById(R.id.EditaTextEdIdPac);
        etNombre     = root.findViewById(R.id.EditaTextEdNombrePac);
        etEdad       = root.findViewById(R.id.EditaTextEdEdadPac);
        etEstatura   = root.findViewById(R.id.EditaTextEdEstaturaPac);
        etPeso       = root.findViewById(R.id.EditaTextEdPesoPac);
        etFIngreso   = root.findViewById(R.id.EditaTextEdIngPac);

        spnArea      = root.findViewById(R.id.EditaSpinAreaPac);
        spnDr        = root.findViewById(R.id.EditaSpin);
        spnGenero    = root.findViewById(R.id.EditaSpinGeneroPac);

        btnLimpiar    = root.findViewById(R.id.EditaButtLimp);
        btnGuardar    = root.findViewById(R.id.EditaButtGuar);
        btnBuscar     = root.findViewById(R.id.EditaButtBusc);
        btnCalendario = root.findViewById(R.id.EditaImgButFechaIng);
        btnFotoCamara = root.findViewById(R.id.btnFotoCamara);
        btnFotoGaleria= root.findViewById(R.id.btnFotoGaleria);

        // Adapters base
        areaAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.opciones, android.R.layout.simple_spinner_item);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnArea.setAdapter(areaAdapter);

        // doctores por defecto -> o0 (no hay área seleccionada)
        setDoctorsAdapter(R.array.o0);

        generoAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.sx, android.R.layout.simple_spinner_item);
        generoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGenero.setAdapter(generoAdapter);

        // Listeners
        btnLimpiar.setOnClickListener(this);
        btnGuardar.setOnClickListener(this);
        btnBuscar.setOnClickListener(this);
        btnCalendario.setOnClickListener(this);
        etFIngreso.setOnClickListener(this);
        btnFotoCamara.setOnClickListener(this);
        btnFotoGaleria.setOnClickListener(this);

        spnArea.setOnItemSelectedListener(this);
        spnDr.setOnItemSelectedListener(this);
        spnGenero.setOnItemSelectedListener(this);

        // Enter en ID = buscar
        etId.setOnEditorActionListener((v, actionId, event) -> { buscarPorId(); return true; });

        // Fecha por defecto = hoy
        cal = Calendar.getInstance();
        anio = cal.get(Calendar.YEAR);
        mes  = cal.get(Calendar.MONTH);
        dia  = cal.get(Calendar.DAY_OF_MONTH);

        // Launchers
        initLaunchers();
    }

    @Override public void onStart() { super.onStart(); sqlite.abrir(); }
    @Override public void onStop() { sqlite.cerrar(); super.onStop(); }

    // ==================== Launchers ====================
    private void initLaunchers() {
        requestCameraPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                        isGranted -> { if (isGranted) abrirCamara(); else toast(getString(R.string.permiso_camara_denegado)); });

        takePictureLauncher =
                registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                    if (success && photoUri != null) {
                        mostrarFoto(currentPhotoPath);
                        currentImgPath = currentPhotoPath; // guardar para UPDATE
                        toast(getString(R.string.foto_capturada));
                    } else {
                        toast(getString(R.string.error_capturar_foto));
                    }
                });

        pickImageLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        String saved = copyPickedImageToAppStorage(uri);
                        if (!TextUtils.isEmpty(saved)) {
                            currentImgPath = saved;
                            mostrarFoto(currentImgPath);
                            toast(getString(R.string.imagen_seleccionada));
                        } else {
                            toast(getString(R.string.error_copiar_imagen));
                        }
                    }
                });
    }

    // ==================== Clicks ====================
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.EditaButtLimp) {
            limpiarCampos();
        } else if (id == R.id.EditaButtGuar) {
            guardarCambios();
        } else if (id == R.id.EditaButtBusc) {
            buscarPorId();
        } else if (id == R.id.EditaImgButFechaIng || id == R.id.EditaTextEdIngPac) {
            mostrarDatePicker();
        } else if (id == R.id.btnFotoCamara) {
            verificarPermisoYAbrirCamara();
        } else if (id == R.id.btnFotoGaleria) {
            pickImageLauncher.launch("image/*");
        }
    }

    // ==================== Cámara / Galería ====================
    private void verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            File photoFile = crearArchivoImagen();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.clinicasx.fileprovider", photoFile);
                takePictureLauncher.launch(photoUri);
            }
        } catch (IOException ex) {
            toast(getString(R.string.error_archivo_foto));
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ClinicaFotos");
        if (!storageDir.exists()) storageDir.mkdirs();
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void mostrarFoto(String path) {
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            Bitmap bm = decodeSampledBitmapFromFile(path, 480, 480);
            ivFoto.setImageBitmap(bm != null ? bm : BitmapFactory.decodeResource(getResources(), R.drawable.ic_person));
        } else {
            ivFoto.setImageResource(R.drawable.ic_person);
        }
    }

    /** Copia la imagen seleccionada de galería al almacenamiento privado de la app y devuelve su path absoluto */
    private String copyPickedImageToAppStorage(@NonNull Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
            String imageFileName = "IMG_PICK_" + timeStamp + ".jpg";
            File storageDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ClinicaFotos");
            if (!storageDir.exists()) storageDir.mkdirs();
            File outFile = new File(storageDir, imageFileName);

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

    // ==================== Spinners ====================
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (updatingUi) return; // evitar reacciones mientras seteamos programáticamente

        if (parent.getId() == R.id.EditaSpinAreaPac) {
            // cambio hecho por el usuario -> refrescamos doctores
            actualizarDoctoresPorArea(position);
        }
    }
    @Override public void onNothingSelected(AdapterView<?> parent) { }

    // ==================== DatePicker ====================
    private void mostrarDatePicker() {
        if (dpd == null) dpd = new DatePickerDialog(requireContext(), this, anio, mes, dia);
        dpd.updateDate(anio, mes, dia);
        dpd.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        anio = year; mes = month; dia = dayOfMonth;
        etFIngreso.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", anio, month + 1, dia));
    }

    // ==================== Buscar / Guardar ====================
    private void buscarPorId() {
        String sid = safeText(etId);
        if (TextUtils.isEmpty(sid)) { toast(getString(R.string.requerido)); return; }
        int id;
        try { id = Integer.parseInt(sid); }
        catch (NumberFormatException e) { toast(getString(R.string.id_invalido)); return; }

        Cursor c = null;
        try {
            c = sqlite.getValor(id);
            if (c != null && c.moveToFirst()) {
                // 0 ID, 1 AREA, 2 DOCTOR, 3 NOMBRE, 4 SEXO, 5 FECHA_INGRESO,
                // 6 EDAD, 7 ESTATURA, 8 PESO, 9 IMAGEN
                String area     = s(c, 1);
                String doctor   = s(c, 2);
                String nombre   = s(c, 3);
                String sexo     = s(c, 4);
                String fecha    = s(c, 5);
                String edad     = s(c, 6);
                String estatura = s(c, 7);
                String peso     = s(c, 8);
                String imagen   = s(c, 9);

                // *** CARGA EN DOS PASOS: ÁREA -> ADAPTER DOCTORES -> DOCTOR ***
                updatingUi = true;

                // 1) Área (posición exacta en "opciones")
                final int areaPos = getIndexInArray(getResources().getStringArray(R.array.opciones), area);
                spnArea.setSelection(areaPos < 0 ? 0 : areaPos, false);

                // 2) Setear adapter de doctores para esa área
                final int doctorsArrayRes = doctorsArrayForAreaPos(areaPos < 0 ? 0 : areaPos);
                setDoctorsAdapter(doctorsArrayRes);

                // 3) Seleccionar el doctor *después* de que el adapter esté listo
                final int drPosTarget = getIndexInArray(getResources().getStringArray(doctorsArrayRes), doctor);
                spnDr.post(() -> {
                    spnDr.setSelection(drPosTarget < 0 ? 0 : drPosTarget, false);
                    // Termina la ventana crítica: ya podemos reactivar eventos
                    updatingUi = false;
                });

                // 4) Género y demás campos (no afectan a los spinners anteriores)
                int genPos = getIndexInArray(getResources().getStringArray(R.array.sx), sexo);
                spnGenero.setSelection(genPos < 0 ? 0 : genPos, false);

                etNombre.setText(nombre);
                etFIngreso.setText(fecha);
                etEdad.setText(edad);
                etEstatura.setText(estatura);
                etPeso.setText(peso);

                // 5) Foto
                currentImgPath = (imagen == null || imagen.trim().isEmpty()) ? "N/A" : imagen;
                mostrarFoto(currentImgPath);

                toast("Paciente cargado");
            } else {
                toast("No se encontró el ID " + id);
                currentImgPath = "N/A";
                ivFoto.setImageResource(R.drawable.ic_person);
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private void guardarCambios() {
        String sid = safeText(etId);
        if (TextUtils.isEmpty(sid)) { toast("Indica el ID a actualizar"); return; }
        int id;
        try { id = Integer.parseInt(sid); }
        catch (NumberFormatException e) { toast(getString(R.string.id_invalido)); return; }

        if (spnArea.getSelectedItemPosition() == 0)  { toast(getString(R.string.seleccione_area)); return; }
        if (spnDr.getSelectedItemPosition() == 0)    { toast(getString(R.string.seleccione_doctor)); return; }
        if (spnGenero.getSelectedItemPosition() == 0){ toast(getString(R.string.seleccione_genero)); return; }

        String area   = (String) spnArea.getSelectedItem();
        String doctor = (String) spnDr.getSelectedItem();
        String nombre = safeText(etNombre);
        String sexo   = (String) spnGenero.getSelectedItem();
        String fecha  = safeText(etFIngreso);
        String edad   = safeText(etEdad);
        String est    = safeText(etEstatura);
        String peso   = safeText(etPeso);

        if (TextUtils.isEmpty(nombre)) { etNombre.setError(getString(R.string.requerido)); etNombre.requestFocus(); return; }
        if (TextUtils.isEmpty(fecha))  { etFIngreso.setError(getString(R.string.requerido)); etFIngreso.requestFocus(); return; }

        String imagen = (currentImgPath == null || currentImgPath.trim().isEmpty()) ? "N/A" : currentImgPath;

        String msg = sqlite.updateRegistroPaciente(id, area, doctor, nombre, sexo, edad, est, peso, imagen, fecha);
        toast(msg);

        if (wasUpdateSuccessful(msg)) {
            limpiarCampos();
        }
    }

    private boolean wasUpdateSuccessful(@NonNull String msg) {
        String m = msg.toLowerCase(Locale.getDefault());
        return m.contains("actualizado") || m.contains("actualizaron");
    }

    // ==================== Utils ====================
    private void actualizarDoctoresPorArea(int areaPos) {
        int res = doctorsArrayForAreaPos(areaPos);
        setDoctorsAdapter(res);
        spnDr.setSelection(0);
    }

    /** Devuelve el array de doctores que corresponde a la posición del área (opciones). */
    private int doctorsArrayForAreaPos(int areaPos) {
        // opciones: 0=Seleccione area, 1=Neurologia, 2=Traumatologia, 3=Cardiologia, 4=Medicina General
        switch (areaPos) {
            case 1: return R.array.o1; // Neurologia
            case 2: return R.array.o2; // Traumatologia
            case 3: return R.array.o3; // Cardiologia
            case 4: return R.array.o4; // Medicina General
            case 0:
            default: return R.array.o0; // No se ha seleccionado area
        }
    }

    private void setDoctorsAdapter(int arrayResId) {
        drAdapter = ArrayAdapter.createFromResource(
                requireContext(), arrayResId, android.R.layout.simple_spinner_item);
        drAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDr.setAdapter(drAdapter);
    }

    /** Index en un array de strings, comparando case-insensitive y trim. */
    private int getIndexInArray(String[] arr, String value) {
        if (arr == null || value == null) return -1;
        String v = value.trim();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i].trim().equalsIgnoreCase(v)) return i;
        }
        return -1;
    }

    private static String s(Cursor c, int i) { return c.isNull(i) ? "" : c.getString(i); }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show(); }

    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opt);
        opt.inSampleSize = calculateInSampleSize(opt, reqWidth, reqHeight);
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, opt);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight, width = options.outWidth, inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfH = height / 2, halfW = width / 2;
            while ((halfH / inSampleSize) >= reqHeight && (halfW / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /** Limpia todos los campos, resetea spinners y vuelve a la foto por defecto. */
    private void limpiarCampos() {
        etId.setText("");
        etNombre.setText("");
        etEdad.setText("");
        etEstatura.setText("");
        etPeso.setText("");
        etFIngreso.setText("");

        // Spinners
        updatingUi = true;
        spnArea.setSelection(0, false);
        setDoctorsAdapter(R.array.o0);
        spnDr.setSelection(0, false);
        spnGenero.setSelection(0, false);
        updatingUi = false;

        // Foto
        currentImgPath = "N/A";
        currentPhotoPath = "";
        ivFoto.setImageResource(R.drawable.ic_person);

        Toast.makeText(requireContext(), getString(R.string.form_limpio), Toast.LENGTH_SHORT).show();
    }
}
