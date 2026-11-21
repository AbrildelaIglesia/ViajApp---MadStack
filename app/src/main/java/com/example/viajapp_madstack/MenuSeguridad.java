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

        // APLICAR ACCESIBILIDAD AL MENÚ SEGURIDAD
        aplicarAccesibilidadSeguridad(
                formulario,
                modoContraste,
                escalaTexto
        );

        // BORDES AZULES EN MODO NORMAL
        if (modoContraste == 0) {

            int[] campos = {
                    R.id.contenedorClaveActual,
                    R.id.contenedorClaveNueva,
                    R.id.contenedorConfirmarClave
            };

            for (int id : campos) {
                View campo = formulario.findViewById(id);

                if (campo != null) {
                    android.graphics.drawable.GradientDrawable fondo =
                            new android.graphics.drawable.GradientDrawable();

                    fondo.setColor(Color.WHITE);
                    fondo.setCornerRadius(dp(actividad, 12));
                    fondo.setStroke(
                            dp(actividad, 1),
                            Color.parseColor("#60A5FA")
                    );

                    campo.setBackground(fondo);
                }
            }
        }


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

        TextView avisoActual =
                formulario.findViewById(R.id.txtErrorClaveActual);

        TextView avisoNueva =
                formulario.findViewById(R.id.txtErrorClaveNueva);

        TextView avisoConfirmar =
                formulario.findViewById(R.id.txtErrorConfirmarClave);

// Color legible en los tres contrastes
        int colorError = modoContraste == 2
                ? Color.parseColor("#FF8A80")
                : Color.parseColor("#B3261E");

        avisoActual.setTextColor(colorError);
        avisoNueva.setTextColor(colorError);
        avisoConfirmar.setTextColor(colorError);


        // BOTONES
        MaterialButton btnGuardar =
                formulario.findViewById(R.id.btnGuardarSeguridad);

        // COLOR DEL BOTÓN SEGÚN EL CONTRASTE
        if (modoContraste == 1) {

            // CREMA: MARRÓN
            btnGuardar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#8B5A2B")
                    )
            );

        } else if (modoContraste == 2) {

            // OSCURO: NEGRO CON BORDE BLANCO
            btnGuardar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#111111")
                    )
            );

            btnGuardar.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(Color.WHITE)
            );

            btnGuardar.setStrokeWidth(dp(actividad, 1));
        }

        btnGuardar.setTextColor(Color.WHITE);

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
            mostrarErrorSeguridad(layoutActual, avisoActual, null);
            mostrarErrorSeguridad(layoutNueva, avisoNueva, null);
            mostrarErrorSeguridad(layoutConfirmar, avisoConfirmar, null);


            String actual = obtenerTexto(edtActual);
            String nueva = obtenerTexto(edtNueva);
            String confirmar = obtenerTexto(edtConfirmar);

            // VALIDAR CAMPOS OBLIGATORIOS

            if (actual.isEmpty()) {
                mostrarErrorSeguridad(
                        layoutActual,
                        avisoActual,
                        "Ingresá tu contraseña actual"
                );
                edtActual.requestFocus();
                return;
            }



            if (nueva.isEmpty()) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "Ingresá una contraseña nueva"
                );
                edtNueva.requestFocus();
                return;
            }


            if (nueva.length() < 8) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "La contraseña debe tener al menos 8 caracteres"
                );
                edtNueva.requestFocus();
                return;
            }


            // VALIDAR LETRA MAYÚSCULA

            if (!nueva.matches(".*[A-Z].*")) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "Debe contener al menos una letra mayúscula"
                );
                edtNueva.requestFocus();
                return;
            }



            if (!nueva.matches(".*[a-z].*")) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "Debe contener al menos una letra minúscula"
                );
                edtNueva.requestFocus();
                return;
            }


            // VALIDAR NÚMERO

            if (!nueva.matches(".*[0-9].*")) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "Debe contener al menos un número"
                );
                edtNueva.requestFocus();
                return;
            }

            // VALIDAR CARÁCTER ESPECIAL

            if (!nueva.matches(".*[^a-zA-Z0-9].*")) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "Debe contener al menos un carácter especial"
                );
                edtNueva.requestFocus();
                return;
            }


            // EVITAR REUTILIZAR LA MISMA CONTRASEÑA

            if (actual.equals(nueva)) {
                mostrarErrorSeguridad(
                        layoutNueva,
                        avisoNueva,
                        "La nueva contraseña debe ser diferente de la actual"
                );
                edtNueva.requestFocus();
                return;
            }

            if (confirmar.isEmpty()) {
                mostrarErrorSeguridad(
                        layoutConfirmar,
                        avisoConfirmar,
                        "Confirmá tu nueva contraseña"
                );
                edtConfirmar.requestFocus();
                return;
            }



            if (!nueva.equals(confirmar)) {
                mostrarErrorSeguridad(
                        layoutConfirmar,
                        avisoConfirmar,
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
                                "Actualizando..."
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

                        mostrarErrorSeguridad(
                                layoutActual,
                                avisoActual,
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


    private static void mostrarErrorSeguridad(
            TextInputLayout layout,
            TextView aviso,
            String mensaje) {

        // Evitar que el mensaje aparezca dentro del campo
        layout.setError(null);

        if (mensaje == null || mensaje.isEmpty()) {
            aviso.setText("");
            aviso.setVisibility(View.GONE);
        } else {
            aviso.setText(mensaje);
            aviso.setVisibility(View.VISIBLE);
            aviso.announceForAccessibility(mensaje);
        }
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


    // ACCESIBILIDAD DEL FORMULARIO SEGURIDAD
    private static void aplicarAccesibilidadSeguridad(
            View vista, int modo, float escala) {

        int fondo = Color.parseColor(
                modo == 2 ? "#111111" :
                        modo == 1 ? "#FFF1D6" : "#FFFFFF");

        int texto = Color.parseColor(
                modo == 2 ? "#FFFFFF" : "#111827");

        int secundario = Color.parseColor(
                modo == 2 ? "#E2E8F0" :
                        modo == 1 ? "#49351F" : "#64748B");

        int borde = Color.parseColor(
                modo == 2 ? "#777777" :
                        modo == 1 ? "#96754D" : "#B6C6E3");

        int campo = Color.parseColor(
                modo == 2 ? "#252525" :
                        modo == 1 ? "#F3DFC0" : "#FFFFFF");

        // FONDO PRINCIPAL
        if (modo != 0 && vista.getId() == R.id.tarjetaSeguridad) {

            com.google.android.material.card.MaterialCardView tarjeta =
                    (com.google.android.material.card.MaterialCardView) vista;

            tarjeta.setCardBackgroundColor(fondo);
            tarjeta.setStrokeColor(borde);
        }

        // CAMPOS DE CONTRASEÑA
        if (modo != 0 &&
                (vista.getId() == R.id.contenedorClaveActual ||
                        vista.getId() == R.id.contenedorClaveNueva ||
                        vista.getId() == R.id.contenedorConfirmarClave)) {

            android.graphics.drawable.GradientDrawable dibujo =
                    new android.graphics.drawable.GradientDrawable();

            dibujo.setColor(campo);

            float densidad = vista.getResources()
                    .getDisplayMetrics().density;

            dibujo.setCornerRadius(12 * densidad);
            dibujo.setStroke(Math.round(densidad), borde);

            vista.setBackground(dibujo);
        }

        // SEPARADOR
        if (modo != 0 && vista.getId() == R.id.divisorSeguridad) {
            vista.setBackgroundColor(borde);
        }

        // TEXTOS
        if (vista instanceof TextView) {

            TextView tv = (TextView) vista;

            tv.setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    tv.getTextSize() * escala
            );

            if (modo != 0 && !(tv instanceof MaterialButton)) {

                boolean enlace =
                        tv.getId() == R.id.txtVolverSeguridad;

                tv.setTextColor(
                        enlace
                                ? Color.parseColor(
                                modo == 2 ? "#FFFFFF" :
                                modo == 1 ? "#664700" : "#2563EB")
                                : texto
                );

                if (tv instanceof TextInputEditText) {
                    tv.setHintTextColor(secundario);
                }
            }
        }

        // OJITOS Y MICRÓFONOS
        if (modo != 0 && vista instanceof ImageView) {
            ImageView icono = (ImageView) vista;
            icono.setImageTintList(
                    android.content.res.ColorStateList.valueOf(texto)
            );
        }

        // RECORRER ELEMENTOS
        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                aplicarAccesibilidadSeguridad(
                        grupo.getChildAt(i),
                        modo,
                        escala
                );
            }
        }
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