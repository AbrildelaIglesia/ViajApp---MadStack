package com.example.viajapp_madstack;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MenuSeguridad {

    public static final int REQ_DICTADO_CLAVE_ACTUAL = 4101;
    public static final int REQ_DICTADO_CLAVE_NUEVA = 4102;
    public static final int REQ_DICTADO_CONFIRMAR_CLAVE = 4103;

    private static WeakReference<PopupWindow> popupActivoRef = new WeakReference<>(null);
    private static WeakReference<TextInputEditText> edtActualRef = new WeakReference<>(null);
    private static WeakReference<TextInputEditText> edtNuevaRef = new WeakReference<>(null);
    private static WeakReference<TextInputEditText> edtConfirmarRef = new WeakReference<>(null);

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

        // GUARDAR REFERENCIAS ACTIVAS PARA EL DICTADO POR VOZ
        popupActivoRef = new WeakReference<>(popup);
        edtActualRef = new WeakReference<>(edtActual);
        edtNuevaRef = new WeakReference<>(edtNueva);
        edtConfirmarRef = new WeakReference<>(edtConfirmar);

        popup.setOnDismissListener(() -> {
            if (popupActivoRef.get() == popup) {
                popupActivoRef.clear();
                edtActualRef.clear();
                edtNuevaRef.clear();
                edtConfirmarRef.clear();
            }
        });

        configurarVisibilidadYMicrofonos(
                actividad,
                edtActual,
                edtNueva,
                edtConfirmar,
                formulario.findViewById(R.id.btnToggleClaveActual),
                formulario.findViewById(R.id.btnToggleClaveNueva),
                formulario.findViewById(R.id.btnToggleConfirmarClave),
                formulario.findViewById(R.id.btnMicClaveActual),
                formulario.findViewById(R.id.btnMicClaveNueva),
                formulario.findViewById(R.id.btnMicConfirmarClave)
        );

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

            // VALIDAR LETRA MAYÚSCULA
            if (!nueva.matches(".*[A-Z].*")) {
                layoutNueva.setError(
                        "Debe contener al menos una letra mayúscula"
                );
                edtNueva.requestFocus();
                return;
            }

            // VALIDAR LETRA MINÚSCULA
            if (!nueva.matches(".*[a-z].*")) {
                layoutNueva.setError(
                        "Debe contener al menos una letra minúscula"
                );
                edtNueva.requestFocus();
                return;
            }

            // VALIDAR NÚMERO
            if (!nueva.matches(".*[0-9].*")) {
                layoutNueva.setError(
                        "Debe contener al menos un número"
                );
                edtNueva.requestFocus();
                return;
            }

            // VALIDAR CARÁCTER ESPECIAL
            if (!nueva.matches(".*[^a-zA-Z0-9].*")) {
                layoutNueva.setError(
                        "Debe contener al menos un carácter especial"
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

    private static void configurarVisibilidadYMicrofonos(
            Activity actividad,
            TextInputEditText edtActual,
            TextInputEditText edtNueva,
            TextInputEditText edtConfirmar,
            ImageView btnToggleActual,
            ImageView btnToggleNueva,
            ImageView btnToggleConfirmar,
            ImageView btnMicActual,
            ImageView btnMicNueva,
            ImageView btnMicConfirmar) {

        configurarTogglePassword(edtActual, btnToggleActual);
        configurarTogglePassword(edtNueva, btnToggleNueva);
        configurarTogglePassword(edtConfirmar, btnToggleConfirmar);

        if (btnMicActual != null) {
            btnMicActual.setOnClickListener(v ->
                    VoiceUtils.iniciarDictado(
                            actividad,
                            REQ_DICTADO_CLAVE_ACTUAL,
                            "Dictá tu contraseña actual"
                    )
            );
        }

        if (btnMicNueva != null) {
            btnMicNueva.setOnClickListener(v ->
                    VoiceUtils.iniciarDictado(
                            actividad,
                            REQ_DICTADO_CLAVE_NUEVA,
                            "Dictá tu nueva contraseña"
                    )
            );
        }

        if (btnMicConfirmar != null) {
            btnMicConfirmar.setOnClickListener(v ->
                    VoiceUtils.iniciarDictado(
                            actividad,
                            REQ_DICTADO_CONFIRMAR_CLAVE,
                            "Repetí tu nueva contraseña por voz"
                    )
            );
        }
    }

    private static void configurarTogglePassword(TextInputEditText campo, ImageView toggle) {
        if (campo == null || toggle == null) return;

        toggle.setOnClickListener(v -> {
            boolean esVisible = (campo.getInputType() & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                    == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

            if (esVisible) {
                campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggle.setImageResource(R.drawable.visible_false);
                toggle.setContentDescription("Mostrar contraseña");
            } else {
                campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggle.setImageResource(R.drawable.visible_true);
                toggle.setContentDescription("Ocultar contraseña");
            }

            if (campo.getText() != null) {
                campo.setSelection(campo.getText().length());
            }
        });
    }

    public static boolean manejarResultadoDictado(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return false;
        }

        TextInputEditText destino = null;
        if (requestCode == REQ_DICTADO_CLAVE_ACTUAL) {
            destino = edtActualRef.get();
        } else if (requestCode == REQ_DICTADO_CLAVE_NUEVA) {
            destino = edtNuevaRef.get();
        } else if (requestCode == REQ_DICTADO_CONFIRMAR_CLAVE) {
            destino = edtConfirmarRef.get();
        }

        if (destino == null) {
            return false;
        }

        ArrayList<String> resultados = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (resultados != null && !resultados.isEmpty()) {
            String textoProcesado = VoiceUtils.procesarTextoDictado(
                    resultados.get(0),
                    VoiceUtils.TIPO_PASSWORD
            );
            destino.setText(textoProcesado);
            destino.setSelection(textoProcesado.length());
            destino.requestFocus();
            return true;
        }

        return false;
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