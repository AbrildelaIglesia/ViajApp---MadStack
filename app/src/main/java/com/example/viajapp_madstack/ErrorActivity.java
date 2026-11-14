
package com.example.viajapp_madstack;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// ===================================================
// NUEVO: IMPORTACIONES PARA ACCESIBILIDAD
// ===================================================

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.IdentityHashMap;

public class ErrorActivity extends AppCompatActivity {

    // ===================================================
    // NUEVO: VARIABLES DE ACCESIBILIDAD
    // ===================================================

    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;

    private View pantallaPrincipal;
    private View scrollContenido;

    // ELEMENTOS DE ESTA PANTALLA
    private View contenedorIlustracion;
    private View contenedorRecomendaciones;
    private View btnReintentarConexion;

    // GUARDAR LOS ESTILOS ORIGINALES
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    // TAMAÑO DE TEXTO
    private float escalaTexto = 1.0f;

    // CONTRASTE
    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error_de_conexion);

        // NUEVO: ACTIVAR ACCESIBILIDAD
        configurarAccesibilidad();
    }

    // ===================================================
    // NUEVO: CONFIGURAR MENÚ DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        // EL XML NO TIENE ID EN SU CONTENEDOR PRINCIPAL
        ViewGroup contenidoActivity = findViewById(
                android.R.id.content
        );

        pantallaPrincipal = contenidoActivity.getChildAt(0);

        scrollContenido = findViewById(R.id.scrollContenido);

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

        btnReintentarConexion = findViewById(
                R.id.btnReintentarConexion
        );

        // ENCONTRAR EL RECUADRO DE LA ILUSTRACIÓN
        ImageView imagenError = findViewById(
                R.id.imgErrorConexion
        );

        if (imagenError != null) {
            contenedorIlustracion =
                    (View) imagenError.getParent();
        }

        // ENCONTRAR EL RECUADRO DE RECOMENDACIONES
        TextView tituloRecomendaciones = buscarTexto(
                pantallaPrincipal,
                "Pasos recomendados para continuar"
        );

        if (tituloRecomendaciones != null) {
            contenedorRecomendaciones =
                    (View) tituloRecomendaciones.getParent();
        }

        // GUARDAR TODO ANTES DE MODIFICAR COLORES
        guardarOriginales(pantallaPrincipal);

        // ABRIR Y CERRAR EL MENÚ
        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility()
                            != View.VISIBLE;

            int visibilidad = mostrar
                    ? View.VISIBLE
                    : View.GONE;

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

            btnAccesibilidad.setLayoutParams(
                    parametrosBoton
            );

            int espacio = dp(12);

            btnAccesibilidad.setPadding(
                    mostrar ? 0 : espacio,
                    0,
                    mostrar ? 0 : espacio,
                    0
            );

            // AJUSTAR BARRA DE ACCESIBILIDAD
            ConstraintLayout.LayoutParams parametrosBarra =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = mostrar
                    ? 0
                    : ConstraintLayout.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(
                    parametrosBarra
            );

            btnAccesibilidad.setContentDescription(
                    mostrar
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // AUMENTAR TEXTO
        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round(
                            (escalaTexto + 0.1f) * 10f
                    ) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
        });

        // DISMINUIR TEXTO
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round(
                            (escalaTexto - 0.1f) * 10f
                    ) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
        });

        // CAMBIAR CONTRASTE
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

        // EL BOTÓN AUDIO SE PROGRAMARÁ DESPUÉS
    }

    // ===================================================
    // NUEVO: GUARDAR LOS ESTILOS ORIGINALES
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
    // NUEVO: AUMENTAR Y DISMINUIR LOS TEXTOS
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

        // MISMOS COLORES QUE EN REGISTRO
        int fondoPantalla = Color.parseColor(
                crema ? "#FFF1D6" : "#111111"
        );

        int fondoBarra = Color.parseColor(
                crema ? "#F6E8CF" : "#181818"
        );

        int fondoControl = Color.parseColor(
                crema ? "#FFFAF0" : "#252525"
        );

        int fondoRecuadro = Color.parseColor(
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

        // MODO NORMAL: RECUPERAR EL DISEÑO ORIGINAL
        if (normal) {

            restaurarDisenoOriginal(pantallaPrincipal);
            return;
        }

        // 1. FONDO PRINCIPAL
        // CREAR NUEVOS FONDOS PARA NO MODIFICAR LOS ORIGINALES
        pantallaPrincipal.setBackground(
                new ColorDrawable(fondoPantalla)
        );

        scrollContenido.setBackground(
                new ColorDrawable(fondoPantalla)
        );

        // 2. BARRA DE ACCESIBILIDAD
        barraAccesibilidad.setBackground(
                fondoRedondeado(fondoBarra, borde, 16)
        );

        // 3. BOTONES DEL MENÚ
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

        // 4. TEXTOS DEL CONTENIDO
        actualizarColoresTextos(
                scrollContenido,
                textoPrincipal,
                textoSecundario
        );

        // 5. RECUADRO DE LA ILUSTRACIÓN
        if (contenedorIlustracion != null) {

            contenedorIlustracion.setBackground(
                    fondoRedondeado(
                            fondoRecuadro,
                            borde,
                            12
                    )
            );
        }

        // LA IMAGEN DEL GATITO NO SE MODIFICA
        // NI SE LE APLICA NINGÚN TINTE

        // 6. RECUADRO DE RECOMENDACIONES
        if (contenedorRecomendaciones != null) {

            contenedorRecomendaciones.setBackground(
                    fondoRedondeado(
                            fondoRecuadro,
                            borde,
                            12
                    )
            );
        }

        // 7. BOTÓN REINTENTAR CONEXIÓN
        if (btnReintentarConexion != null) {

            btnReintentarConexion.setBackground(
                    fondoRedondeado(
                            fondoRecuadro,
                            borde,
                            12
                    )
            );
        }

        // 8. LÍNEA DIVISORIA
        actualizarLineaDivisoria(
                pantallaPrincipal,
                borde
        );
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

                int colorOriginal =
                        original.getDefaultColor();

                boolean esSecundario =
                        colorOriginal == Color.parseColor("#6B7280")
                                || colorOriginal == Color.parseColor("#4B5563")
                                || colorOriginal == Color.parseColor("#64748B");

                texto.setTextColor(
                        esSecundario ? secundario : principal
                );
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
    // NUEVO: ADAPTAR LA LÍNEA DIVISORIA
    // ===================================================

    private void actualizarLineaDivisoria(
            View vista,
            int color) {

        if (vista == null) {
            return;
        }

        if (!(vista instanceof ViewGroup)
                && !(vista instanceof TextView)
                && !(vista instanceof ImageView)
                && vista.getLayoutParams() != null
                && vista.getLayoutParams().height == dp(1)) {

            vista.setBackground(
                    new ColorDrawable(color)
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarLineaDivisoria(
                        grupo.getChildAt(i),
                        color
                );
            }
        }
    }

    // ===================================================
    // NUEVO: BUSCAR TEXTO EN LA PANTALLA
    // ===================================================

    private TextView buscarTexto(
            View vista,
            String contenidoBuscado) {

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            if (texto.getText() != null
                    && texto.getText().toString()
                    .contains(contenidoBuscado)) {

                return texto;
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                TextView encontrado = buscarTexto(
                        grupo.getChildAt(i),
                        contenidoBuscado
                );

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    // ===================================================
    // NUEVO: RESTAURAR EL DISEÑO NORMAL
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

        // RESTAURAR COLOR ORIGINAL DE LOS TEXTOS
        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal =
                    coloresOriginales.get(texto);

            if (colorOriginal != null) {

                texto.setTextColor(colorOriginal);
            }
        }

        // RECORRER TODOS LOS ELEMENTOS
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
    // NUEVO: FONDOS REDONDEADOS
    // ===================================================

    private Drawable fondoRedondeado(
            int colorFondo,
            int colorBorde,
            int radioDp) {

        GradientDrawable forma =
                new GradientDrawable();

        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(colorFondo);
        forma.setCornerRadius(dp(radioDp));

        forma.setStroke(
                dp(1),
                colorBorde
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
