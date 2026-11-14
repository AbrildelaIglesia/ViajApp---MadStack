
package com.example.viajapp_madstack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
// ===================================================
// NUEVAS IMPORTACIONES PARA ACCESIBILIDAD
// ===================================================

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.IdentityHashMap;

import android.graphics.drawable.ColorDrawable;

public class AccesibilidadActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares;

    // ===================================================
    // NUEVO: VARIABLES DE ACCESIBILIDAD
    // ===================================================

    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;

    private View pantallaPrincipal;
    private View scrollContenido;
    private View header;
    private View bottomNav;

    // GUARDAR EL DISEÑO ORIGINAL
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private float escalaTexto = 1.0f;

    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accesibilidad);

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
                Intent intent = new Intent(AccesibilidadActivity.this, PlanearActivity.class);
                startActivity(intent);
            }
        });
        navAccesibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccesibilidadActivity.this, AccesibilidadActivity.class);
                startActivity(intent);
            }
        });
        navItinerario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccesibilidadActivity.this, ItinerarioActivity.class);
                startActivity(intent);
            }
        });
        navLugares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AccesibilidadActivity.this, LugaresActivity.class);
                startActivity(intent);
            }
        });
    }

    // ===================================================
    // NUEVO: CONFIGURAR ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        pantallaPrincipal = findViewById(R.id.main);
        scrollContenido = findViewById(R.id.scrollContenido);
        header = findViewById(R.id.header);
        bottomNav = findViewById(R.id.bottomNav);

        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        // GUARDAR TODOS LOS COLORES Y FONDOS ORIGINALES
        // ANTES DE MODIFICAR CUALQUIER ELEMENTO
        guardarOriginales(pantallaPrincipal);

        // ABRIR Y CERRAR EL MENÚ
        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility() != View.VISIBLE;

            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            // AJUSTAR BOTÓN PRINCIPAL
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

            // AJUSTAR ANCHO DE LA BARRA
            ConstraintLayout.LayoutParams parametrosBarra =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = mostrar
                    ? 0
                    : ConstraintLayout.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(parametrosBarra);

            // DESCRIPCIÓN PARA LECTORES DE PANTALLA
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

        // AUDIO SE PROGRAMARÁ MÁS ADELANTE
    }

    // ===================================================
    // NUEVO: GUARDAR EL DISEÑO ORIGINAL
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

                guardarOriginales(
                        grupo.getChildAt(i)
                );
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

            Float tamanoOriginal =
                    tamanosOriginales.get(texto);

            if (tamanoOriginal != null) {

                texto.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        tamanoOriginal * escalaTexto
                );
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarTamanoTextos(
                        grupo.getChildAt(i)
                );
            }
        }
    }

    // ===================================================
    // NUEVO: CONTRASTE NORMAL, CREMA Y OSCURO
    // ===================================================

    private void aplicarTemaContraste() {

        boolean normal = modoContraste == 0;
        boolean crema = modoContraste == 1;

        // MISMOS COLORES QUE REGISTRO E INICIO
        int fondoPantalla = Color.parseColor(
                crema ? "#FFF1D6" : "#111111"
        );

        int fondoBarra = Color.parseColor(
                crema ? "#F6E8CF" : "#181818"
        );

        int fondoControl = Color.parseColor(
                crema ? "#FFFAF0" : "#252525"
        );

        int fondoCampo = Color.parseColor(
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

        // 1. RESTAURAR TODOS LOS ELEMENTOS EN MODO NORMAL
        if (normal) {

            restaurarDisenoOriginal(pantallaPrincipal);
            return;
        }

        // 2. FONDO PRINCIPAL
        pantallaPrincipal.setBackground(new ColorDrawable(fondoPantalla));
        scrollContenido.setBackground(new ColorDrawable(fondoPantalla));

        // 3. ENCABEZADO
        header.setBackground(new ColorDrawable(fondoBarra));

        // 4. NAVEGACIÓN INFERIOR
        bottomNav.setBackground(new ColorDrawable(fondoBarra));

        // 5. OPCIÓN ACTIVA DE LA NAVEGACIÓN
        View opcionActual =
                findViewById(R.id.navAccesibilidad);

        opcionActual.setBackground(
                new ColorDrawable(
                        crema
                                ? Color.parseColor("#E9D7B8")
                                : Color.parseColor("#303030")
                )
        );

        // 6. BARRA DE ACCESIBILIDAD
        barraAccesibilidad.setBackground(
                fondoRedondeado(
                        fondoBarra,
                        borde,
                        16
                )
        );

        // 7. BOTONES DE ACCESIBILIDAD
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

        // 8. TEXTOS DEL ENCABEZADO
        actualizarColoresTextos(
                header,
                textoPrincipal,
                textoSecundario
        );

        // 9. TEXTOS DE LA NAVEGACIÓN
        actualizarColoresTextos(
                bottomNav,
                textoPrincipal,
                textoSecundario
        );

        // 10. TEXTOS DEL CONTENIDO
        actualizarColoresTextos(
                scrollContenido,
                textoPrincipal,
                textoSecundario
        );

        // 11. CONSERVAR EL LOGO ORIGINAL
        // NO SE APLICAN TINTES A SU IMAGEN DE FONDO
    }

    // ===================================================
    // NUEVO: CAMBIAR COLORES DE LOS TEXTOS
    // ===================================================

    private void actualizarColoresTextos(
            View vista,
            int principal,
            int secundario) {

        if (vista == null) {
            return;
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (original != null) {

                CharSequence descripcion =
                        texto.getContentDescription();

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
                                    || colorOriginal == Color.parseColor("#43474D")
                                    || colorOriginal == Color.parseColor("#4B5563")
                                    || colorOriginal == Color.parseColor("#6B7280");

                    texto.setTextColor(
                            esSecundario
                                    ? secundario
                                    : principal
                    );
                }
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarColoresTextos(
                        grupo.getChildAt(i),
                        principal,
                        secundario
                );
            }
        }
    }

    // ===================================================
    // NUEVO: RESTAURAR DISEÑO NORMAL
    // ===================================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) {
            return;
        }

        // RESTAURAR FONDO ORIGINAL
        if (fondosOriginales.containsKey(vista)) {

            vista.setBackground(
                    fondosOriginales.get(vista)
            );
        }

        // RESTAURAR COLOR ORIGINAL DEL TEXTO
        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal =
                    coloresOriginales.get(texto);

            if (colorOriginal != null) {

                texto.setTextColor(colorOriginal);
            }
        }

        // RESTAURAR ELEMENTOS INTERNOS
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

        forma.setCornerRadius(
                dp(radioDp)
        );

        forma.setStroke(
                dp(1),
                borde
        );

        return forma;
    }

    // CONVERTIR DP A PÍXELES
    private int dp(int valor) {

        return Math.round(
                valor * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
