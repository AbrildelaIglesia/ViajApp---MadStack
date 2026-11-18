
package com.example.viajapp_madstack;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuEditarPerfil {

    public static void mostrar(
            Activity actividad,
            View botonPerfil,
            int modoContraste,
            float escalaTexto) {

        // CARGAR FORMULARIO
        View formulario = LayoutInflater.from(actividad)
                .inflate(R.layout.menu_editar_perfil, null);

        // CREAR DESPLEGABLE
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
        popup.setAnimationStyle(
                android.R.style.Animation_Dialog
        );


        // ESCUCHA PARA DETECTAR EL REGRESO DESDE GMAIL
        final ViajAppApplication.EscuchaRegreso[] escuchaCorreo =
                new ViajAppApplication.EscuchaRegreso[1];

        ViajAppApplication aplicacion =
                (ViajAppApplication) actividad.getApplication();

        // EVITAR QUE LA ESCUCHA QUEDE ACTIVA
        // SI SE CIERRA EDITAR PERFIL
        popup.setOnDismissListener(() -> {

            if (escuchaCorreo[0] != null) {
                aplicacion.quitarEscuchaRegreso(
                        escuchaCorreo[0]
                );

                escuchaCorreo[0] = null;
            }
        });


        // CAMPOS
        TextInputEditText edtNombre =
                formulario.findViewById(
                        R.id.edtNombreApellidoPerfil
                );

        TextInputEditText edtCorreo =
                formulario.findViewById(
                        R.id.edtCorreoPerfil
                );

        // CONTENEDORES DE ERRORES
        TextInputLayout layoutNombre =
                formulario.findViewById(
                        R.id.inputLayoutNombreApellidoPerfil
                );

        TextInputLayout layoutCorreo =
                formulario.findViewById(
                        R.id.inputLayoutCorreoPerfil
                );

        // BOTONES
        MaterialButton btnGuardar =
                formulario.findViewById(
                        R.id.btnGuardarEditarPerfil
                );

        TextView btnVolver =
                formulario.findViewById(
                        R.id.txtVolverMiCuenta
                );

        // VOLVER AL MENÚ MI CUENTA
        btnVolver.setOnClickListener(v -> {
            popup.dismiss();

            botonPerfil.post(() -> {
                if (!actividad.isFinishing()
                        && !actividad.isDestroyed()) {

                    MenuPerfilUsuario.mostrar(
                            actividad,
                            botonPerfil,
                            modoContraste,
                            escalaTexto
                    );
                }
            });
        });

        // GUARDAR EL NOMBRE ORIGINAL DEL PERFIL
        final String[] nombreOriginal = {null};

        // CARGAR DATOS EXISTENTES
        cargarDatosUsuario(
                actividad,
                edtNombre,
                edtCorreo,
                nombre -> nombreOriginal[0] = nombre
        );

        // GUARDAR CAMBIOS
        btnGuardar.setOnClickListener(v -> {


            layoutNombre.setError(null);
            layoutNombre.setErrorEnabled(false);

            layoutCorreo.setError(null);
            layoutCorreo.setErrorEnabled(false);


            String nombreCompleto =
                    obtenerTexto(edtNombre).trim();

            String correoNuevo =
                    obtenerTexto(edtCorreo).trim();

            // VALIDAR NOMBRE COMPLETO
            if (nombreCompleto.isEmpty()) {
                layoutNombre.setError(
                        "Ingresá tu nombre y apellido"
                );
                edtNombre.requestFocus();
                return;
            }

            if (nombreCompleto.split("\\s+").length < 2) {
                layoutNombre.setError(
                        "Ingresá nombre y apellido"
                );
                edtNombre.requestFocus();
                return;
            }

            // VALIDAR CORREO
            if (correoNuevo.isEmpty()) {
                layoutCorreo.setError(
                        "Ingresá un correo electrónico"
                );
                edtCorreo.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(
                    correoNuevo).matches()) {

                layoutCorreo.setError(
                        "El correo electrónico no es válido"
                );
                edtCorreo.requestFocus();
                return;
            }

            FirebaseUser usuario =
                    FirebaseAuth.getInstance()
                            .getCurrentUser();

            if (usuario == null) {
                mostrarMensaje(
                        actividad,
                        "No hay una sesión iniciada"
                );
                return;
            }

            String correoActual = usuario.getEmail();

            boolean cambioCorreo =
                    correoActual == null
                            || !correoActual.equalsIgnoreCase(
                            correoNuevo
                    );

            // COMPROBAR QUE EL NOMBRE ORIGINAL YA SE CARGÓ
            if (nombreOriginal[0] == null) {
                mostrarMensaje(
                        actividad,
                        "Esperá a que se carguen los datos del perfil"
                );
                return;
            }

            // COMPROBAR SI CAMBIÓ EL NOMBRE
            boolean cambioNombre =
                    !nombreCompleto.equals(nombreOriginal[0]);

            // SI NO CAMBIÓ NADA, NO GUARDAR EN FIREBASE
            if (!cambioNombre && !cambioCorreo) {

                mostrarMensaje(
                        actividad,
                        "No se detectaron cambios en tu perfil"
                );

                return;
            }

            // EVITAR DOBLE ENVÍO
            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            // ACTUALIZAR NOMBRE SIN BORRAR OTROS DATOS
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(usuario.getUid())
                    .update(
                            "nombreApellidos",
                            nombreCompleto
                    )
                    .addOnSuccessListener(vacio -> {

                        if (actividad.isFinishing()
                                || actividad.isDestroyed()) {
                            return;
                        }

                        if (cambioNombre) {
                            nombreOriginal[0] = nombreCompleto;
                        }

                        // SI EL CORREO NO CAMBIÓ,
                        // FINALIZAMOS EL GUARDADO
                        if (!cambioCorreo) {

                            btnGuardar.setEnabled(true);
                            btnGuardar.setText(
                                    "Guardar cambios"
                            );

                            mostrarMensaje(
                                    actividad,
                                    "Nombre y apellido actualizados correctamente"
                            );

                            return;
                        }

                        // SI CAMBIÓ, PEDIR VERIFICACIÓN

                        btnGuardar.setText(
                                "Enviando verificación..."
                        );

                        // ENVIAR CORREO DE VERIFICACIÓN EN ESPAÑOL
                        FirebaseAuth.getInstance().setLanguageCode("es");

                        usuario.verifyBeforeUpdateEmail(
                                        correoNuevo
                                )

                                .addOnSuccessListener(resultado -> {

                                    if (actividad.isFinishing()
                                            || actividad.isDestroyed()) {
                                        return;
                                    }

                                    btnGuardar.setEnabled(true);
                                    btnGuardar.setText(
                                            "Guardar cambios"
                                    );


                                    // RECORDAR EL CORREO PENDIENTE
                                    // HASTA QUE EL USUARIO LO VERIFIQUE
                                    aplicacion.guardarCorreoPendiente(
                                            usuario.getUid(),
                                            correoNuevo
                                    );


                                    // DETECTAR EL REGRESO DESDE GMAIL
                                    final boolean[] comprobandoCorreo = {false};

                                    escuchaCorreo[0] = actividadRegreso -> {

                                        if (!popup.isShowing()
                                                || actividad.isFinishing()
                                                || actividad.isDestroyed()
                                                || comprobandoCorreo[0]) {
                                            return;
                                        }

                                        comprobandoCorreo[0] = true;

                                        SincronizarCorreoUsuario.comprobarCambioCorreo(
                                                actividad,
                                                correoNuevo,
                                                estado -> {

                                                    comprobandoCorreo[0] = false;

                                                    if (!popup.isShowing()
                                                            || actividad.isFinishing()
                                                            || actividad.isDestroyed()) {
                                                        return;
                                                    }

                                                    switch (estado) {

                                                        case ACTUALIZADO:

                                                            // EL CORREO YA FUE ACTUALIZADO
                                                            // BORRAR EL CAMBIO PENDIENTE
                                                            aplicacion.limpiarCorreoPendiente(
                                                                    usuario.getUid()
                                                            );


                                                            popup.dismiss();

                                                            new androidx.appcompat.app.AlertDialog.Builder(actividad)
                                                                    .setTitle("¡Verificación completada!")
                                                                    .setMessage(
                                                                            "Tu nueva dirección de correo electrónico "
                                                                                    + "fue verificada y actualizada.\n\n"
                                                                                    + "Los datos de tu cuenta ya están sincronizados."
                                                                    )
                                                                    .setCancelable(false)
                                                                    .setPositiveButton(
                                                                            "Volver a Mi cuenta",
                                                                            (dialogo, which) -> {

                                                                                botonPerfil.post(() -> {

                                                                                    if (!actividad.isFinishing()
                                                                                            && !actividad.isDestroyed()) {

                                                                                        MenuPerfilUsuario.mostrar(
                                                                                                actividad,
                                                                                                botonPerfil,
                                                                                                modoContraste,
                                                                                                escalaTexto
                                                                                        );
                                                                                    }
                                                                                });
                                                                            }
                                                                    )
                                                                    .show();
                                                            break;


                                                        case SESION_INVALIDA:

                                                            // EVITAR QUE SE MUESTRE DOS VECES
                                                            // EL AVISO DE INICIO DE SESIÓN
                                                            aplicacion.marcarAvisoSesionMostrado();

                                                            popup.dismiss();

                                                            SincronizarCorreoUsuario
                                                                    .solicitarNuevoInicioSesion(actividad);
                                                            break;


                                                        case PENDIENTE:

                                                            mostrarMensaje(
                                                                    actividad,
                                                                    "El correo todavía está pendiente de verificación."
                                                            );
                                                            break;
                                                        case ERROR:

                                                            new androidx.appcompat.app.AlertDialog.Builder(actividad)
                                                                    .setTitle("No se pudo comprobar el cambio")
                                                                    .setMessage(
                                                                            "No pudimos verificar o sincronizar "
                                                                                    + "tu nuevo correo.\n\n"
                                                                                    + "Revisá tu conexión a Internet "
                                                                                    + "y volvé a ViajAPP para intentarlo nuevamente."
                                                                    )
                                                                    .setPositiveButton("Entendido", null)
                                                                    .show();
                                                            break;
                                                    }
                                                }
                                        );
                                    };

                                    aplicacion.establecerEscuchaRegreso(
                                            escuchaCorreo[0]
                                    );


                                    new androidx.appcompat.app.AlertDialog.Builder(actividad)
                                            .setTitle("¡Correo de verificación enviado!")
                                            .setMessage(
                                                    "Te enviamos un enlace de verificación a:\n\n"
                                                            + correoNuevo
                                                            + "\n\nPara completar el cambio:\n\n"
                                                            + "1. Abrí tu correo electrónico.\n"
                                                            + "2. Confirmá el cambio desde el enlace recibido.\n"
                                                            + "3. Regresá a ViajAPP.\n\n"
                                                            + "Cuando vuelvas, comprobaremos si tu correo "
                                                            + "se actualizó correctamente."
                                            )
                                            .setPositiveButton("Entendido", null)
                                            .show();

                                })
                                .addOnFailureListener(error -> {

                                    if (actividad.isFinishing()
                                            || actividad.isDestroyed()) {
                                        return;
                                    }

                                    btnGuardar.setEnabled(true);
                                    btnGuardar.setText(
                                            "Guardar cambios"
                                    );

                                    mostrarMensaje(
                                            actividad,
                                            "El nombre se guardó, pero no se pudo solicitar el cambio de correo. Puede ser necesario volver a iniciar sesión. Detalle: "
                                                    + error.getMessage()
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
                                "Guardar cambios"
                        );

                        mostrarMensaje(
                                actividad,
                                "No se pudieron guardar los datos: "
                                        + error.getMessage()
                        );
                    });
        });

        // MOSTRAR DEBAJO DEL ICONO DE PERFIL
        popup.showAsDropDown(
                botonPerfil,
                0,
                dp(actividad, 8),
                Gravity.END
        );
    }

    // =========================================
    // CARGAR DATOS DE FIREBASE
    // =========================================

    private static void cargarDatosUsuario(
            Activity actividad,
            TextInputEditText edtNombre,
            TextInputEditText edtCorreo,
            java.util.function.Consumer<String> alCargarNombre) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            mostrarMensaje(
                    actividad,
                    "No hay una sesión iniciada"
            );
            return;
        }

        // ACTUALIZAR CORREO DESDE FIREBASE
        SincronizarCorreoUsuario.actualizar(
                actividad,
                edtCorreo
        );

        // NOMBRE GUARDADO EN FIRESTORE
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {

                    if (actividad.isFinishing()
                            || actividad.isDestroyed()) {
                        return;
                    }

                    String nombreCompleto =
                            documento.getString(
                                    "nombreApellidos"
                            );

                    // COMPATIBILIDAD CON OTROS CAMPOS
                    String nombre =
                            documento.getString("nombre");

                    String apellido =
                            documento.getString("apellido");

                    if (nombre != null
                            && !nombre.trim().isEmpty()
                            && apellido != null
                            && !apellido.trim().isEmpty()) {

                        nombreCompleto =
                                nombre.trim() + " "
                                        + apellido.trim();
                    }

                    if (nombreCompleto == null
                            || nombreCompleto.trim().isEmpty()) {

                        nombreCompleto =
                                usuario.getDisplayName();
                    }

                    String nombreCargado =
                            nombreCompleto != null
                                    ? nombreCompleto
                                    : "";

                    edtNombre.setText(nombreCargado);

                    // RECORDAR EL VALOR ORIGINAL PARA COMPARARLO
                    alCargarNombre.accept(nombreCargado.trim());
                })

                .addOnFailureListener(error -> {

                    if (!actividad.isFinishing()
                            && !actividad.isDestroyed()) {

                        mostrarMensaje(
                                actividad,
                                "No se pudieron cargar los datos del perfil"
                        );
                    }
                });

    }

    // OBTENER TEXTO

    private static String obtenerTexto(
            TextInputEditText campo) {

        if (campo.getText() == null) {
            return "";
        }

        return campo.getText().toString();
    }

    // MOSTRAR MENSAJES
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
