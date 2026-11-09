package com.example.viajapp_madstack;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class RegistroActivity extends AppCompatActivity {
    private EditText etNombreApellidos, etCorreo, etContrasena, etConfirmarContrasena;
    private CheckBox cbSillaRuedas, cbRecorridosCortos, cbVisionReducida,
            cbSubtitulos, cbSinAdaptaciones, cbTerminos;
    private LinearLayout btnCrearCuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        etNombreApellidos = findViewById(R.id.etNombreApellidos);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena);
        cbSillaRuedas = findViewById(R.id.cbSillaRuedas);
        cbRecorridosCortos = findViewById(R.id.cbRecorridosCortos);
        cbVisionReducida = findViewById(R.id.cbVisionReducida);
        cbSubtitulos = findViewById(R.id.cbSubtitulos);
        cbSinAdaptaciones = findViewById(R.id.cbSinAdaptaciones);
        cbTerminos = findViewById(R.id.cbTerminos);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        btnCrearCuenta.setOnClickListener(v ->
        {
            String nombre = etNombreApellidos.getText().toString().trim().replaceAll(" +", " ");
            nombre = formatearNombre(nombre);
            etNombreApellidos.setText(nombre);
            String correo = etCorreo.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();
            String confirmarContrasena = etConfirmarContrasena.getText().toString().trim();
            if (!validarNombre(nombre)) {
                return;
            }
        });
    }
    private boolean validarNombre(String nombre)
    {
        if (nombre.isEmpty()) {
            etNombreApellidos.setError("Ingrese su nombre y apellido");
            etNombreApellidos.requestFocus();
            return false;
        }
        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+")) {
            etNombreApellidos.setError("El nombre y apellido solo pueden contener letras");
            etNombreApellidos.requestFocus();
            return false;
        }
        if (nombre.length() < 2 || nombre.length() > 100) {
            etNombreApellidos.setError("El nombre y apellido deben tener entre 2 y 100 caracteres");
            etNombreApellidos.requestFocus();
            return false;
        }

        return true;
    }

    private String formatearNombre(String nombre) {

        String[] palabras = nombre.toLowerCase().split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                if (resultado.length() > 0) {
                    resultado.append(" ");
                }

                if (palabra.equals("de") || palabra.equals("del")
                        || palabra.equals("la") || palabra.equals("las")
                        || palabra.equals("los")) {
                    resultado.append(palabra);
                } else {
                    resultado.append(palabra.substring(0, 1).toUpperCase())
                            .append(palabra.substring(1));
                }
            }
        }

        return resultado.toString();
    }
    
}