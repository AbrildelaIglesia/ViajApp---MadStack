
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.IdentityHashMap;

public class PlanearActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad,
            navItinerario, navLugares;

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

    private final IdentityHashMap<View, ColorStateList> tintesOriginales =
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
        setContentView(R.layout.activity_planear_viaje);

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
                Intent intent = new Intent(
                        PlanearActivity.this,
                        PlanearActivity.class
                );
                startActivity(intent);
            }
        });

        navAccesibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        PlanearActivity.this,
                        AccesibilidadActivity.class
                );
                startActivity(intent);
            }
        });

        navItinerario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        PlanearActivity.this,
                        ItinerarioActivity.class
                );
                startActivity(intent);
            }
        });

        navLugares.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        PlanearActivity.this,
                        LugaresActivity.class
                );
                startActivity(intent);
            }
        });
    }

    // ===================================================
    // NUEVO: CONFIGURAR MENÚ DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        pantallaPrincipal = findViewById(R.id.main);
        scrollContenido = findViewById(R.id.scrollContenido);
        header = findViewById(R.id.header);
        bottomNav = findViewById(R.id.bottomNav);

        barraAccesibilidad = findViewById(
                R.id.barraAccesibilidad
        );

        btnAccesibilidad = findViewById(
                R.id.btnAccesibilidad
        );

        btnDisminuirTexto = findViewById(
                R.id.btnDisminuirTexto
        );

        btnAumentarTexto = findViewById(
                R.id.btnAumentarTexto
        );

        btnContraste = findViewById(
                R.id.btnContraste
        );

        btnAudio = findViewById(
                R.id.btnAudio
        );

        // GUARDAR EL DISEÑO ANTES DE MODIFICARLO
        guardarOriginales(pantallaPrincipal);

        // ===================================================
        // ABRIR Y CERRAR EL MENÚ
        // ===================================================

        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility() != View.VISIBLE;

            int visibilidad = mostrar
                    ? View.VISIBLE
                    : View.GONE;

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

        // ===================================================
        // BOTÓN A+
        // ===================================================

        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(header);
            actualizarTamanoTextos(bottomNav);
        });

        // ===================================================
        // BOTÓN A-
        // ===================================================

        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(header);
            actualizarTamanoTextos(bottomNav);
        });

        // ===================================================
        // BOTÓN CONTRASTE
        // ===================================================

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

        // AUDIO SE IMPLEMENTARÁ DESPUÉS
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

            tintesOriginales.put(
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

        // RESTAURAR EL DISEÑO ORIGINAL

        if (normal) {

            restaurarDisenoOriginal(pantallaPrincipal);
            return;
        }

        // COLORES UTILIZADOS EN LAS OTRAS PANTALLAS

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

        // 3. ENCABEZADO Y MENÚ INFERIOR

        header.setBackground(
                new ColorDrawable(fondoBarra)
        );

        bottomNav.setBackground(
                new ColorDrawable(fondoBarra)
        );

        // 4. OPCIÓN ACTIVA: PLANEAR

        View opcionActual = findViewById(R.id.navPlanear);

        opcionActual.setBackground(
                new ColorDrawable(
                        crema
                                ? Color.parseColor("#E9D7B8")
                                : Color.parseColor("#303030")
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

        // 6. BOTONES DE ACCESIBILIDAD

        TextView[] controles = {
                btnAccesibilidad,
                btnDisminuirTexto,
                btnAumentarTexto,
                btnContraste,
                btnAudio
        };

        for (TextView control : controles) {

            control.setBackgroundTintList(null);

            control.setBackground(
                    fondoRedondeado(
                            fondoControl,
                            bordeControl,
                            12
                    )
            );

            control.setTextColor(textoPrincipal);
        }

        // 7. ADAPTAR LOS BOTONES + Y -

        configurarBotonTema(
                findViewById(R.id.btnMenosDias),
                fondoControl,
                textoPrincipal,
                bordeControl,
                12
        );

        configurarBotonTema(
                findViewById(R.id.btnMasDias),
                fondoControl,
                textoPrincipal,
                bordeControl,
                12
        );

        configurarBotonTema(
                findViewById(R.id.btnMenosViajeros),
                fondoControl,
                textoPrincipal,
                bordeControl,
                12
        );

        configurarBotonTema(
                findViewById(R.id.btnMasViajeros),
                fondoControl,
                textoPrincipal,
                bordeControl,
                12
        );

        // 8. BOTÓN CREAR VIAJE CON IA

        configurarBotonTema(
                findViewById(R.id.btnCrearViajeIA),
                crema
                        ? Color.parseColor("#E9D7B8")
                        : Color.parseColor("#303030"),
                textoPrincipal,
                bordeControl,
                28
        );
    }

    // ===================================================
    // NUEVO: ADAPTAR RECUADROS Y TEXTOS
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

        // NO MODIFICAR EL CONTENIDO SOBRE LA FOTO
        // DE BARILOCHE

        if (estaDentroDeFoto(vista)) {
            return;
        }

        Drawable fondoOriginal = fondosOriginales.get(vista);

        CharSequence descripcion =
                vista.getContentDescription();

        boolean esLogoOPerfil =
                descripcion != null
                        && (
                        "Logo ViajAPP".contentEquals(descripcion)
                                || "Perfil".contentEquals(descripcion)
                );

        // ADAPTAR RECUADROS DE INFORMACIÓN

        if (!esLogoOPerfil
                && fondoOriginal != null
                && vista instanceof LinearLayout) {

            vista.setBackgroundTintList(null);

            vista.setBackground(
                    fondoRedondeado(
                            fondoTarjeta,
                            bordeTarjeta,
                            12
                    )
            );
        }

        // ADAPTAR ELEMENTOS QUE TIENEN FONDOS
        // SIN CAMBIAR LOS CONTENEDORES DE FOTOGRAFÍAS

        if (!esLogoOPerfil
                && fondoOriginal != null
                && vista instanceof TextView
                && !(vista instanceof Button)
                && vista != btnAccesibilidad
                && vista != btnDisminuirTexto
                && vista != btnAumentarTexto
                && vista != btnContraste
                && vista != btnAudio) {

            vista.setBackgroundTintList(null);

            int radio = dp(12);

            if (vista.getLayoutParams() != null
                    && vista.getLayoutParams().width > 0
                    && vista.getLayoutParams().width <= dp(60)) {

                radio = dp(30);
            }

            GradientDrawable forma = new GradientDrawable();

            forma.setShape(GradientDrawable.RECTANGLE);
            forma.setColor(fondoTarjeta);
            forma.setCornerRadius(radio);
            forma.setStroke(dp(1), bordeTarjeta);

            vista.setBackground(forma);
        }

        // ADAPTAR COLORES DEL TEXTO

        if (vista instanceof TextView && !esLogoOPerfil) {

            TextView texto = (TextView) vista;

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (original != null) {

                int colorOriginal = original.getDefaultColor();

                boolean esSecundario =
                        colorOriginal == Color.parseColor("#64748B")
                                || colorOriginal == Color.parseColor("#475569")
                                || colorOriginal == Color.parseColor("#43474D")
                                || colorOriginal == Color.parseColor("#94A3B8");

                texto.setTextColor(
                        esSecundario ? secundario : principal
                );
            }
        }

        // RECORRER LOS ELEMENTOS HIJOS

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
    // NUEVO: PROTEGER LOS TEXTOS DE LA FOTOGRAFÍA
    // ===================================================

    private boolean estaDentroDeFoto(View vista) {

        View actual = vista;

        while (actual != null) {

            // EL FRAME DEL DESTINO CONTIENE LA FOTO,
            // LOS TEXTOS BLANCOS Y EL DEGRADADO

            if (actual instanceof FrameLayout) {

                ViewGroup grupo = (ViewGroup) actual;

                for (int i = 0; i < grupo.getChildCount(); i++) {

                    if (grupo.getChildAt(i)
                            instanceof android.widget.ImageView) {

                        return true;
                    }
                }
            }

            if (!(actual.getParent() instanceof View)) {
                break;
            }

            actual = (View) actual.getParent();
        }

        return false;
    }

    // ===================================================
    // NUEVO: ADAPTAR BOTONES
    // ===================================================

    private void configurarBotonTema(
            Button boton,
            int fondo,
            int texto,
            int borde,
            int radio) {

        if (boton == null) {
            return;
        }

        // EVITAR QUE EL TINTE ORIGINAL TAPE
        // EL COLOR DEL CONTRASTE

        boton.setBackgroundTintList(null);

        GradientDrawable forma = new GradientDrawable();

        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(fondo);
        forma.setCornerRadius(dp(radio));
        forma.setStroke(dp(2), borde);

        boton.setBackground(forma);
        boton.setTextColor(texto);
    }

    // ===================================================
    // NUEVO: RESTAURAR EL DISEÑO ORIGINAL
    // ===================================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) {
            return;
        }

        // RESTAURAR FONDOS Y TINTES

        if (fondosOriginales.containsKey(vista)) {

            vista.setBackgroundTintList(null);

            vista.setBackground(
                    fondosOriginales.get(vista)
            );

            vista.setBackgroundTintList(
                    tintesOriginales.get(vista)
            );
        }

        // RESTAURAR LOS COLORES ORIGINALES

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal =
                    coloresOriginales.get(texto);

            if (colorOriginal != null) {

                texto.setTextColor(colorOriginal);
            }
        }

        // RESTAURAR LOS ELEMENTOS HIJOS

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
