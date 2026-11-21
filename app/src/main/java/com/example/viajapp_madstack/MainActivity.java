
package com.example.viajapp_madstack;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

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
                    etContrasena.setTransformationMethod(
                            android.text.method.HideReturnsTransformationMethod.getInstance()
                    );
                    // btnTogglePassword.setImageResource(R.drawable.visible_true); // Descomenta si usas icono de ojo abierto
                } else {
                    // Oculta la contraseña con asteriscos
                    etContrasena.setTransformationMethod(
                            android.text.method.PasswordTransformationMethod.getInstance()
                    );
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

        // --- 3. RECUPERACIÓN DE CONTRASEÑA ---
        TextView txtRecuperarContrasena =
                findViewById(R.id.txtRecuperarContrasena);

        if (txtRecuperarContrasena != null) {
            txtRecuperarContrasena.setOnClickListener(v -> {
                mostrarDialogoRecuperacion();
            });
        }
    }

    // --- 4. DIÁLOGO PARA RECUPERAR CONTRASEÑA ---
    private void mostrarDialogoRecuperacion() {

        EditText inputCorreo = new EditText(this);
        inputCorreo.setHint("Correo electrónico");
        inputCorreo.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        inputCorreo.setSingleLine(true);

        // Si el usuario ya escribió su correo, lo recuperamos
        EditText etCorreo = findViewById(R.id.etCorreo);

        if (etCorreo != null) {
            inputCorreo.setText(etCorreo.getText().toString().trim());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage(
                        "Ingresá tu correo electrónico para recibir un enlace de recuperación."
                )
                .setView(inputCorreo)
                .setPositiveButton("Enviar correo", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String correo = inputCorreo.getText()
                                .toString()
                                .trim();

                        // Validación del correo
                        if (correo.isEmpty() ||
                                !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

                            inputCorreo.setError("Ingresá un correo válido");
                            return;
                        }

                        // Envío del correo mediante Firebase Authentication
                        FirebaseAuth.getInstance()
                                .sendPasswordResetEmail(correo)
                                .addOnCompleteListener(task -> {

                                    if (task.isSuccessful()) {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Si el correo corresponde a una cuenta, recibirás un enlace de recuperación.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        dialog.dismiss();

                                    } else {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "No se pudo enviar el correo. Intentá nuevamente.",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });
                    });
        });

        dialog.show();
    }
}
