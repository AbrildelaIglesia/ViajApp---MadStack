package com.example.viajapp_madstack;

import android.content.Intent;
import android.app.AlertDialog;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class AccesibilidadActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares, barraAccesibilidad;

    private TextView btnAccesibilidad, btnDisminuirTexto, btnAumentarTexto, btnContraste, btnAudio, btnPreferencias;
    private TextView btnCheckMovilidad, btnCheckCortos, btnCheckBanos, btnCheckVisual, btnCheckAuditiva;
    private View pantallaPrincipal, scrollContenido, header, bottomNav, tarjetaTamanoTexto;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private String userId;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accesibilidad);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, InicioActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();

        configurarAccesibilidad();
        configurarNavegacion();
        configurarBotonesSeleccion();

        // Menú desplegable del perfil de usuario
        View btnAbrirPerfil = findViewById(R.id.btnAbrirPerfil);
        if (btnAbrirPerfil != null) {
            btnAbrirPerfil.setOnClickListener(v -> {
                MenuPerfilUsuario.mostrar(
                        AccesibilidadActivity.this,
                        btnAbrirPerfil,
                        modoContraste,
                        escalaTexto
                );
            });
        }

        // Cargar las preferencias guardadas de Firestore al iniciar (Versión B)
        cargarPreferenciasDesdeCloud();
    }

    // NAVEGACIÓN INFERIOR
    private void configurarNavegacion() {
        navPlanear = findViewById(R.id.navPlanear);
        navAccesibilidad = findViewById(R.id.navAccesibilidad);
        navItinerario = findViewById(R.id.navItinerario);
        navLugares = findViewById(R.id.navLugares);

        if (navPlanear != null) {
            navPlanear.setOnClickListener(v -> startActivity(new Intent(AccesibilidadActivity.this, PlanearActivity.class)));
        }
        if (navItinerario != null) {
            navItinerario.setOnClickListener(v -> startActivity(new Intent(AccesibilidadActivity.this, ItinerarioActivity.class)));
        }
        if (navLugares != null) {
            navLugares.setOnClickListener(v -> startActivity(new Intent(AccesibilidadActivity.this, LugaresActivity.class)));
        }
    }

    // CONFIGURACIÓN DE LOS 5 BOTONES DE SELECCIÓN
    private void configurarBotonesSeleccion() {
        btnCheckMovilidad = findViewById(R.id.btnCheckMovilidad);
        btnCheckCortos = findViewById(R.id.btnCheckCortos);
        btnCheckBanos = findViewById(R.id.btnCheckBanos);
        btnCheckVisual = findViewById(R.id.btnCheckVisual);
        btnCheckAuditiva = findViewById(R.id.btnCheckAuditiva);

        inicializarBoton(btnCheckMovilidad, false);
        inicializarBoton(btnCheckCortos, false);
        inicializarBoton(btnCheckBanos, false);
        inicializarBotonVisual(false); // <--- Lógica especial con TalkBack (Versión A)
        inicializarBoton(btnCheckAuditiva, false);
    }

    private void inicializarBoton(TextView boton, boolean seleccionadoInicial) {
        if (boton == null) return;

        boton.setTag(seleccionadoInicial);
        actualizarVistaBoton(boton, seleccionadoInicial);
        configurarSemanticaCasilla(boton, nombrePreferencia(boton));

        boton.setOnClickListener(v -> {
            boolean seleccionado = !Boolean.TRUE.equals(boton.getTag());
            boton.setTag(seleccionado);
            actualizarVistaBoton(boton, seleccionado);
        });
    }

    private void inicializarBotonVisual(boolean seleccionadoInicial) {
        if (btnCheckVisual == null) return;

        btnCheckVisual.setTag(seleccionadoInicial);
        actualizarVistaBoton(btnCheckVisual, seleccionadoInicial);
        configurarSemanticaCasilla(btnCheckVisual, "Dificultad visual");

        btnCheckVisual.setOnClickListener(v -> {
            boolean seleccionado = !Boolean.TRUE.equals(btnCheckVisual.getTag());
            btnCheckVisual.setTag(seleccionado);
            actualizarVistaBoton(btnCheckVisual, seleccionado);

            if (seleccionado) {
                preguntarPorTalkBack();
            }
        });
    }

    private String nombrePreferencia(TextView boton) {
        if (boton == btnCheckMovilidad) return "Silla de ruedas o movilidad reducida";
        if (boton == btnCheckCortos) return "Priorizar recorridos cortos";
        if (boton == btnCheckBanos) return "Baños adaptados";
        if (boton == btnCheckAuditiva) return "Dificultad auditiva";
        return "Preferencia de accesibilidad";
    }

    private void configurarSemanticaCasilla(TextView boton, String nombre) {
        boton.setContentDescription(nombre);
        boton.setClickable(true);
        boton.setFocusable(true);
        ViewCompat.setAccessibilityDelegate(boton, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.CheckBox");
                info.setCheckable(true);
                info.setChecked(Boolean.TRUE.equals(boton.getTag()));
            }
        });
    }

    private void preguntarPorTalkBack() {
        new AlertDialog.Builder(this)
                .setTitle("Dificultad visual")
                .setMessage("¿Querés abrir los ajustes de Android para configurar TalkBack? "
                        + "Es opcional y no modifica tu preferencia de viaje.")
                .setPositiveButton("Configurar TalkBack", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "No se pudo abrir la configuración de accesibilidad", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Ahora no", null)
                .show();
    }

    // CARGAR PREFERENCIAS DESDE FIRESTORE (Versión B)
    private void cargarPreferenciasDesdeCloud() {
        firestore.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> accesibilidad = (Map<String, Object>) documentSnapshot.get("accesibilidad");

                        if (accesibilidad != null) {
                            boolean movilidad = Boolean.TRUE.equals(accesibilidad.get("sillaRuedas"));
                            boolean cortos = Boolean.TRUE.equals(accesibilidad.get("recorridosCortos"));
                            boolean banos = Boolean.TRUE.equals(accesibilidad.get("banosAdaptados"));
                            boolean visual = Boolean.TRUE.equals(accesibilidad.get("visionReducida"));
                            boolean auditiva = Boolean.TRUE.equals(accesibilidad.get("subtitulos"));

                            if (btnCheckMovilidad != null) {
                                btnCheckMovilidad.setTag(movilidad);
                                actualizarVistaBoton(btnCheckMovilidad, movilidad);
                            }
                            if (btnCheckCortos != null) {
                                btnCheckCortos.setTag(cortos);
                                actualizarVistaBoton(btnCheckCortos, cortos);
                            }
                            if (btnCheckBanos != null) {
                                btnCheckBanos.setTag(banos);
                                actualizarVistaBoton(btnCheckBanos, banos);
                            }
                            if (btnCheckVisual != null) {
                                btnCheckVisual.setTag(visual);
                                actualizarVistaBoton(btnCheckVisual, visual);
                            }
                            if (btnCheckAuditiva != null) {
                                btnCheckAuditiva.setTag(auditiva);
                                actualizarVistaBoton(btnCheckAuditiva, auditiva);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar preferencias: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarVistaBoton(TextView boton, boolean seleccionado) {
        if (seleccionado) {
            boton.setBackgroundResource(R.drawable.bg_check_circle);
            boton.setText("✓");
        } else {
            boton.setBackgroundResource(R.drawable.bg_uncheck_circle);
            boton.setText("");
        }
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
        btnPreferencias = findViewById(R.id.btnPreferencias);

        guardarOriginales(pantallaPrincipal);

        if (btnAccesibilidad != null) {
            btnAccesibilidad.setOnClickListener(v -> {
                controlesVisibles = !controlesVisibles;
                int visibilidad = controlesVisibles ? View.VISIBLE : View.GONE;

                if (btnDisminuirTexto != null) btnDisminuirTexto.setVisibility(visibilidad);
                if (btnAumentarTexto != null) btnAumentarTexto.setVisibility(visibilidad);
                if (btnContraste != null) btnContraste.setVisibility(visibilidad);
                if (btnAudio != null) btnAudio.setVisibility(visibilidad);

                btnAccesibilidad.setContentDescription(
                        controlesVisibles ? "Cerrar menú de accesibilidad" : "Abrir menú de accesibilidad"
                );
            });
        }

        if (btnDisminuirTexto != null) {
            btnDisminuirTexto.setOnClickListener(v -> {
                escalaTexto = Math.max(0.9f, escalaTexto - 0.1f);
                actualizarTamanoTextos(pantallaPrincipal);
            });
        }

        if (btnAumentarTexto != null) {
            btnAumentarTexto.setOnClickListener(v -> {
                escalaTexto = Math.min(1.3f, escalaTexto + 0.1f);
                actualizarTamanoTextos(pantallaPrincipal);
            });
        }

        if (btnContraste != null) {
            btnContraste.setOnClickListener(v -> {
                modoContraste++;
                if (modoContraste > 2) {
                    modoContraste = 0;
                }
                aplicarTemaContraste();
            });
        }

        if (btnAudio != null) {
            btnAudio.setOnClickListener(v -> {
                Toast.makeText(this, "Función de audio próximamente", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnPreferencias != null) {
            btnPreferencias.setOnClickListener(v -> guardarPreferenciasEnCloud());
        }
    }

    private void guardarPreferenciasEnCloud() {
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean movilidad = btnCheckMovilidad != null && Boolean.TRUE.equals(btnCheckMovilidad.getTag());
        boolean recorridosCortos = btnCheckCortos != null && Boolean.TRUE.equals(btnCheckCortos.getTag());
        boolean banos = btnCheckBanos != null && Boolean.TRUE.equals(btnCheckBanos.getTag());
        boolean visual = btnCheckVisual != null && Boolean.TRUE.equals(btnCheckVisual.getTag());
        boolean auditiva = btnCheckAuditiva != null && Boolean.TRUE.equals(btnCheckAuditiva.getTag());

        Map<String, Object> actualizacion = new HashMap<>();
        actualizacion.put("accesibilidad.sillaRuedas", movilidad);
        actualizacion.put("accesibilidad.recorridosCortos", recorridosCortos);
        actualizacion.put("accesibilidad.banosAdaptados", banos);
        actualizacion.put("accesibilidad.visionReducida", visual);
        actualizacion.put("accesibilidad.subtitulos", auditiva);

        if (btnPreferencias != null) {
            btnPreferencias.setEnabled(false);
        }

        firestore.collection("usuarios").document(userId)
                .update(actualizacion)
                .addOnSuccessListener(aVoid -> {
                    if (btnPreferencias != null) btnPreferencias.setEnabled(true);
                    Toast.makeText(this, "¡Preferencias actualizadas!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (btnPreferencias != null) btnPreferencias.setEnabled(true);
                    Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void guardarOriginales(View vista) {
        if (vista == null) return;

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
            coloresOriginales.put(texto, texto.getTextColors());
            tamanosOriginales.put(texto, texto.getTextSize());
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    private void actualizarTamanoTextos(View vista) {
        if (vista == null) return;

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

    private void aplicarTemaContraste() {
        restaurarDisenoOriginal(pantallaPrincipal);

        if (modoContraste == 0) {
            if (btnContraste != null) btnContraste.setText("◐  Contraste");
            sincronizarBotonesSeleccionDespuesDeRestaurar();
            return;
        }

        int fondoPantalla, fondoTarjeta, borde, textoPrincipal, textoSecundario;

        if (modoContraste == 1) {
            fondoPantalla = Color.rgb(255, 247, 230);
            fondoTarjeta = Color.rgb(255, 253, 247);
            borde = Color.rgb(214, 199, 161);

            textoPrincipal = Color.rgb(59, 47, 32);
            textoSecundario = Color.rgb(102, 90, 74);

            if (btnPreferencias != null) {
                btnPreferencias.setBackground(fondoRedondeado(Color.rgb(130, 85, 35), Color.rgb(100, 65, 30), 12));
            }
            if (btnAudio != null) {
                btnAudio.setBackground(fondoRedondeado(Color.rgb(245, 235, 215), Color.rgb(180, 160, 130), 12));
            }
            if (btnContraste != null) btnContraste.setText("◐  Crema");

        } else {
            fondoPantalla = Color.rgb(18, 18, 18);
            fondoTarjeta = Color.rgb(30, 30, 30);
            borde = Color.rgb(85, 85, 85);

            textoPrincipal = Color.WHITE;
            textoSecundario = Color.rgb(210, 210, 210);

            if (btnPreferencias != null) {
                btnPreferencias.setBackground(fondoRedondeado(Color.rgb(55, 55, 55), Color.rgb(120, 120, 120), 12));
            }
            if (btnAudio != null) {
                btnAudio.setBackground(fondoRedondeado(Color.rgb(55, 55, 55), Color.rgb(120, 120, 120), 12));
            }
            if (btnContraste != null) btnContraste.setText("◐  Oscuro");
        }

        if (pantallaPrincipal != null) pantallaPrincipal.setBackgroundColor(fondoPantalla);
        if (scrollContenido != null) scrollContenido.setBackgroundColor(fondoPantalla);
        if (header != null) header.setBackgroundColor(fondoTarjeta);
        if (bottomNav != null) bottomNav.setBackgroundColor(fondoTarjeta);
        if (barraAccesibilidad != null) barraAccesibilidad.setBackground(fondoRedondeado(fondoTarjeta, borde, 16));

        if (btnAccesibilidad != null) btnAccesibilidad.setBackground(fondoRedondeado(fondoTarjeta, borde, 12));
        if (btnDisminuirTexto != null) btnDisminuirTexto.setBackground(fondoRedondeado(fondoTarjeta, borde, 12));
        if (btnAumentarTexto != null) btnAumentarTexto.setBackground(fondoRedondeado(fondoTarjeta, borde, 12));
        if (btnContraste != null) btnContraste.setBackground(fondoRedondeado(fondoTarjeta, borde, 12));

        if (scrollContenido != null) actualizarFondosTarjetas(scrollContenido, fondoTarjeta, borde);
        if (pantallaPrincipal != null) actualizarColoresTextos(pantallaPrincipal, textoPrincipal, textoSecundario);
        sincronizarBotonesSeleccionDespuesDeRestaurar();
    }

    private void sincronizarBotonesSeleccionDespuesDeRestaurar() {
        if (btnCheckMovilidad != null) actualizarVistaBoton(btnCheckMovilidad, Boolean.TRUE.equals(btnCheckMovilidad.getTag()));
        if (btnCheckCortos != null) actualizarVistaBoton(btnCheckCortos, Boolean.TRUE.equals(btnCheckCortos.getTag()));
        if (btnCheckBanos != null) actualizarVistaBoton(btnCheckBanos, Boolean.TRUE.equals(btnCheckBanos.getTag()));
        if (btnCheckVisual != null) actualizarVistaBoton(btnCheckVisual, Boolean.TRUE.equals(btnCheckVisual.getTag()));
        if (btnCheckAuditiva != null) actualizarVistaBoton(btnCheckAuditiva, Boolean.TRUE.equals(btnCheckAuditiva.getTag()));
    }

    private void actualizarFondosTarjetas(View vista, int colorFondo, int colorBorde) {
        if (vista == null) return;

        if (vista instanceof LinearLayout) {
            Drawable fondoOriginal = fondosOriginales.get(vista);
            if (fondoOriginal instanceof GradientDrawable) {
                vista.setBackground(fondoRedondeado(colorFondo, colorBorde, 10));
            }
        }

        if (vista.getTag() != null && "interest".equals(vista.getTag().toString())) {
            int fondoIcono = modoContraste == 2 ? Color.rgb(55, 55, 55) : Color.rgb(237, 227, 201);
            vista.setBackground(fondoRedondeado(fondoIcono, colorBorde, 8));
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarFondosTarjetas(grupo.getChildAt(i), colorFondo, colorBorde);
            }
        }
    }

    private void actualizarColoresTextos(View vista, int colorPrincipal, int colorSecundario) {
        if (vista == null) return;

        if (vista instanceof TextView) {
            TextView texto = (TextView) vista;
            ColorStateList colorOriginal = coloresOriginales.get(texto);

            if (colorOriginal != null) {
                int color = colorOriginal.getDefaultColor();

                if ("Logo ViajAPP".equals(texto.getContentDescription())
                        || "Perfil".equals(texto.getContentDescription())
                        || texto == btnCheckMovilidad || texto == btnCheckCortos
                        || texto == btnCheckBanos || texto == btnCheckVisual
                        || texto == btnCheckAuditiva) {

                    if (Boolean.TRUE.equals(texto.getTag())) {
                        texto.setTextColor(Color.WHITE);
                    } else {
                        texto.setTextColor(colorOriginal);
                    }
                } else if (color == Color.rgb(100, 116, 139)
                        || color == Color.rgb(67, 71, 77)
                        || color == Color.rgb(30, 41, 59)) {
                    texto.setTextColor(colorSecundario);
                } else {
                    texto.setTextColor(colorPrincipal);
                }
            }
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarColoresTextos(grupo.getChildAt(i), colorPrincipal, colorSecundario);
            }
        }
    }

    private void restaurarDisenoOriginal(View vista) {
        if (vista == null) return;

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
                restaurarDisenoOriginal(grupo.getChildAt(i));
            }
        }
    }

    private GradientDrawable fondoRedondeado(int colorFondo, int colorBorde, int radio) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colorFondo);
        drawable.setCornerRadius(dp(radio));
        drawable.setStroke(dp(1), colorBorde);
        return drawable;
    }

    private int dp(int valor) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }
}