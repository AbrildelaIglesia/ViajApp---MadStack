
package com.example.viajapp_madstack;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InicioActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private TextView txtRegistrarse;
    private LinearLayout btnIniciarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v ->
        {
            String correo = etCorreo.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();
            if (!validarCorreo(correo)) {
                return;
            }
            etCorreo.setText(correo);
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

