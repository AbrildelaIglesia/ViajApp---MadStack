
package com.example.viajapp_madstack;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuPerfilUsuario {

    // MODOS DE ACCESIBILIDAD
    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO

    public static void mostrar(
            Activity actividad,
            View botonPerfil,
            int modoContraste,
            float escalaTexto) {

        // CARGAR DISEÑO DEL MENÚ
        View menu = LayoutInflater.from(actividad)
                .inflate(R.layout.menu_perfil_usuario, null);

        // CREAR VENTANA DESPLEGABLE
        PopupWindow popup = new PopupWindow(
                menu,
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
        popup.setClippingEnabled(true);
        popup.setAnimationStyle(android.R.style.Animation_Dialog);

        // ELEMENTOS DEL MENÚ
        TextView txtNombre =
                menu.findViewById(R.id.txtNombrePerfil);

        TextView txtCorreo =
                menu.findViewById(R.id.txtCorreoPerfil);

        MaterialCardView btnEditar =
                menu.findViewById(R.id.btnMenuEditarPerfil);

        MaterialCardView btnSeguridad =
                menu.findViewById(R.id.btnMenuSeguridad);

        MaterialCardView btnCerrar =
                menu.findViewById(R.id.btnMenuCerrarSesion);

        // APLICAR ACCESIBILIDAD
        aplicarContraste(menu, modoContraste);
        aplicarTamanoTexto(menu, escalaTexto);

        // CARGAR DATOS REALES DE FIREBASE
        cargarDatosUsuario(
                actividad,
                txtNombre,
                txtCorreo
        );

        // EDITAR PERFIL
        btnEditar.setOnClickListener(v -> {

            // CERRAR MENÚ PRINCIPAL
            popup.dismiss();

            // ABRIR FORMULARIO EDITAR PERFIL
            MenuEditarPerfil.mostrar(
                    actividad,
                    botonPerfil,
                    modoContraste,
                    escalaTexto
            );
        });

        // SEGURIDAD
        btnSeguridad.setOnClickListener(v -> {

            // CERRAR MENÚ PRINCIPAL
            popup.dismiss();

            MenuSeguridad.mostrar(
                    actividad,
                    botonPerfil,
                    modoContraste,
                    escalaTexto
            );
        });

        // CERRAR SESIÓN
        btnCerrar.setOnClickListener(v -> {

            popup.dismiss();

            new androidx.appcompat.app.AlertDialog.Builder(actividad)
                    .setTitle("Cerrar sesión")
                    .setMessage(
                            "¿Querés cerrar tu sesión de ViajAPP?"
                    )
                    .setNegativeButton(
                            "Cancelar",
                            (dialog, which) -> dialog.dismiss()
                    )
                    .setPositiveButton(
                            "Cerrar sesión",
                            (dialog, which) -> cerrarSesion(actividad)
                    )
                    .show();
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
            TextView txtNombre,
            TextView txtCorreo) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {

            txtNombre.setText("Sin sesión iniciada");
            txtCorreo.setText("");

            return;
        }

        // ACTUALIZAR CORREO DESDE FIREBASE
        SincronizarCorreoUsuario.actualizar(
                actividad,
                txtCorreo
        );

        // NOMBRE DESDE FIRESTORE
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .get()
                .addOnSuccessListener(documento -> {

                    if (actividad.isFinishing()
                            || actividad.isDestroyed()) {
                        return;
                    }

                    String nombreCompleto = "";

                    if (documento.exists()) {

                        // COMPATIBLE CON REGISTRO ACTUAL
                        String nombreApellidos =
                                documento.getString("nombreApellidos");

                        if (nombreApellidos != null) {
                            nombreCompleto = nombreApellidos;
                        }

                        // COMPATIBLE CON FUTURA EDICIÓN
                        String nombre =
                                documento.getString("nombre");

                        String apellido =
                                documento.getString("apellido");

                        if (nombre != null
                                && !nombre.trim().isEmpty()
                                && apellido != null
                                && !apellido.trim().isEmpty()) {

                            nombreCompleto =
                                    nombre.trim() + " " + apellido.trim();
                        }
                    }

                    if (!nombreCompleto.trim().isEmpty()) {

                        txtNombre.setText(nombreCompleto);

                    } else if (usuario.getDisplayName() != null
                            && !usuario.getDisplayName().isEmpty()) {

                        txtNombre.setText(usuario.getDisplayName());

                    } else {

                        txtNombre.setText("Mi usuario");
                    }
                })
                .addOnFailureListener(error -> {

                    if (actividad.isFinishing()
                            || actividad.isDestroyed()) {
                        return;
                    }

                    String nombre = usuario.getDisplayName();

                    txtNombre.setText(
                            nombre != null && !nombre.isEmpty()
                                    ? nombre : "Mi usuario"
                    );
                });
    }

    // =========================================
    // CERRAR SESIÓN
    // =========================================

    private static void cerrarSesion(Activity actividad) {

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(
                actividad,
                InicioActivity.class
        );

        // LIMPIAR HISTORIAL DE PANTALLAS
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        actividad.startActivity(intent);
        actividad.finish();
    }

    // =========================================
    // APLICAR CONTRASTE
    // =========================================

    private static void aplicarContraste(
            View menu,
            int modo) {

        boolean crema = modo == 1;
        boolean oscuro = modo == 2;

        int fondo = Color.parseColor(
                oscuro ? "#111111"
                        : crema ? "#FFF1D6"
                          : "#FFFFFF"
        );

        int fondoBotones = Color.parseColor(
                oscuro ? "#252525"
                        : crema ? "#F3DFC0"
                          : "#DBEAFE"
        );

        int bordeBotones = Color.parseColor(
                oscuro ? "#777777"
                        : crema ? "#A6947F"
                          : "#93C5FD"
        );

        int texto = Color.parseColor(
                oscuro ? "#FFFFFF" : "#111827"
        );

        int secundario = Color.parseColor(
                oscuro ? "#E2E8F0"
                        : crema ? "#49351F"
                          : "#475569"
        );


        int destacado = Color.parseColor(
                oscuro ? "#FFFFFF"
                        : crema ? "#664700"
                          : "#2563EB"
        );


    // =========================================
    // TARJETA PRINCIPAL DEL MENÚ DESPLEGABLE
    // =========================================

        MaterialCardView tarjeta =
                menu.findViewById(R.id.tarjetaMenuPerfil);

    // BORDE SEGÚN EL MODO DE CONTRASTE
        int bordeMenu = Color.parseColor(
                oscuro ? "#777777"
                        : crema ? "#96754D"
                          : "#94A3B8"
        );

    // FONDO DEL MENÚ
        tarjeta.setCardBackgroundColor(fondo);

    // CONTORNO MÁS VISIBLE
        tarjeta.setStrokeColor(bordeMenu);
        tarjeta.setStrokeWidth(dpDesdeVista(menu, 2));

    // ESQUINAS REDONDEADAS
        tarjeta.setRadius(dpDesdeVista(menu, 20));


        // AVATAR
        MaterialCardView avatar =
                menu.findViewById(R.id.tarjetaAvatarPerfil);

        avatar.setCardBackgroundColor(fondoBotones);
        avatar.setStrokeColor(bordeBotones);

        ImageView imgAvatar =
                menu.findViewById(R.id.imgAvatarPerfil);

        imgAvatar.setImageTintList(
                ColorStateList.valueOf(destacado)
        );

        // BOTONES AZULES
        MaterialCardView editar =
                menu.findViewById(R.id.btnMenuEditarPerfil);

        MaterialCardView seguridad =
                menu.findViewById(R.id.btnMenuSeguridad);

        editar.setCardBackgroundColor(fondoBotones);
        editar.setStrokeColor(bordeBotones);

        seguridad.setCardBackgroundColor(fondoBotones);
        seguridad.setStrokeColor(bordeBotones);

        // CERRAR SESIÓN
        MaterialCardView cerrar =
                menu.findViewById(R.id.btnMenuCerrarSesion);

        cerrar.setCardBackgroundColor(
                Color.parseColor(
                        oscuro ? "#351C1C"
                                : crema ? "#F1D3CD"
                                  : "#FEE2E2"
                )
        );

        cerrar.setStrokeColor(
                Color.parseColor(
                        oscuro ? "#E57373"
                                : crema ? "#B87474"
                                  : "#FCA5A5"
                )
        );

        // TEXTOS DE LA CUENTA
        ((TextView) menu.findViewById(R.id.txtMiCuenta))
                .setTextColor(destacado);

        ((TextView) menu.findViewById(R.id.txtNombrePerfil))
                .setTextColor(texto);

        ((TextView) menu.findViewById(R.id.txtCorreoPerfil))
                .setTextColor(secundario);

        // TEXTOS E ICONOS DE BOTONES
        int[] textos = {
                R.id.iconoEditarPerfil,
                R.id.txtEditarPerfil,
                R.id.flechaEditarPerfil,
                R.id.txtSeguridadPerfil,
                R.id.flechaSeguridadPerfil
        };

        for (int id : textos) {

            TextView vista = menu.findViewById(id);
            vista.setTextColor(texto);
        }

        ImageView iconoSeguridad =
                menu.findViewById(R.id.iconoSeguridadPerfil);

        iconoSeguridad.setImageTintList(
                ColorStateList.valueOf(texto)
        );

        int rojo = Color.parseColor(
                oscuro ? "#FFB4B4" : "#B91C1C"
        );


        ImageView iconoCerrarSesion =
                menu.findViewById(R.id.iconoCerrarSesion);

        iconoCerrarSesion.setImageTintList(
                ColorStateList.valueOf(rojo)
        );


        ((TextView) menu.findViewById(R.id.txtCerrarSesion))
                .setTextColor(rojo);

        // SEPARADORES
        int separador = Color.parseColor(
                oscuro ? "#555555"
                        : crema ? "#C9B8A3"
                          : "#E2E8F0"
        );

        menu.findViewById(R.id.divisorSuperiorPerfil)
                .setBackgroundColor(separador);

        menu.findViewById(R.id.divisorInferiorPerfil)
                .setBackgroundColor(separador);
    }

    // =========================================
    // AUMENTAR Y DISMINUIR TEXTO
    // =========================================

    private static void aplicarTamanoTexto(
            View vista,
            float escala) {

        float escalaSegura =
                Math.max(0.9f, Math.min(1.3f, escala));

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            float tamanoOriginal = texto.getTextSize();

            texto.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    tamanoOriginal * escalaSegura
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                aplicarTamanoTexto(
                        grupo.getChildAt(i),
                        escalaSegura
                );
            }
        }
    }

    // =========================================
    // CONVERTIR DP A PIXELES
    // =========================================


    private static int dpDesdeVista(View vista, int valor) {

        return Math.round(
                valor * vista.getResources()
                        .getDisplayMetrics().density
        );
    }

    // =========================================
    // CONVERTIR DP A PÍXELES DESDE UNA ACTIVITY
    // =========================================

    private static int dp(Activity actividad, int valor) {

        return Math.round(
                valor * actividad.getResources()
                        .getDisplayMetrics().density
        );
    }


}
