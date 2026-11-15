
package com.example.viajapp_madstack;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

// IMPORTACIONES PARA DETECTAR EL REGRESO A VIAJAPP
import android.app.Activity;
import android.os.Bundle;

// IMPORTACIÓN PARA RECORDAR EL CORREO PENDIENTE
import android.content.SharedPreferences;

// IMPORTACIONES PARA LA COMPROBACIÓN GLOBAL
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ViajAppApplication extends Application {

    // DETECTAR CUANDO EL USUARIO REGRESA A VIAJAPP
    public interface EscuchaRegreso {
        void alRegresar(Activity actividad);
    }

    private EscuchaRegreso escuchaRegreso;

    private int actividadesIniciadas = 0;
    private boolean estabaEnSegundoPlano = false;
    private boolean regresoPendiente = false;

    public void establecerEscuchaRegreso(EscuchaRegreso escucha) {
        escuchaRegreso = escucha;
    }

    public void quitarEscuchaRegreso(EscuchaRegreso escucha) {
        if (escuchaRegreso == escucha) {
            escuchaRegreso = null;
        }
    }

    // =========================================
    // RECORDAR EL CAMBIO DE CORREO PENDIENTE
    // =========================================

    private static final String PREFERENCIAS_CORREO =
            "viajapp_cambio_correo";

    private static final String CLAVE_UID =
            "uid_pendiente";

    private static final String CLAVE_CORREO =
            "correo_pendiente";

    // GUARDAR EL CORREO QUE SE DEBE VERIFICAR
    public void guardarCorreoPendiente(
            String uid,
            String correoNuevo) {

        if (uid == null || uid.trim().isEmpty()
                || correoNuevo == null
                || correoNuevo.trim().isEmpty()) {
            return;
        }

        // PERMITIR COMPROBAR UN NUEVO CAMBIO DE CORREO
        avisoSesionMostrado = false;

        SharedPreferences preferencias =
                getSharedPreferences(
                        PREFERENCIAS_CORREO,
                        MODE_PRIVATE
                );

        preferencias.edit()
                .putString(CLAVE_UID, uid)
                .putString(CLAVE_CORREO, correoNuevo)
                .apply();
    }

    // OBTENER EL CORREO PENDIENTE
    // SOLO SI CORRESPONDE AL MISMO USUARIO
    public String obtenerCorreoPendiente(String uid) {

        if (uid == null) {
            return null;
        }

        SharedPreferences preferencias =
                getSharedPreferences(
                        PREFERENCIAS_CORREO,
                        MODE_PRIVATE
                );

        String uidGuardado =
                preferencias.getString(CLAVE_UID, null);

        if (!uid.equals(uidGuardado)) {
            return null;
        }

        return preferencias.getString(
                CLAVE_CORREO,
                null
        );
    }

    // BORRAR EL CAMBIO PENDIENTE CUANDO SE RESUELVE
    public void limpiarCorreoPendiente(String uid) {

        if (uid == null) {
            return;
        }

        SharedPreferences preferencias =
                getSharedPreferences(
                        PREFERENCIAS_CORREO,
                        MODE_PRIVATE
                );

        String uidGuardado =
                preferencias.getString(CLAVE_UID, null);

        if (!uid.equals(uidGuardado)) {
            return;
        }

        preferencias.edit()
                .remove(CLAVE_UID)
                .remove(CLAVE_CORREO)
                .apply();
    }

    // =========================================
    // COMPROBACIÓN GLOBAL DEL CORREO PENDIENTE
    // =========================================

    private boolean comprobandoCorreoGlobal = false;
    private boolean avisoSesionMostrado = false;

    // EVITAR QUE SE REPITA EL AVISO DE SESIÓN
    public void marcarAvisoSesionMostrado() {
        avisoSesionMostrado = true;
    }

    // CONSULTAR FIREBASE AL VOLVER A VIAJAPP
    private void comprobarCorreoPendienteGlobal(Activity actividad) {

        if (actividad.isFinishing()
                || actividad.isDestroyed()
                || comprobandoCorreoGlobal
                || avisoSesionMostrado) {
            return;
        }

        // NO MOSTRAR AVISOS SOBRE LA PANTALLA DE LOGIN
        if (actividad instanceof InicioActivity) {
            return;
        }

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        String uid = usuario.getUid();

        String correoPendiente =
                obtenerCorreoPendiente(uid);

        if (correoPendiente == null
                || correoPendiente.trim().isEmpty()) {
            return;
        }

        comprobandoCorreoGlobal = true;

        SincronizarCorreoUsuario.comprobarCambioCorreo(
                actividad,
                correoPendiente,
                estado -> {

                    comprobandoCorreoGlobal = false;

                    if (actividad.isFinishing()
                            || actividad.isDestroyed()) {
                        return;
                    }

                    switch (estado) {

                        case ACTUALIZADO:

                            limpiarCorreoPendiente(uid);

                            new AlertDialog.Builder(actividad)
                                    .setTitle(
                                            "¡Correo actualizado correctamente!"
                                    )
                                    .setMessage(
                                            "Tu nuevo correo electrónico "
                                                    + "fue confirmado y "
                                                    + "sincronizado correctamente.\n\n"
                                                    + "Ya podés consultar tus "
                                                    + "datos actualizados en Mi cuenta."
                                    )
                                    .setPositiveButton(
                                            "Entendido",
                                            null
                                    )
                                    .show();
                            break;

                        case SESION_INVALIDA:

                            if (!avisoSesionMostrado) {

                                avisoSesionMostrado = true;

                                SincronizarCorreoUsuario
                                        .solicitarNuevoInicioSesion(
                                                actividad
                                        );
                            }
                            break;

                        case PENDIENTE:

                            // TODAVÍA NO FUE CONFIRMADO.
                            // CONSERVAR EL CORREO PENDIENTE.
                            break;

                        case ERROR:

                            // NO BORRAR EL CORREO PENDIENTE.
                            // SE VOLVERÁ A COMPROBAR DESPUÉS.
                            break;
                    }
                }
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // App Check para pruebas locales
        if (BuildConfig.DEBUG) {
            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance();

            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        }

        // DETECTAR CUANDO VIAJAPP VUELVE
        // DESPUES DE ESTAR EN SEGUNDO PLANO
        registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(
                            Activity actividad,
                            Bundle estado) {
                    }

                    @Override
                    public void onActivityStarted(Activity actividad) {

                        if (actividadesIniciadas == 0
                                && estabaEnSegundoPlano) {

                            regresoPendiente = true;
                            estabaEnSegundoPlano = false;
                        }

                        actividadesIniciadas++;
                    }

                    @Override
                    public void onActivityResumed(Activity actividad) {

                        if (regresoPendiente) {
                            regresoPendiente = false;

                            if (escuchaRegreso != null) {
                                escuchaRegreso.alRegresar(actividad);
                            }
                        }

                        // COMPROBACIÓN GLOBAL NUEVA
                        // SOLO SI EDITAR PERFIL NO TIENE
                        // UNA ESCUCHA ACTIVA
                        if (escuchaRegreso == null) {
                            comprobarCorreoPendienteGlobal(
                                    actividad
                            );
                        }
                    }

                    @Override
                    public void onActivityPaused(Activity actividad) {
                    }

                    @Override
                    public void onActivityStopped(Activity actividad) {

                        actividadesIniciadas =
                                Math.max(0, actividadesIniciadas - 1);

                        if (actividadesIniciadas == 0
                                && !actividad.isChangingConfigurations()) {

                            estabaEnSegundoPlano = true;
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            Activity actividad,
                            Bundle estado) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity actividad) {
                    }
                }
        );
    }
}
