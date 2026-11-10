
package com.example.viajapp_madstack;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;

public class InicioActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private TextView txtRegistrarse;
    private LinearLayout btnIniciarSesion;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v -> {

        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        // Validar correo
        if (!validarCorreo(correo)) {
            return;
        }

        // Validar contraseña
        if (contrasena.isEmpty()) {
            etContrasena.setError("Ingrese su contraseña");
            etContrasena.requestFocus();
            return;
        }

        // Consultar Firebase Authentication
        mAuth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this, task -> {

                if (task.isSuccessful()) {

                    Toast.makeText(
                            InicioActivity.this,
                            "¡Bienvenido!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(InicioActivity.this, AccesibilidadActivity.class);
                    startActivity(intent);

                } else {

                    Toast.makeText(
                            InicioActivity.this,
                            "No se pudo iniciar sesión. Verificá tu correo y contraseña.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });

        // BOTÓN "REGISTRATE ACÁ"
        txtRegistrarse = findViewById(R.id.txtRegistrarse);

        txtRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InicioActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validarCorreo(String correo)
    {
        if (correo.isEmpty()) {
            etCorreo.setError("Ingrese su correo");
            etCorreo.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("El formato de correo es inválido");
            etCorreo.requestFocus();
            return false;
        }
        return true;
    }

}

