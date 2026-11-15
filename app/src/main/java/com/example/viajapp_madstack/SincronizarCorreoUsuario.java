
package com.example.viajapp_madstack;

import android.app.Activity;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class SincronizarCorreoUsuario {


    // EVITAR DOS AVISOS DE SESIÓN AL MISMO TIEMPO
    private static boolean avisoSesionEnPantalla = false;


    // USADO POR MI CUENTA Y EDITAR PERFIL
    public static void actualizar(
            Activity actividad,
            TextView campoCorreo) {

        sincronizar(actividad, campoCorreo);
    }

    // PARA COMPROBAR EL CORREO AL VOLVER A VIAJAPP
    public static void verificarAlVolver(Activity actividad) {

        sincronizar(actividad, null);
    }

    private static void sincronizar(
            Activity actividad,
            TextView campoCorreo) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        String uid = usuario.getUid();

        // Mostrar los datos actuales mientras consultamos Firebase
        if (campoCorreo != null && actividadValida(actividad)) {
            campoCorreo.setText(
                    usuario.getEmail() != null
                            ? usuario.getEmail() : ""
            );
        }

        // ACTUALIZAR DATOS DESDE FIREBASE AUTHENTICATION
        usuario.reload().addOnCompleteListener(tarea -> {

            if (!actividadValida(actividad)) {
                return;
            }

            if (!tarea.isSuccessful()) {

                Exception error = tarea.getException();

                if (sesionInvalida(error)) {
                    mostrarDialogoSesion(actividad);

                } else if (error instanceof FirebaseNetworkException) {
                    mostrarMensaje(
                            actividad,
                            "No se pudo comprobar el correo. Revisá tu conexión a Internet."
                    );

                } else {
                    mostrarMensaje(
                            actividad,
                            "No se pudo actualizar la cuenta. Intentá nuevamente."
                    );
                }

                return;
            }

            FirebaseUser usuarioActual =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (usuarioActual == null
                    || !uid.equals(usuarioActual.getUid())) {
                return;
            }

            String correoConfirmado = usuarioActual.getEmail();

            if (correoConfirmado == null
                    || correoConfirmado.trim().isEmpty()) {
                return;
            }

            // ACTUALIZAR CORREO EN EL FORMULARIO O MENÚ
            if (campoCorreo != null) {
                campoCorreo.setText(correoConfirmado);
            }

            // COMPROBAR EL CORREO GUARDADO EN FIRESTORE
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener(documento -> {

                        FirebaseUser sesion =
                                FirebaseAuth.getInstance()
                                        .getCurrentUser();

                        if (!actividadValida(actividad)
                                || sesion == null
                                || !uid.equals(sesion.getUid())) {
                            return;
                        }

                        if (!documento.exists()) {
                            mostrarMensaje(
                                    actividad,
                                    "No se encontró el perfil en Firestore."
                            );
                            return;
                        }

                        String correoGuardado =
                                documento.getString("correo");

                        // YA ESTÁ SINCRONIZADO
                        if (correoConfirmado.equals(correoGuardado)) {
                            return;
                        }

                        // ACTUALIZAR SOLO EL CORREO
                        FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(uid)
                                .update("correo", correoConfirmado)
                                .addOnFailureListener(error -> {

                                    if (actividadValida(actividad)) {
                                        mostrarMensaje(
                                                actividad,
                                                "No se pudo sincronizar el correo con Firestore."
                                        );
                                    }
                                });
                    })
                    .addOnFailureListener(error -> {

                        if (actividadValida(actividad)) {
                            mostrarMensaje(
                                    actividad,
                                    "No se pudo consultar el perfil en Firestore."
                            );
                        }
                    });
        });
    }


    // RESULTADOS POSIBLES DEL CAMBIO DE CORREO
    public enum EstadoCambioCorreo {
        ACTUALIZADO,
        PENDIENTE,
        SESION_INVALIDA,
        ERROR
    }

    // COMUNICAR EL RESULTADO A EDITAR PERFIL
    public interface RespuestaCambioCorreo {
        void alFinalizar(EstadoCambioCorreo estado);
    }

    // COMPROBAR EL CAMBIO AL REGRESAR DE GMAIL
    public static void comprobarCambioCorreo(
            Activity actividad,
            String correoEsperado,
            RespuestaCambioCorreo respuesta) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            respuesta.alFinalizar(
                    EstadoCambioCorreo.SESION_INVALIDA
            );
            return;
        }

        String uid = usuario.getUid();

        usuario.reload().addOnCompleteListener(tarea -> {

            if (!actividadValida(actividad)) {
                return;
            }

            if (!tarea.isSuccessful()) {

                if (sesionInvalida(tarea.getException())) {
                    respuesta.alFinalizar(
                            EstadoCambioCorreo.SESION_INVALIDA
                    );
                } else {
                    respuesta.alFinalizar(
                            EstadoCambioCorreo.ERROR
                    );
                }
                return;
            }

            FirebaseUser usuarioActual =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (usuarioActual == null
                    || !uid.equals(usuarioActual.getUid())) {
                respuesta.alFinalizar(
                        EstadoCambioCorreo.SESION_INVALIDA
                );
                return;
            }

            String correoActual = usuarioActual.getEmail();

            // TODAVÍA NO SE CONFIRMÓ EL CORREO NUEVO
            if (correoActual == null
                    || !correoActual.equalsIgnoreCase(correoEsperado)) {

                respuesta.alFinalizar(
                        EstadoCambioCorreo.PENDIENTE
                );
                return;
            }

            // CORREO CONFIRMADO: SINCRONIZAR FIRESTORE
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener(documento -> {

                        if (!actividadValida(actividad)) {
                            return;
                        }

                        if (!documento.exists()) {
                            respuesta.alFinalizar(
                                    EstadoCambioCorreo.ERROR
                            );
                            return;
                        }

                        String correoGuardado =
                                documento.getString("correo");

                        if (correoActual.equals(correoGuardado)) {
                            respuesta.alFinalizar(
                                    EstadoCambioCorreo.ACTUALIZADO
                            );
                            return;
                        }

                        FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(uid)
                                .update("correo", correoActual)
                                .addOnSuccessListener(vacio -> {

                                    if (actividadValida(actividad)) {
                                        respuesta.alFinalizar(
                                                EstadoCambioCorreo.ACTUALIZADO
                                        );
                                    }
                                })
                                .addOnFailureListener(error -> {

                                    if (actividadValida(actividad)) {
                                        respuesta.alFinalizar(
                                                EstadoCambioCorreo.ERROR
                                        );
                                    }
                                });
                    })
                    .addOnFailureListener(error -> {

                        if (actividadValida(actividad)) {
                            respuesta.alFinalizar(
                                    EstadoCambioCorreo.ERROR
                            );
                        }
                    });
        });
    }


    // MOSTRAR EL AVISO EXISTENTE CUANDO
    // FIREBASE SOLICITA INICIAR SESIÓN NUEVAMENTE
    public static void solicitarNuevoInicioSesion(Activity actividad) {

        if (actividadValida(actividad)) {
            mostrarDialogoSesion(actividad);
        }
    }


    // IDENTIFICAR ERRORES DE AUTENTICACIÓN
    private static boolean sesionInvalida(Exception error) {

        if (error instanceof FirebaseAuthInvalidUserException) {
            return true;
        }

        if (error instanceof FirebaseAuthException) {

            String codigo =
                    ((FirebaseAuthException) error).getErrorCode();

            return "ERROR_USER_TOKEN_EXPIRED".equals(codigo)
                    || "ERROR_INVALID_USER_TOKEN".equals(codigo);
        }

        return false;
    }

    private static boolean actividadValida(Activity actividad) {

        return !actividad.isFinishing()
                && !actividad.isDestroyed();
    }

    private static void mostrarMensaje(
            Activity actividad,
            String mensaje) {

        Toast.makeText(
                actividad,
                mensaje,
                Toast.LENGTH_LONG
        ).show();
    }

    // SOLO PARA ERRORES DE SESIÓN
    private static void mostrarDialogoSesion(Activity actividad) {

        // EVITAR MENSAJES DUPLICADOS
        if (!actividadValida(actividad)
                || avisoSesionEnPantalla) {
            return;
        }

        avisoSesionEnPantalla = true;

        // AVISAR A VIAJAPP QUE YA SE MOSTRÓ EL MENSAJE
        ViajAppApplication aplicacion =
                (ViajAppApplication) actividad.getApplication();

        aplicacion.marcarAvisoSesionMostrado();

        new AlertDialog.Builder(actividad)
                .setTitle("Necesitás iniciar sesión nuevamente")

                .setMessage(
                        "Por seguridad, necesitás iniciar sesión nuevamente.\n\n"
                                + "Si cambiaste tu correo electrónico y confirmaste "
                                + "el enlace recibido, ingresá con tu CORREO NUEVO "
                                + "y tu CONTRASEÑA HABITUAL.\n\n"
                                + "No necesitás cambiar tu contraseña. "
                                + "Utilizá la misma que usabas antes."
                )

                .setCancelable(false)
                .setPositiveButton(
                        "Ir a iniciar sesión",
                        (dialogo, which) -> {

                            FirebaseAuth.getInstance().signOut();

                            Intent intent = new Intent(
                                    actividad,
                                    InicioActivity.class
                            );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            actividad.startActivity(intent);
                            actividad.finish();
                        }
                )
                .setOnDismissListener(dialogo -> {
                    avisoSesionEnPantalla = false;
                })
                .show();
    }

}
