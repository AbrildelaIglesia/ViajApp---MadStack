
package com.example.viajapp_madstack;

import android.content.Intent;
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

        // NUEVO: INICIAR ACCESIBILIDAD
        configurarAccesibilidad();

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
                Intent intent = new Intent(LugaresActivity.this, LugaresActivity.class);
                startActivity(intent);
            }
        });
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
        });

        // BOTÓN A-

        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(pantallaPrincipal);
        });

        // BOTÓN CONTRASTE

        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();

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
