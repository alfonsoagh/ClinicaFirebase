package com.example.clinicasx.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clinicasx.MainActivity;
import com.example.clinicasx.R;
import com.example.clinicasx.db.SQLite;
import com.example.clinicasx.sync.SyncManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailField, passwordField;
    private ProgressBar progressBar;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            goToMain();
            return;
        }

        emailField = findViewById(R.id.inputEmail);
        passwordField = findViewById(R.id.inputPassword);
        progressBar = findViewById(R.id.progress);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        View.OnClickListener handler = v -> {
            String email = emailField.getText().toString().trim();
            String pass = passwordField.getText().toString().trim();

            boolean ok = validate(email, pass);
            if (!ok) return;

            setLoading(true);
            if (v.getId() == R.id.btnLogin) doLogin(email, pass); else doRegister(email, pass);
        };

        btnLogin.setOnClickListener(handler);
        btnRegister.setOnClickListener(handler);

        TextWatcher clearErrors = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailField.setError(null);
                passwordField.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        emailField.addTextChangedListener(clearErrors);
        passwordField.addTextChangedListener(clearErrors);
    }

    private boolean validate(String email, String pass) {
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            emailField.setError(getString(R.string.requerido));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError(getString(R.string.email)); // idealmente un string específico de error de formato
            valid = false;
        }
        if (TextUtils.isEmpty(pass)) {
            passwordField.setError(getString(R.string.requerido));
            valid = false;
        } else if (pass.length() < 6) {
            passwordField.setError(getString(R.string.register_failed)); // placeholder; ideal: crear string "password_short"
            valid = false;
        }
        return valid;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        emailField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    private void doLogin(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        afterAuthCleanup();
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void doRegister(String email, String pass) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        afterAuthCleanup();
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.register_failed), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void afterAuthCleanup() {
        try {
            FirebaseUser u = mAuth.getCurrentUser();
            if (u != null) {
                String uid = u.getUid();
                SQLite db = new SQLite(this);
                db.setUserId(uid);
                db.abrir();
                db.clearForUidSwitch(uid); // borra pacientes locales de otros usuarios
                db.cerrar();

                // Resetear last_sync para este UID y disparar un pull completo inmediatamente
                SyncManager.setLastSync(this, uid, "1970-01-01T00:00:00Z");
                SyncManager.enqueueNow(this);
            }
        } catch (Exception ignored) {}
    }

    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
