package com.example.viajapp_madstack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

// ===================================================
// NUEVAS IMPORTACIONES PARA ACCESIBILIDAD
// ===================================================

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.IdentityHashMap;

public class ItinerarioActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares;

    // ===================================================
    // NUEVO: VARIABLES DE ACCESIBILIDAD
    // ===================================================

    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;
    private View pantallaPrincipal, scrollContenido, header, bottomNav;

    // GUARDAR DISEÑO ORIGINAL
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<View, ColorStateList> tintesFondosOriginales =
            new IdentityHashMap<>();

    private float escalaTexto = 1.0f;

    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerario);

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
                Intent intent = new Intent(ItinerarioActivity.this, PlanearActivity.class);
                startActivity(intent);
            }
        });
        navAccesibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItinerarioActivity.this, AccesibilidadActivity.class);
                startActivity(intent);
            }
        });
        navItinerario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItinerarioActivity.this, ItinerarioActivity.class);
                startActivity(intent);
            }
        });
        navLugares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItinerarioActivity.this, LugaresActivity.class);
                startActivity(intent);
            }
        });
    }

    // ===================================================
    // NUEVO: CONFIGURAR MENÚ DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        // EL XML NO TIENE ID EN EL CONTENEDOR PRINCIPAL
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

        // GUARDAR TODOS LOS ESTILOS ANTES DE CAMBIARLOS
        guardarOriginales(pantallaPrincipal);

        // ABRIR Y CERRAR MENÚ
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

            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(header);
            actualizarTamanoTextos(bottomNav);
        });

        // BOTÓN A-
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(header);
            actualizarTamanoTextos(bottomNav);
        });

        // BOTÓN CONTRASTE
        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();

            String nombreModo;

            if (modoContraste == 0) {
                nombreModo = "Normal";
            } else if (modoContraste == 1) {
                nombreModo = "Crema";
            } else {
                nombreModo = "Oscuro";
            }

            Toast.makeText(
                    this,
                    "Contraste: " + nombreModo,
                    Toast.LENGTH_SHORT
            ).show();
        });

        // AUDIO SE PROGRAMARÁ DESPUÉS
    }

    // ===================================================
    // NUEVO: GUARDAR DISEÑO ORIGINAL
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

            tintesFondosOriginales.put(
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

        boolean normal = modoContraste == 0;
        boolean crema = modoContraste == 1;


        // PRIMERO: RESTAURAR TODO AL VOLVER A NORMAL
        if (normal) {

            restaurarDisenoOriginal(pantallaPrincipal);

            // RESTAURAR BOTÓN ESCUCHAR ITINERARIO
            Button botonAudioItinerario =
                    findViewById(R.id.btnAudioItinerario);

            botonAudioItinerario.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#2563EB")
                    )
            );

            botonAudioItinerario.setTextColor(Color.WHITE);


            // RESTAURAR BOTÓN ESCUCHAR GUÍA DE AUDIO
            Button botonAudioMirador =
                    findViewById(R.id.btnAudioMirador);

            botonAudioMirador.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#EFF6FF")
                    )
            );

            botonAudioMirador.setTextColor(
                    Color.parseColor("#1D4ED8")
            );

            // RESTAURAR BOTÓN VER MAPA
            Button botonVerMapa =
                    findViewById(R.id.btnVerMapa);

            botonVerMapa.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#F43F5E")
                    )
            );

            botonVerMapa.setTextColor(Color.WHITE);

            // QUITAR BORDES AL VOLVER AL MODO NORMAL
            botonAudioItinerario.setForeground(null);
            botonAudioMirador.setForeground(null);
            botonVerMapa.setForeground(null);


            return;
        }


        // COLORES DE REGISTRO, INICIO Y ACCESIBILIDAD
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
                crema ? "#F3DFC0" : "#222222"
        );

        int borde = Color.parseColor(
                crema ? "#A6947F" : "#666666"
        );

        int textoPrincipal = Color.parseColor(
                crema ? "#222222" : "#FFFFFF"
        );

        int textoSecundario = Color.parseColor(
                crema ? "#5A5146" : "#D0D0D0"
        );

        // 1. FONDOS Y COLORES DE TODA LA PANTALLA
        actualizarElementosTema(
                pantallaPrincipal,
                fondoTarjeta,
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

        // 4. OPCIÓN ACTIVA: ITINERARIO
        View opcionActual = findViewById(R.id.navItinerario);

        opcionActual.setBackground(
                new ColorDrawable(
                        crema
                                ? Color.parseColor("#E9D7B8")
                                : Color.parseColor("#303030")
                )
        );

        // 5. BARRA DE ACCESIBILIDAD
        barraAccesibilidad.setBackground(
                fondoRedondeado(fondoBarra, borde, 16)
        );

        // 6. BOTONES DEL MENÚ
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
                            borde,
                            12
                    )
            );

            control.setTextColor(textoPrincipal);
        }

        // 7. BOTONES DEL ITINERARIO
        configurarBotonTema(
                findViewById(R.id.btnAudioItinerario),
                crema
                        ? Color.parseColor("#E9D7B8")
                        : Color.parseColor("#303030"),
                textoPrincipal
        );

        configurarBotonTema(
                findViewById(R.id.btnAudioMirador),
                fondoTarjeta,
                textoPrincipal
        );

        configurarBotonTema(
                findViewById(R.id.btnVerMapa),
                crema
                        ? Color.parseColor("#E9D7B8")
                        : Color.parseColor("#303030"),
                textoPrincipal
        );
    }

    // ===================================================
    // NUEVO: ADAPTAR LOS ELEMENTOS DE LA PANTALLA
    // ===================================================

    private void actualizarElementosTema(
            View vista,
            int fondoTarjeta,
            int principal,
            int secundario) {

        if (vista == null) {
            return;
        }

        // CAMBIAR SOLO FONDOS DE COLOR SÓLIDO
        // NO CAMBIAR FONDOS QUE CONTENGAN IMÁGENES
        Drawable fondoOriginal = fondosOriginales.get(vista);

        if (fondoOriginal instanceof ColorDrawable) {

            int colorOriginal =
                    ((ColorDrawable) fondoOriginal).getColor();

            int nuevoFondo = fondoTarjeta;

            // DISTINGUIR LA ETIQUETA DEL DÍA SELECCIONADO
            if (colorOriginal == Color.parseColor("#F43F5E")) {

                nuevoFondo = modoContraste == 1
                        ? Color.parseColor("#D8C5A8")
                        : Color.parseColor("#404040");
            }



            // ADAPTAR RECUADROS AL MODO CREMA Y OSCURO
            // SIN PONER BORDES A TODOS LOS TEXTOS

            if (vista instanceof LinearLayout) {

                int colorFondoTarjeta = Color.parseColor(
                        modoContraste == 1 ? "#EBD6B4" : "#222222"
                );

                int colorBorde = Color.parseColor(
                        modoContraste == 1 ? "#C7AC88" : "#444444"
                );

                GradientDrawable forma = new GradientDrawable();
                forma.setShape(GradientDrawable.RECTANGLE);
                forma.setColor(colorFondoTarjeta);
                forma.setCornerRadius(dp(10));
                forma.setStroke(dp(1), colorBorde);

                vista.setBackground(forma);

            } else if (vista instanceof TextView) {

                // LAS ETIQUETAS Y LOS DÍAS SE ADAPTAN AL TEMA
                // SIN AGREGAR BORDES INNECESARIOS

                vista.setBackground(
                        new ColorDrawable(nuevoFondo)
                );

            } else {

                vista.setBackground(
                        new ColorDrawable(nuevoFondo)
                );
            }

        }

        // CAMBIAR COLORES DE TEXTO
        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (original != null) {

                CharSequence descripcion =
                        texto.getContentDescription();

                // NO ALTERAR EL LOGO NI EL ICONO DE PERFIL
                boolean esLogo =
                        descripcion != null
                                && descripcion.toString()
                                .equals("Logo ViajAPP");

                boolean esPerfil =
                        descripcion != null
                                && descripcion.toString()
                                .equals("Perfil");

                if (!esLogo && !esPerfil) {

                    int colorOriginal =
                            original.getDefaultColor();

                    boolean esSecundario =
                            colorOriginal == Color.parseColor("#64748B")
                                    || colorOriginal == Color.parseColor("#475569")
                                    || colorOriginal == Color.parseColor("#43474D")
                                    || colorOriginal == Color.parseColor("#4B5563");

                    texto.setTextColor(
                            esSecundario ? secundario : principal
                    );
                }
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarElementosTema(
                        grupo.getChildAt(i),
                        fondoTarjeta,
                        principal,
                        secundario
                );
            }
        }
    }

    // ===================================================
    // NUEVO: ADAPTAR BOTONES CON BACKGROUNDTINT
    // ===================================================



    private void configurarBotonTema(
            Button boton,
            int fondo,
            int texto) {

        if (boton == null) {
            return;
        }

        // BORDE BEIGE EN CREMA Y GRIS EN OSCURO
        int colorBorde = Color.parseColor(
                modoContraste == 1 ? "#B99768" : "#888888"
        );

        // CAMBIAR EL COLOR DEL BOTÓN SEGÚN EL CONTRASTE
        ColorStateList colorFondo =
                ColorStateList.valueOf(fondo);

        boton.setBackgroundTintList(colorFondo);

        // CREAR SOLO EL BORDE, SIN PINTAR TODO EL BOTÓN
        GradientDrawable bordeDrawable = new GradientDrawable();
        bordeDrawable.setShape(GradientDrawable.RECTANGLE);
        bordeDrawable.setColor(Color.TRANSPARENT);
        bordeDrawable.setCornerRadius(dp(32));
        bordeDrawable.setStroke(dp(2), colorBorde);

        boton.setForeground(bordeDrawable);

        // TEXTO OSCURO EN CREMA Y BLANCO EN OSCURO
        boton.setTextColor(texto);
    }




    // ===================================================
    // NUEVO: RESTAURAR DISEÑO ORIGINAL
    // ===================================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) {
            return;
        }

        // RESTAURAR FONDO
        if (fondosOriginales.containsKey(vista)) {

            // LIMPIAR TINTE ANTES DE RECUPERAR EL FONDO
            vista.setBackgroundTintList(null);

            vista.setBackground(
                    fondosOriginales.get(vista)
            );

            // RESTAURAR EL TINTE ORIGINAL
            vista.setBackgroundTintList(
                    tintesFondosOriginales.get(vista)
            );
        }

        // RESTAURAR TEXTO ORIGINAL
        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal =
                    coloresOriginales.get(texto);

            if (colorOriginal != null) {

                texto.setTextColor(colorOriginal);
            }
        }

        // RESTAURAR TODOS LOS HIJOS
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

    private int dp(int valor) {

        return Math.round(
                valor * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
