package com.example.viajapp_madstack;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MenuSeguridad {

    public static void mostrar(Activity actividad,
                               View botonPerfil,
                               int modoContraste,
                               float escalaTexto) {

        // CARGAR EL DISEÑO DE SEGURIDAD
        View formulario = LayoutInflater.from(actividad)
                .inflate(R.layout.menu_seguridad, null);

        // CREAR VENTANA EMERGENTE
        PopupWindow popup = new PopupWindow(
                formulario,
                dp(actividad, 320),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popup.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.setElevation(dp(actividad, 8));

        // OBTENER LOS CAMPOS
        TextInputEditText edtActual =
                formulario.findViewById(R.id.edtClaveActual);

        TextInputEditText edtNueva =
                formulario.findViewById(R.id.edtClaveNueva);

        TextInputEditText edtConfirmar =
                formulario.findViewById(R.id.edtConfirmarClave);

        // OBTENER LOS CONTENEDORES DE ERRORES
        TextInputLayout layoutActual =
                formulario.findViewById(R.id.inputLayoutClaveActual);

        TextInputLayout layoutNueva =
                formulario.findViewById(R.id.inputLayoutClaveNueva);

        TextInputLayout layoutConfirmar =
                formulario.findViewById(R.id.inputLayoutConfirmarClave);

        // BOTONES
        MaterialButton btnGuardar =
                formulario.findViewById(R.id.btnGuardarSeguridad);

        TextView btnVolver =
                formulario.findViewById(R.id.txtVolverSeguridad);

        // VOLVER
        btnVolver.setOnClickListener(v -> {
            popup.dismiss();

            botonPerfil.post(() ->
                    MenuPerfilUsuario.mostrar(
                            actividad,
                            botonPerfil,
                            modoContraste,
                            escalaTexto
                    )
            );
        });

        // GUARDAR NUEVA CONTRASEÑA
        btnGuardar.setOnClickListener(v -> {

            // LIMPIAR ERRORES ANTERIORES
            layoutActual.setError(null);
            layoutNueva.setError(null);
            layoutConfirmar.setError(null);

            String actual = obtenerTexto(edtActual);
            String nueva = obtenerTexto(edtNueva);
            String confirmar = obtenerTexto(edtConfirmar);

            // VALIDAR CAMPOS OBLIGATORIOS
            if (actual.isEmpty()) {
                layoutActual.setError(
                        "Ingresá tu contraseña actual"
                );
                edtActual.requestFocus();
                return;
            }

            if (nueva.isEmpty()) {
                layoutNueva.setError(
                        "Ingresá una contraseña nueva"
                );
                edtNueva.requestFocus();
                return;
            }

            // VALIDAR LONGITUD
            if (nueva.length() < 8) {
                layoutNueva.setError(
                        "La contraseña debe tener al menos 8 caracteres"
                );
                edtNueva.requestFocus();
                return;
            }

            // EVITAR REUTILIZAR LA MISMA CONTRASEÑA
            if (actual.equals(nueva)) {
                layoutNueva.setError(
                        "La nueva contraseña debe ser diferente de la actual"
                );
                edtNueva.requestFocus();
                return;
            }

            if (confirmar.isEmpty()) {
                layoutConfirmar.setError(
                        "Confirmá tu nueva contraseña"
                );
                edtConfirmar.requestFocus();
                return;
            }

            if (!nueva.equals(confirmar)) {
                layoutConfirmar.setError(
                        "Las contraseñas no coinciden"
                );
                edtConfirmar.requestFocus();
                return;
            }

            // OBTENER USUARIO ACTUAL
            FirebaseUser usuario =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (usuario == null) {
                mostrarMensaje(
                        actividad,
                        "No hay una sesión iniciada"
                );
                return;
            }

            String correo = usuario.getEmail();

            if (correo == null || correo.isEmpty()) {
                mostrarMensaje(
                        actividad,
                        "No se pudo obtener el correo de la cuenta"
                );
                return;
            }

            // EVITAR DOBLE ENVÍO
            btnGuardar.setEnabled(false);
            btnGuardar.setText("Verificando...");

            // VERIFICAR CONTRASEÑA ACTUAL
            AuthCredential credencial =
                    EmailAuthProvider.getCredential(
                            correo,
                            actual
                    );

            usuario.reauthenticate(credencial)
                    .addOnSuccessListener(resultado -> {

                        if (actividad.isFinishing()
                                || actividad.isDestroyed()
                                || !popup.isShowing()) {
                            return;
                        }

                        btnGuardar.setText(
                                "Actualizando contraseña..."
                        );

                        // GUARDAR CONTRASEÑA EN FIREBASE AUTH
                        usuario.updatePassword(nueva)
                                .addOnSuccessListener(vacio -> {

                                    if (actividad.isFinishing()
                                            || actividad.isDestroyed()) {
                                        return;
                                    }

                                    // LIMPIAR LOS CAMPOS
                                    edtActual.setText("");
                                    edtNueva.setText("");
                                    edtConfirmar.setText("");

                                    popup.dismiss();

                                    mostrarMensaje(
                                            actividad,
                                            "Contraseña actualizada correctamente"
                                    );
                                })
                                .addOnFailureListener(error -> {

                                    if (actividad.isFinishing()
                                            || actividad.isDestroyed()) {
                                        return;
                                    }

                                    btnGuardar.setEnabled(true);
                                    btnGuardar.setText(
                                            "Actualizar contraseña"
                                    );

                                    mostrarMensaje(
                                            actividad,
                                            "No se pudo actualizar la contraseña. Intentá nuevamente."
                                    );
                                });
                    })
                    .addOnFailureListener(error -> {

                        if (actividad.isFinishing()
                                || actividad.isDestroyed()) {
                            return;
                        }

                        btnGuardar.setEnabled(true);
                        btnGuardar.setText(
                                "Actualizar contraseña"
                        );

                        layoutActual.setError(
                                "No se pudo verificar la contraseña actual"
                        );
                    });
        });


        popup.setAnimationStyle(android.R.style.Animation_Dialog);

        popup.showAsDropDown(
                botonPerfil,
                0,
                dp(actividad, 8),
                Gravity.END
        );
    }

    // OBTENER TEXTO DE LOS CAMPOS
    private static String obtenerTexto(
            TextInputEditText campo) {

        if (campo.getText() == null) {
            return "";
        }

        return campo.getText().toString();
    }

    // MOSTRAR MENSAJES AL USUARIO
    private static void mostrarMensaje(
            Activity actividad,
            String mensaje) {

        Toast.makeText(
                actividad,
                mensaje,
                Toast.LENGTH_LONG
        ).show();
    }

    // CONVERTIR DP A PÍXELES
    private static int dp(
            Activity actividad,
            int valor) {

        return Math.round(
                valor * actividad.getResources()
                        .getDisplayMetrics().density
        );
    }
}
