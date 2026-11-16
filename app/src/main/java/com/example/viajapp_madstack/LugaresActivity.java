
package com.example.viajapp_madstack;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.Button;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Comparator;
import java.text.DateFormat;
import java.util.Date;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;

import java.util.IdentityHashMap;

public class LugaresActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares;
    private LinearLayout contenedorItinerarios;
    private TextView tvEstadoColeccion;
    private static final String TAG = "VIAJAPP_COLECCION";

    // ===================================================
    // NUEVO: ACCESIBILIDAD (SIN CAMBIAR EL XML)
    // ===================================================

    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;
    private View pantallaPrincipal, scrollContenido, header, bottomNav;

    // GUARDAR DISEÑO ORIGINAL

    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<View, ColorStateList> tintesFondoOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<ImageView, ColorStateList> tintesIconosOriginales =
            new IdentityHashMap<>();

    private float escalaTexto = 1.0f;

    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lugares_turisticos);

        contenedorItinerarios = findViewById(R.id.contenedorItinerarios);
        tvEstadoColeccion = findViewById(R.id.tvEstadoColeccion);

        // NUEVO: INICIAR ACCESIBILIDAD
        configurarAccesibilidad();


        // ===================================================
        // MENÚ DESPLEGABLE DEL PERFIL DE USUARIO
        // ===================================================

        View btnAbrirPerfil = findViewById(R.id.btnAbrirPerfil);

        btnAbrirPerfil.setOnClickListener(v -> {

            MenuPerfilUsuario.mostrar(
                    LugaresActivity.this,
                    btnAbrirPerfil,
                    modoContraste,
                    escalaTexto
            );

        });


        // ===================================================
        // CÓDIGO ORIGINAL DE NAVEGACIÓN - SIN MODIFICACIONES
        // ===================================================

        // BOTONES DE NAVEGACIÓN
        navPlanear = findViewById(R.id.navPlanear);
        navAccesibilidad = findViewById(R.id.navAccesibilidad);
        navItinerario = findViewById(R.id.navItinerario);
        navLugares = findViewById(R.id.navLugares);

        navPlanear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LugaresActivity.this, PlanearActivity.class);
                startActivity(intent);
            }
        });

        navAccesibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LugaresActivity.this, AccesibilidadActivity.class);
                startActivity(intent);
            }
        });

        navItinerario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LugaresActivity.this, ItinerarioActivity.class);
                startActivity(intent);
            }
        });

        navLugares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarItinerarios();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        modoContraste =
                PreferenciasAccesibilidad.obtenerContraste(this);

        escalaTexto =
                PreferenciasAccesibilidad.obtenerEscalaTexto(this);

        aplicarTemaContraste();
        actualizarTamanoTextos(pantallaPrincipal);

        // CONSERVAR LA CARGA ORIGINAL DESDE FIREBASE
        cargarItinerarios();
    }

    private void cargarItinerarios() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        contenedorItinerarios.removeAllViews();
        if (usuario == null) {
            tvEstadoColeccion.setText("Iniciá sesión para ver tus itinerarios guardados.");
            return;
        }
        tvEstadoColeccion.setText("Cargando itinerarios...");
        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .collection("itinerarios")
                .get()
                .addOnSuccessListener(resultado -> {
                    if (isFinishing() || isDestroyed()) return;
                    FirebaseUser actual = FirebaseAuth.getInstance().getCurrentUser();
                    if (actual == null || !usuario.getUid().equals(actual.getUid())) return;
                    contenedorItinerarios.removeAllViews();
                    if (resultado.isEmpty()) {
                        tvEstadoColeccion.setText("Todavía no tenés itinerarios guardados. Creá uno desde Planear.");
                        return;
                    }
                    ArrayList<DocumentSnapshot> documentos = new ArrayList<>(resultado.getDocuments());
                    // Ordenar en el dispositivo para incluir documentos antiguos sin fechaCreacion.
                    documentos.sort((a, b) -> {
                        Date fechaA = a.getDate("fechaCreacion");
                        Date fechaB = b.getDate("fechaCreacion");
                        if (fechaA == null && fechaB == null) return 0;
                        if (fechaA == null) return 1;
                        if (fechaB == null) return -1;
                        return fechaB.compareTo(fechaA);
                    });
                    tvEstadoColeccion.setText(documentos.size() == 1
                            ? "1 itinerario guardado" : documentos.size() + " itinerarios guardados");
                    for (DocumentSnapshot documento : documentos) agregarTarjeta(documento);
                    guardarOriginales(contenedorItinerarios);
                    actualizarTamanoTextos(contenedorItinerarios);
                    if (modoContraste != 0) aplicarTemaContraste();
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    Log.e(TAG, "Error al cargar itinerarios", error);
                    tvEstadoColeccion.setText("No se pudieron cargar los itinerarios: "
                            + (error.getMessage() == null ? "error desconocido" : error.getMessage()));
                });
    }

    private void agregarTarjeta(DocumentSnapshot documento) {
        String destino = documento.getString("destino");
        if (destino == null || destino.trim().isEmpty()) destino = "Viaje sin nombre";
        Long dias = documento.getLong("cantidadDias");
        Date fecha = documento.getDate("fechaCreacion");

        LinearLayout tarjeta = new LinearLayout(this);
        tarjeta.setOrientation(LinearLayout.VERTICAL);
        tarjeta.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(Color.parseColor("#EFF6FF"));
        fondo.setCornerRadius(dp(16));
        fondo.setStroke(dp(1), Color.parseColor("#BFDBFE"));
        tarjeta.setBackground(fondo);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(14);
        tarjeta.setLayoutParams(params);

        TextView titulo = new TextView(this);
        titulo.setText(destino);
        titulo.setTextColor(Color.parseColor("#1E3A8A"));
        titulo.setTextSize(19);
        titulo.setTypeface(null, Typeface.BOLD);
        tarjeta.addView(titulo);

        TextView detalle = new TextView(this);
        String resumen = dias == null ? "Duración no informada" : dias + " días";
        if (fecha != null) resumen += " · " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(fecha);
        detalle.setText(resumen);
        detalle.setTextColor(Color.parseColor("#475569"));
        detalle.setTextSize(13);
        detalle.setPadding(0, dp(8), 0, dp(12));
        tarjeta.addView(detalle);

        Button abrir = new Button(this);
        abrir.setText("Ver itinerario y actividades");
        abrir.setAllCaps(false);
        abrir.setTextColor(Color.WHITE);
        abrir.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        tarjeta.addView(abrir, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        abrir.setOnClickListener(v -> {
            Intent intent = new Intent(LugaresActivity.this, ItinerarioActivity.class);
            intent.putExtra("itinerarioId", documento.getId());
            startActivity(intent);
        });
        Button eliminar = new Button(this);
        eliminar.setText("Eliminar itinerario");
        eliminar.setAllCaps(false);
        eliminar.setTextColor(Color.parseColor("#B91C1C"));
        eliminar.setContentDescription("Eliminar itinerario de " + destino);
        eliminar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        LinearLayout.LayoutParams parametrosEliminar = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        parametrosEliminar.topMargin = dp(8);
        tarjeta.addView(eliminar, parametrosEliminar);
        final String nombreDestino = destino;
        eliminar.setOnClickListener(v -> confirmarEliminacion(documento.getId(), nombreDestino, eliminar));

        contenedorItinerarios.addView(tarjeta);
    }

    private void confirmarEliminacion(String itinerarioId, String destino, Button botonEliminar) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar itinerario")
                .setMessage("¿Querés eliminar definitivamente el itinerario de " + destino
                        + "? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", (dialogo, cual) -> dialogo.dismiss())
                .setPositiveButton("Eliminar", (dialogo, cual) -> {
                    FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
                    if (usuario == null) {
                        Toast.makeText(this, "Iniciá sesión para eliminar itinerarios.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    botonEliminar.setEnabled(false);
                    FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(usuario.getUid())
                            .collection("itinerarios")
                            .document(itinerarioId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                if (isFinishing() || isDestroyed()) return;
                                Toast.makeText(this, "Itinerario eliminado", Toast.LENGTH_SHORT).show();
                                cargarItinerarios();
                            })
                            .addOnFailureListener(error -> {
                                if (isFinishing() || isDestroyed()) return;
                                botonEliminar.setEnabled(true);
                                Log.e(TAG, "No se pudo eliminar el itinerario", error);
                                Toast.makeText(this, "No se pudo eliminar el itinerario. Intentá nuevamente.",
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .show();
    }

    // ===================================================
    // NUEVO: CONFIGURACIÓN DE LA BARRA DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        ViewGroup contenidoActivity = findViewById(android.R.id.content);
        pantallaPrincipal = contenidoActivity.getChildAt(0);

        scrollContenido = findViewById(R.id.scrollContenido);
        header = findViewById(R.id.header);
        bottomNav = findViewById(R.id.bottomNav);

        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        // GUARDAR TODOS LOS ESTILOS ORIGINALES
        guardarOriginales(pantallaPrincipal);

        // ABRIR Y CERRAR MENÚ DE ACCESIBILIDAD

        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility() != View.VISIBLE;

            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            LinearLayout.LayoutParams parametrosBoton =
                    (LinearLayout.LayoutParams)
                            btnAccesibilidad.getLayoutParams();

            parametrosBoton.width = mostrar
                    ? 0
                    : LinearLayout.LayoutParams.WRAP_CONTENT;

            parametrosBoton.weight = mostrar ? 1f : 0f;

            btnAccesibilidad.setLayoutParams(parametrosBoton);

            int espacio = dp(12);

            btnAccesibilidad.setPadding(
                    mostrar ? 0 : espacio,
                    0,
                    mostrar ? 0 : espacio,
                    0
            );

            ConstraintLayout.LayoutParams parametrosBarra =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = mostrar
                    ? 0
                    : ConstraintLayout.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(parametrosBarra);

            btnAccesibilidad.setContentDescription(
                    mostrar
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // BOTÓN A+

        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(pantallaPrincipal);
            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // BOTÓN A-

        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(pantallaPrincipal);
            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // BOTÓN CONTRASTE

        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();
            PreferenciasAccesibilidad.guardarContraste(this, modoContraste);

            String nombreModo = modoContraste == 0
                    ? "Normal"
                    : modoContraste == 1
                      ? "Crema"
                      : "Oscuro";

            Toast.makeText(
                    this,
                    "Contraste: " + nombreModo,
                    Toast.LENGTH_SHORT
            ).show();
        });

        // AUDIO SE IMPLEMENTARÁ DESPUÉS
    }

    // ===================================================
    // NUEVO: GUARDAR DISEÑO ORIGINAL ANTES DE CAMBIARLO
    // ===================================================

    private void guardarOriginales(View vista) {

        if (vista == null) {
            return;
        }

        if (!fondosOriginales.containsKey(vista)) {

            fondosOriginales.put(
                    vista,
                    vista.getBackground()
            );

            tintesFondoOriginales.put(
                    vista,
                    vista.getBackgroundTintList()
            );
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            if (!coloresOriginales.containsKey(texto)) {

                coloresOriginales.put(
                        texto,
                        texto.getTextColors()
                );

                tamanosOriginales.put(
                        texto,
                        texto.getTextSize()
                );
            }
        }

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;

            if (!tintesIconosOriginales.containsKey(imagen)) {

                tintesIconosOriginales.put(
                        imagen,
                        ImageViewCompat.getImageTintList(imagen)
                );
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    // ===================================================
    // NUEVO: AUMENTAR Y DISMINUIR TEXTOS
    // ===================================================

    private void actualizarTamanoTextos(View vista) {

        if (vista == null) {
            return;
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            Float original = tamanosOriginales.get(texto);

            if (original != null) {

                texto.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        original * escalaTexto
                );
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarTamanoTextos(grupo.getChildAt(i));
            }
        }
    }

    // ===================================================
    // NUEVO: CONTRASTE NORMAL, CREMA Y OSCURO
    // ===================================================

    private void aplicarTemaContraste() {

        // VOLVER AL DISEÑO ORIGINAL

        if (modoContraste == 0) {

            restaurarDisenoOriginal(pantallaPrincipal);
            return;
        }

        boolean crema = modoContraste == 1;

        int fondoPantalla = Color.parseColor(
                crema ? "#FFF1D6" : "#111111"
        );

        int fondoBarra = Color.parseColor(
                crema ? "#F6E8CF" : "#181818"
        );

        int fondoControl = Color.parseColor(
                crema ? "#FFFAF0" : "#252525"
        );

        int fondoTarjeta = Color.parseColor(
                crema ? "#EBD6B4" : "#222222"
        );

        int bordeTarjeta = Color.parseColor(
                crema ? "#C7AC88" : "#444444"
        );

        int bordeControl = Color.parseColor(
                crema ? "#B99768" : "#888888"
        );

        int textoPrincipal = Color.parseColor(
                crema ? "#222222" : "#FFFFFF"
        );

        int textoSecundario = Color.parseColor(
                crema ? "#5A5146" : "#D0D0D0"
        );

        // 1. ADAPTAR LOS ELEMENTOS DE LA PANTALLA

        actualizarElementosTema(
                pantallaPrincipal,
                fondoTarjeta,
                bordeTarjeta,
                textoPrincipal,
                textoSecundario
        );

        // 2. FONDO PRINCIPAL

        pantallaPrincipal.setBackground(
                new ColorDrawable(fondoPantalla)
        );

        scrollContenido.setBackground(
                new ColorDrawable(fondoPantalla)
        );

        // 3. ENCABEZADO Y NAVEGACIÓN INFERIOR

        header.setBackground(
                new ColorDrawable(fondoBarra)
        );

        bottomNav.setBackground(
                new ColorDrawable(fondoBarra)
        );

        // 4. MARCAR LUGARES COMO PANTALLA ACTIVA

        navLugares.setBackground(
                new ColorDrawable(
                        Color.parseColor(
                                crema ? "#E9D7B8" : "#303030"
                        )
                )
        );

        // 5. BARRA DE ACCESIBILIDAD

        barraAccesibilidad.setBackground(
                fondoRedondeado(
                        fondoBarra,
                        bordeControl,
                        16
                )
        );

        // 6. BOTONES DEL MENÚ DE ACCESIBILIDAD

        TextView[] controles = {
                btnAccesibilidad,
                btnDisminuirTexto,
                btnAumentarTexto,
                btnContraste,
                btnAudio
        };

        for (TextView control : controles) {

            control.setBackground(
                    fondoRedondeado(
                            fondoControl,
                            bordeControl,
                            12
                    )
            );

            control.setTextColor(textoPrincipal);
        }
    }

    // ===================================================
    // NUEVO: ADAPTAR RECUADROS, TEXTOS E ÍCONOS
    // ===================================================

    private void actualizarElementosTema(
            View vista,
            int fondoTarjeta,
            int bordeTarjeta,
            int principal,
            int secundario) {

        if (vista == null) {
            return;
        }

        Drawable fondoOriginal = fondosOriginales.get(vista);

        CharSequence descripcion =
                vista.getContentDescription();

        // NO MODIFICAR EL LOGO NI EL ÍCONO DE PERFIL

        boolean esLogoOPerfil =
                descripcion != null
                        && (
                        "Logo ViajAPP".contentEquals(descripcion)
                                || "Perfil".contentEquals(descripcion)
                );

        // CAMBIAR FONDOS DE RECUADROS
        // SIN MODIFICAR LAS FOTOGRAFÍAS

        if (!esLogoOPerfil
                && fondoOriginal != null
                && (vista instanceof LinearLayout
                || vista instanceof TextView)) {

            vista.setBackgroundTintList(null);

            vista.setBackground(
                    fondoRedondeado(
                            fondoTarjeta,
                            bordeTarjeta,
                            12
                    )
            );
        }

        // ADAPTAR COLORES DE LOS TEXTOS

        if (vista instanceof TextView && !esLogoOPerfil) {

            TextView texto = (TextView) vista;

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (original != null) {

                int colorOriginal =
                        original.getDefaultColor();

                boolean esSecundario =
                        colorOriginal == Color.parseColor("#64748B")
                                || colorOriginal == Color.parseColor("#475569")
                                || colorOriginal == Color.parseColor("#43474D")
                                || colorOriginal == Color.parseColor("#94A3B8")
                                || colorOriginal == Color.parseColor("#334155");

                texto.setTextColor(
                        esSecundario ? secundario : principal
                );
            }
        }

        // ADAPTAR ÍCONOS VECTORIALES CON TINTE
        // LAS FOTOS EN IMAGEVIEW NO SE MODIFICAN

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;

            ColorStateList tinteOriginal =
                    tintesIconosOriginales.get(imagen);

            if (tinteOriginal != null) {

                ImageViewCompat.setImageTintList(
                        imagen,
                        ColorStateList.valueOf(principal)
                );
            }
        }

        // RECORRER TODOS LOS ELEMENTOS DE LA PANTALLA

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarElementosTema(
                        grupo.getChildAt(i),
                        fondoTarjeta,
                        bordeTarjeta,
                        principal,
                        secundario
                );
            }
        }
    }

    // ===================================================
    // NUEVO: RESTAURAR EL DISEÑO ORIGINAL DEL XML
    // ===================================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) {
            return;
        }

        // RESTAURAR FONDOS

        if (fondosOriginales.containsKey(vista)) {

            vista.setBackgroundTintList(null);

            vista.setBackground(
                    fondosOriginales.get(vista)
            );

            vista.setBackgroundTintList(
                    tintesFondoOriginales.get(vista)
            );
        }

        // RESTAURAR COLORES DE TEXTO

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal =
                    coloresOriginales.get(texto);

            if (colorOriginal != null) {

                texto.setTextColor(colorOriginal);
            }
        }

        // RESTAURAR ÍCONOS ORIGINALES

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;

            ImageViewCompat.setImageTintList(
                    imagen,
                    tintesIconosOriginales.get(imagen)
            );
        }

        // RESTAURAR TODOS LOS ELEMENTOS HIJOS

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                restaurarDisenoOriginal(
                        grupo.getChildAt(i)
                );
            }
        }
    }

    // ===================================================
    // NUEVO: CREAR FONDOS REDONDEADOS
    // ===================================================

    private Drawable fondoRedondeado(
            int color,
            int borde,
            int radioDp) {

        GradientDrawable forma = new GradientDrawable();

        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(color);

        forma.setCornerRadius(dp(radioDp));
        forma.setStroke(dp(1), borde);

        return forma;
    }

    // ===================================================
    // NUEVO: CONVERTIR DP A PÍXELES
    // ===================================================

    private int dp(int valor) {

        return Math.round(
                valor * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}

