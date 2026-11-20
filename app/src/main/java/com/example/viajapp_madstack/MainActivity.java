package com.example.viajapp_madstack;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- 1. CONFIGURACIÓN DEL OJITO DE LA CONTRASEÑA ---
        EditText etContrasena = findViewById(R.id.etContrasena);
        ImageView btnTogglePassword = findViewById(R.id.btnTogglePassword);

        if (etContrasena != null && btnTogglePassword != null) {
            final boolean[] isPasswordVisible = {false};

            btnTogglePassword.setOnClickListener(v -> {
                isPasswordVisible[0] = !isPasswordVisible[0];

                if (isPasswordVisible[0]) {
                    // Muestra el texto plano (desoculta la contraseña)
                    etContrasena.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                    // btnTogglePassword.setImageResource(R.drawable.visible_true); // Descomenta si usas icono de ojo abierto
                } else {
                    // Oculta la contraseña con asteriscos
                    etContrasena.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                    // btnTogglePassword.setImageResource(R.drawable.visible_false); // Descomenta si usas icono de ojo cerrado
                }

                // Mantiene el cursor al final del texto
                etContrasena.setSelection(etContrasena.getText().length());
            });
        }

        // --- 2. CONFIGURACIÓN DE INSETS (Diseño responsivo del sistema) ---
        View content = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return windowInsets;
        });
    }
}
