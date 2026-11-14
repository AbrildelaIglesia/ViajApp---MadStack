
package com.example.viajapp_madstack;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
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

import java.util.IdentityHashMap;

public class AccesibilidadActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares, barraAccesibilidad;

    private TextView btnAccesibilidad, btnDisminuirTexto, btnAumentarTexto, btnContraste, btnAudio, btnCrearViajeIA;

    private View pantallaPrincipal, scrollContenido, header, bottomNav, tarjetaTamanoTexto;

    // Guardado de los valores originales
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    // Estados de accesibilidad
    private float escalaTexto = 1.0f;
    private int modoContraste = 0;
    private boolean controlesVisibles = true;

    // 0 = Normal
    // 1 = Crema
    // 2 = Oscuro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accesibilidad);

        configurarAccesibilidad();
        configurarNavegacion();
    }

    // NAVEGACIÓN INFERIOR
    private void configurarNavegacion() {

        navPlanear = findViewById(R.id.navPlanear);
        navAccesibilidad = findViewById(R.id.navAccesibilidad);
        navItinerario = findViewById(R.id.navItinerario);
        navLugares = findViewById(R.id.navLugares);

        navPlanear.setOnClickListener(v -> {
            startActivity(new Intent(
                    AccesibilidadActivity.this,
                    PlanearActivity.class
            ));
        });

        navAccesibilidad.setOnClickListener(v -> {
            // Ya estamos en esta pantalla
        });

        navItinerario.setOnClickListener(v -> {
            startActivity(new Intent(
                    AccesibilidadActivity.this,
                    ItinerarioActivity.class
            ));
        });

        navLugares.setOnClickListener(v -> {
            startActivity(new Intent(
                    AccesibilidadActivity.this,
                    LugaresActivity.class
            ));
        });
    }

    // CONFIGURACIÓN DE ACCESIBILIDAD
    private void configurarAccesibilidad() {

        pantallaPrincipal = findViewById(R.id.main);
        scrollContenido = findViewById(R.id.scrollContenido);
        header = findViewById(R.id.header);
        bottomNav = findViewById(R.id.bottomNav);
        tarjetaTamanoTexto = findViewById(R.id.tarjetaTamanoTexto);

        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);
        btnCrearViajeIA = findViewById(R.id.btnCrearViajeIA);

        // Guardamos los valores originales de toda la pantalla
        guardarOriginales(pantallaPrincipal);

        // Mostrar u ocultar los controles
        btnAccesibilidad.setOnClickListener(v -> {

            controlesVisibles = !controlesVisibles;

            int visibilidad = controlesVisibles
                    ? View.VISIBLE
                    : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            btnAccesibilidad.setContentDescription(
                    controlesVisibles
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // Disminuir tamaño del texto
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(0.9f, escalaTexto - 0.1f);

            actualizarTamanoTextos(pantallaPrincipal);
        });

        // Aumentar tamaño del texto
        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(1.3f, escalaTexto + 0.1f);

            actualizarTamanoTextos(pantallaPrincipal);
        });

        // Cambiar modo de contraste
        btnContraste.setOnClickListener(v -> {

            modoContraste++;

            if (modoContraste > 2) {
                modoContraste = 0;
            }

            aplicarTemaContraste();
        });

        // Botón de audio
        btnAudio.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "Función de audio próximamente",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // GUARDAR DISEÑO ORIGINAL
    private void guardarOriginales(View vista) {

        if (vista == null) {
            return;
        }

        fondosOriginales.put(
                vista,
                vista.getBackground() != null
                        ? vista.getBackground().getConstantState() != null
                          ? vista.getBackground().getConstantState().newDrawable().mutate()
                          : vista.getBackground().mutate()
                        : null
        );

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            coloresOriginales.put(
                    texto,
                    texto.getTextColors()
            );

            tamanosOriginales.put(
                    texto,
                    texto.getTextSize()
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    // CAMBIAR TAMAÑO DE LOS TEXTOS
    private void actualizarTamanoTextos(View vista) {

        if (vista == null) {
            return;
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            Float tamanoOriginal = tamanosOriginales.get(texto);

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
                actualizarTamanoTextos(grupo.getChildAt(i));
            }
        }
    }

    // APLICAR CONTRASTE
    private void aplicarTemaContraste() {

        // Primero restauramos los valores originales
        restaurarDisenoOriginal(pantallaPrincipal);

        // MODO NORMAL
        if (modoContraste == 0) {

            btnContraste.setText("◐  Contraste");

            return;
        }

        int fondoPantalla, fondoTarjeta, borde, textoPrincipal, textoSecundario;

        // MODO CREMA
        if (modoContraste == 1) {

            fondoPantalla = Color.rgb(255, 247, 230);
            fondoTarjeta = Color.rgb(255, 253, 247);
            borde = Color.rgb(214, 199, 161);

            textoPrincipal = Color.rgb(59, 47, 32);
            textoSecundario = Color.rgb(102, 90, 74);

            btnCrearViajeIA.setBackground(
                    fondoRedondeado(
                            Color.rgb(130, 85, 35),
                            Color.rgb(100, 65, 30),
                            12
                    )
            );
            btnAudio.setBackground(
                    fondoRedondeado(
                            Color.rgb(245, 235, 215),
                            Color.rgb(180, 160, 130),
                            12
                    )
            );
            btnContraste.setText("◐  Crema");

        } else {

            // MODO OSCURO
            fondoPantalla = Color.rgb(18, 18, 18);
            fondoTarjeta = Color.rgb(30, 30, 30);
            borde = Color.rgb(85, 85, 85);

            textoPrincipal = Color.WHITE;
            textoSecundario = Color.rgb(210, 210, 210);

            btnCrearViajeIA.setBackground(
                    fondoRedondeado(
                            Color.rgb(55, 55, 55),
                            Color.rgb(120, 120, 120),
                            12
                    )
            );
            btnAudio.setBackground(
                    fondoRedondeado(
                            Color.rgb(55, 55, 55),
                            Color.rgb(120, 120, 120),
                            12
                    )
            );
            btnContraste.setText("◐  Oscuro");
        }

        // Fondo general
        pantallaPrincipal.setBackgroundColor(fondoPantalla);
        scrollContenido.setBackgroundColor(fondoPantalla);

        // Encabezado y navegación
        header.setBackgroundColor(fondoTarjeta);
        bottomNav.setBackgroundColor(fondoTarjeta);

        // Barra de accesibilidad
        barraAccesibilidad.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 16)
        );

        btnAccesibilidad.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnDisminuirTexto.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnAumentarTexto.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnContraste.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        // Tarjetas del contenido
        actualizarFondosTarjetas(
                scrollContenido,
                fondoTarjeta,
                borde
        );

        // Cambiar los colores de los textos
        actualizarColoresTextos(
                pantallaPrincipal,
                textoPrincipal,
                textoSecundario
        );
    }

    // ACTUALIZAR FONDOS DE LAS TARJETAS
    private void actualizarFondosTarjetas(
            View vista,
            int colorFondo,
            int colorBorde) {

        if (vista == null) {
            return;
        }

        if (vista instanceof LinearLayout) {

            Drawable fondoOriginal = fondosOriginales.get(vista);

            if (fondoOriginal instanceof GradientDrawable) {

                vista.setBackground(
                        fondoRedondeado(colorFondo, colorBorde, 10)
                );
            }
        }

        // Iconos marcados con tag="interest"
        if (vista.getTag() != null
                && "interest".equals(vista.getTag().toString())) {

            int fondoIcono = modoContraste == 2
                    ? Color.rgb(55, 55, 55)
                    : Color.rgb(237, 227, 201);

            vista.setBackground(
                    fondoRedondeado(fondoIcono, colorBorde, 8)
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarFondosTarjetas(
                        grupo.getChildAt(i),
                        colorFondo,
                        colorBorde
                );
            }
        }
    }

    // =====================================================
    // ACTUALIZAR COLORES DE LOS TEXTOS
    // =====================================================

    private void actualizarColoresTextos(
            View vista,
            int colorPrincipal,
            int colorSecundario) {

        if (vista == null) {
            return;
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal = coloresOriginales.get(texto);

            if (colorOriginal != null) {

                int color = colorOriginal.getDefaultColor();

                if ("Logo ViajAPP".equals(
                        texto.getContentDescription())
                        || "Perfil".equals(
                        texto.getContentDescription())) {

                    texto.setTextColor(colorOriginal);
                }

                // Textos secundarios
                else if (color == Color.rgb(100, 116, 139)
                        || color == Color.rgb(67, 71, 77)
                        || color == Color.rgb(30, 41, 59)) {

                    texto.setTextColor(colorSecundario);
                }

                // Textos principales
                else {
                    texto.setTextColor(colorPrincipal);
                }
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarColoresTextos(
                        grupo.getChildAt(i),
                        colorPrincipal,
                        colorSecundario
                );
            }
        }
    }

    // =====================================================
    // RESTAURAR DISEÑO ORIGINAL
    // =====================================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) {
            return;
        }

        Drawable fondoOriginal = fondosOriginales.get(vista);

        if (fondoOriginal != null) {
            Drawable copia = fondoOriginal.getConstantState() != null
                    ? fondoOriginal.getConstantState().newDrawable().mutate()
                    : fondoOriginal.mutate();

            vista.setBackground(copia);
        } else {
            vista.setBackground(null);
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList colorOriginal = coloresOriginales.get(texto);

            if (colorOriginal != null) {
                texto.setTextColor(colorOriginal);
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                restaurarDisenoOriginal(
                        grupo.getChildAt(i)
                );
            }
        }
    }

    // =====================================================
    // CREAR FONDOS REDONDEADOS
    // =====================================================

    private GradientDrawable fondoRedondeado(
            int colorFondo,
            int colorBorde,
            int radio) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(colorFondo);
        drawable.setCornerRadius(dp(radio));
        drawable.setStroke(dp(1), colorBorde);

        return drawable;
    }

    // =====================================================
    // CONVERTIR DP A PIXELES
    // =====================================================

    private int dp(int valor) {

        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }
}
