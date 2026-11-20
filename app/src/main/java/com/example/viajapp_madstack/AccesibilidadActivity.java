
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class AccesibilidadActivity extends AppCompatActivity {

    private LinearLayout navPlanear, navAccesibilidad, navItinerario, navLugares, barraAccesibilidad;

    private TextView btnDisminuirTexto, btnAumentarTexto, btnContraste, btnAudio, btnPreferencias, btnAccesibilidad;
    private TextView btnCheckMovilidad, btnCheckCortos, btnCheckBanos, btnCheckVisual, btnCheckAuditiva;
    private View pantallaPrincipal, scrollContenido, header, bottomNav;

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

        // INICIAR ACCESIBILIDAD
        configurarAccesibilidad();


        // ===================================================
        // MENÚ DESPLEGABLE DEL PERFIL DE USUARIO
        // ===================================================

        View btnAbrirPerfil = findViewById(R.id.btnAbrirPerfil);

        btnAbrirPerfil.setOnClickListener(v -> {

            MenuPerfilUsuario.mostrar(
                    AccesibilidadActivity.this,
                    btnAbrirPerfil,
                    modoContraste,
                    escalaTexto
            );

        });



        // NAVEGACIÓN INFERIOR ORIGINAL
        configurarNavegacion();
        configurarBotonesSeleccion();

        // ABRIR PANTALLA DE PRESUPUESTO Y GASTOS
        findViewById(R.id.btnIrPresupuesto).setOnClickListener(v -> {
            Intent intent = new Intent(
                    AccesibilidadActivity.this,
                    PresupuestoActivity.class
            );
            startActivity(intent);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (MenuSeguridad.manejarResultadoDictado(requestCode, resultCode, data)
                || MenuEditarPerfil.manejarResultadoDictado(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // NAVEGACIÓN INFERIOR
    private void configurarNavegacion() {
        navPlanear = findViewById(R.id.navPlanear);
        navAccesibilidad = findViewById(R.id.navAccesibilidad);
        navItinerario = findViewById(R.id.navItinerario);
        navLugares = findViewById(R.id.navLugares);

        navPlanear.setOnClickListener(v -> {
            startActivity(new Intent(AccesibilidadActivity.this, PlanearActivity.class));
        });

        navAccesibilidad.setOnClickListener(v -> {
            // Ya estamos en esta pantalla
        });

        navItinerario.setOnClickListener(v -> {
            startActivity(new Intent(AccesibilidadActivity.this, ItinerarioActivity.class));
        });

        navLugares.setOnClickListener(v -> {
            startActivity(new Intent(AccesibilidadActivity.this, LugaresActivity.class));
        });
    }

    // CONFIGURACIÓN DE LOS 5 BOTONES DE SELECCIÓN
    private void configurarBotonesSeleccion() {
        btnCheckMovilidad = findViewById(R.id.btnCheckMovilidad);
        btnCheckCortos = findViewById(R.id.btnCheckCortos);
        btnCheckBanos = findViewById(R.id.btnCheckBanos);
        btnCheckVisual = findViewById(R.id.btnCheckVisual);
        btnCheckAuditiva = findViewById(R.id.btnCheckAuditiva);

        // Todos comienzan en false (no seleccionados) al entrar
        inicializarBoton(btnCheckMovilidad, false);
        inicializarBoton(btnCheckCortos, false);
        inicializarBoton(btnCheckBanos, false);
        inicializarBotonVisual(false);
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
                        Toast.makeText(this,
                                "No se pudo abrir la configuración de accesibilidad",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Ahora no", null)
                .show();
    }

    private void actualizarVistaBoton(TextView boton, boolean seleccionado) {
        if (boton == null) return;

        boton.setText(seleccionado ? "✓" : "");

        if (modoContraste == 1) {
            // MODO CREMA
            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(dp(24));
            fondo.setColor(Color.parseColor(seleccionado ? "#F7EACF" : "#EFE1C4"));
            fondo.setStroke(dp(1), Color.parseColor("#A88659"));

            boton.setBackground(
                    new android.graphics.drawable.InsetDrawable(
                            fondo, dp(6)
                    )
            );
            boton.setTextColor(Color.parseColor("#2B241B"));

        } else if (modoContraste == 2) {
            // MODO OSCURO
            GradientDrawable fondo = new GradientDrawable();
            fondo.setCornerRadius(dp(24));
            fondo.setColor(Color.parseColor(seleccionado ? "#2B2B2B" : "#252525"));
            fondo.setStroke(dp(1), Color.parseColor("#7A7A7A"));

            boton.setBackground(
                    new android.graphics.drawable.InsetDrawable(
                            fondo, dp(6)
                    )
            );
            boton.setTextColor(Color.WHITE);

        } else {
            // MODO NORMAL
            if (seleccionado) {
                boton.setBackgroundResource(R.drawable.bg_check_circle);
                boton.setTextColor(Color.WHITE);
            } else {
                boton.setBackgroundResource(R.drawable.bg_uncheck_circle);
                boton.setTextColor(Color.WHITE);
            }
        }
    }

    // CONFIGURACIÓN DE ACCESIBILIDAD
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
        btnPreferencias = findViewById(R.id.btnPreferencias); // <--- Enlace correcto al ID del XML

        // Guardamos los valores originales de toda la pantalla
        guardarOriginales(pantallaPrincipal);

        btnAccesibilidad.setOnClickListener(v -> {
            boolean mostrar = btnDisminuirTexto.getVisibility() != View.VISIBLE;
            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            LinearLayout.LayoutParams parametrosBoton = (LinearLayout.LayoutParams) btnAccesibilidad.getLayoutParams();
            parametrosBoton.width = mostrar ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT;
            parametrosBoton.weight = mostrar ? 1f : 0f;
            btnAccesibilidad.setLayoutParams(parametrosBoton);

            int espacio = dp(12);
            btnAccesibilidad.setPadding(mostrar ? 0 : espacio, 0, mostrar ? 0 : espacio, 0);

            ConstraintLayout.LayoutParams parametrosBarra = (ConstraintLayout.LayoutParams) barraAccesibilidad.getLayoutParams();
            parametrosBarra.width = mostrar ? 0 : ConstraintLayout.LayoutParams.WRAP_CONTENT;
            barraAccesibilidad.setLayoutParams(parametrosBarra);

            btnAccesibilidad.setContentDescription(mostrar ? "Cerrar menú de accesibilidad" : "Abrir menú de accesibilidad");
        });

        // Disminuir tamaño del texto
        btnDisminuirTexto.setOnClickListener(v -> {
            escalaTexto = Math.max(0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f);

            actualizarTamanoTextos(pantallaPrincipal);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // Aumentar tamaño del texto
        btnAumentarTexto.setOnClickListener(v -> {
            escalaTexto = Math.min(1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f);

            actualizarTamanoTextos(pantallaPrincipal);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // Cambiar modo de contraste
        btnContraste.setOnClickListener(v -> {
            modoContraste++;
            if (modoContraste > 2) {
                modoContraste = 0;
            }
            aplicarTemaContraste();
            PreferenciasAccesibilidad.guardarContraste(this, modoContraste);
        });

        // Botón de audio
        btnAudio.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "No se pudo abrir la configuración de accesibilidad",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Guardar preferencias en Firestore al hacer clic en btnPreferencias
        if (btnPreferencias != null) {
            btnPreferencias.setOnClickListener(v -> {
                guardarPreferenciasEnCloud();
            });
        }
    }

    private void guardarPreferenciasEnCloud() {
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Recolectar el estado actual (true/false) de los 5 botones
        boolean movilidad = btnCheckMovilidad != null && Boolean.TRUE.equals(btnCheckMovilidad.getTag());
        boolean recorridosCortos = btnCheckCortos != null && Boolean.TRUE.equals(btnCheckCortos.getTag());
        boolean banos = btnCheckBanos != null && Boolean.TRUE.equals(btnCheckBanos.getTag()); // Asegúrate de si este campo se llama así en tu Firestore
        boolean visual = btnCheckVisual != null && Boolean.TRUE.equals(btnCheckVisual.getTag());
        boolean auditiva = btnCheckAuditiva != null && Boolean.TRUE.equals(btnCheckAuditiva.getTag());

        // Usamos exactamente el nombre del mapa que tienes en la base de datos ("accesibilidad")
        Map<String, Object> actualizacion = new HashMap<>();
        actualizacion.put("accesibilidad.sillaRuedas", movilidad);
        actualizacion.put("accesibilidad.recorridosCortos", recorridosCortos);
        actualizacion.put("accesibilidad.banosAdaptados", banos); // Cambia "banosAdaptados" si en Firestore se llama diferente
        actualizacion.put("accesibilidad.visionReducida", visual);
        actualizacion.put("accesibilidad.subtitulos", auditiva);

        if (btnPreferencias != null) {
            btnPreferencias.setEnabled(false);
        }

        // Actualizamos únicamente los campos internos dentro del mapa "accesibilidad"[cite: 1]
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

    // GUARDAR DISEÑO ORIGINAL
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

    // CAMBIAR TAMAÑO DE LOS TEXTOS
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

    // APLICAR CONTRASTE
    private void aplicarTemaContraste() {
        restaurarDisenoOriginal(pantallaPrincipal);

        if (modoContraste == 0) {
            btnContraste.setText("◐  Contraste");
            sincronizarBotonesSeleccionDespuesDeRestaurar();
            android.widget.ImageView iconoNormal =
                    findViewById(R.id.imgIconoPresupuesto);

            iconoNormal.clearColorFilter();
            iconoNormal.setImageResource(R.drawable.ic_wallet);
            com.google.android.material.card.MaterialCardView tarjetaNormal =
                    findViewById(R.id.tarjetaIconoPresupuesto);

            tarjetaNormal.setCardBackgroundColor(
                    Color.parseColor("#DBEAFE")
            );
            return;
        }


        int fondoPantalla, fondoTarjeta, borde, textoPrincipal, textoSecundario;

        if (modoContraste == 1) {
            // MODO CREMA
            fondoPantalla = Color.parseColor("#FFF1D6");
            fondoTarjeta = Color.parseColor("#EBD6B4");
            borde = Color.parseColor("#C7AC88");
            textoPrincipal = Color.parseColor("#222222");
            textoSecundario = Color.parseColor("#5A5146");

            btnPreferencias.setBackground(
                    fondoRedondeado(
                            Color.parseColor("#E9D7B8"),
                            Color.parseColor("#B99768"),
                            28
                    )
            );

            btnPreferencias.setTextColor(Color.parseColor("#222222"));
            btnAudio.setBackground(
                    fondoRedondeado(
                            Color.parseColor("#FFFAF0"),
                            borde,
                            12
                    )
            );

            btnContraste.setText("◐  Crema");

        } else {
            // MODO OSCURO
            fondoPantalla = Color.parseColor("#111111");
            fondoTarjeta = Color.parseColor("#222222");
            borde = Color.parseColor("#444444");
            textoPrincipal = Color.WHITE;
            textoSecundario = Color.parseColor("#D0D0D0");

            btnPreferencias.setBackground(
                    fondoRedondeado(
                            Color.parseColor("#303030"),
                            Color.parseColor("#888888"),
                            28
                    )
            );

            btnPreferencias.setTextColor(Color.WHITE);

            btnAudio.setBackground(
                    fondoRedondeado(
                            Color.parseColor("#252525"),
                            borde,
                            12
                    )
            );

            btnContraste.setText("◐  Oscuro");
        }


        pantallaPrincipal.setBackgroundColor(fondoPantalla);
        scrollContenido.setBackgroundColor(fondoPantalla);

        header.setBackgroundColor(fondoTarjeta);
        bottomNav.setBackgroundColor(fondoTarjeta);

        // CORREGIR EL FONDO DE LA OPCIÓN ACTIVA DEL MENÚ INFERIOR
        navAccesibilidad.setBackgroundColor(
                modoContraste == 1
                        ? Color.parseColor("#E9D7B8")
                        : Color.parseColor("#303030")
        );



        GradientDrawable fondoBarra = fondoRedondeado(
                fondoTarjeta,
                modoContraste == 1
                        ? Color.parseColor("#B68A59")
                        : borde,
                16
        );

        fondoBarra.setStroke(
                dp(1),
                modoContraste == 1
                        ? Color.parseColor("#C7AC88")
                        : borde
        );

        barraAccesibilidad.setBackground(fondoBarra);


        int fondoBotones = modoContraste == 1
                ? Color.parseColor("#FFFAF0")
                : fondoTarjeta;

        int bordeBotones = modoContraste == 1
                ? Color.parseColor("#C7AC88")
                : borde;

        btnAccesibilidad.setBackground(
                fondoRedondeado(fondoBotones, bordeBotones, 12));

        btnDisminuirTexto.setBackground(
                fondoRedondeado(fondoBotones, bordeBotones, 12));

        btnAumentarTexto.setBackground(
                fondoRedondeado(fondoBotones, bordeBotones, 12));

        btnContraste.setBackground(
                fondoRedondeado(fondoBotones, bordeBotones, 12));


        actualizarFondosTarjetas(scrollContenido, fondoTarjeta, borde);

        TextView[] opciones = {
                btnCheckMovilidad,
                btnCheckCortos,
                btnCheckBanos,
                btnCheckVisual,
                btnCheckAuditiva
        };

        for (TextView opcion : opciones) {
            if (opcion != null && opcion.getParent() instanceof View) {
                View tarjeta = (View) opcion.getParent();

                tarjeta.setBackground(
                        fondoRedondeado(
                                fondoTarjeta,
                                borde,
                                10
                        )
                );
            }
        }

        actualizarColoresTextos(pantallaPrincipal, textoPrincipal, textoSecundario);

        // Mantener el texto blanco sobre el botón marrón en modo crema
        if (modoContraste == 1 && btnPreferencias != null) {
            btnPreferencias.setTextColor(Color.parseColor("#222222"));
        }

        sincronizarBotonesSeleccionDespuesDeRestaurar();

        // BOTÓN PRESUPUESTO SEGÚN EL CONTRASTE
        TextView btnPresupuesto =
                findViewById(R.id.btnIrPresupuesto);

        com.google.android.material.card.MaterialCardView tarjetaIconoPresupuesto =
                findViewById(R.id.tarjetaIconoPresupuesto);

        android.widget.ImageView imgIconoPresupuesto =
                findViewById(R.id.imgIconoPresupuesto);

        if (modoContraste == 1) {

            btnPresupuesto.setBackground(
                    fondoRedondeado(
                            Color.parseColor("#E9D7B8"),
                            Color.parseColor("#B99768"),
                            28
                    )
            );

            btnPresupuesto.setTextColor(Color.parseColor("#222222"));

            imgIconoPresupuesto.clearColorFilter();
            imgIconoPresupuesto.setImageResource(R.drawable.ic_wallet_crema);

            tarjetaIconoPresupuesto.setCardBackgroundColor(
                    Color.parseColor("#F3DFC0")
            );


        } else if (modoContraste == 2) {

            // OSCURO: BORDE BLANCO MÁS DEFINIDO
            GradientDrawable fondoPresupuesto = new GradientDrawable();

            fondoPresupuesto.setColor(Color.parseColor("#303030"));
            fondoPresupuesto.setCornerRadius(dp(28));
            fondoPresupuesto.setStroke(dp(1), Color.parseColor("#888888"));

            btnPresupuesto.setBackground(fondoPresupuesto);
            btnPresupuesto.setTextColor(Color.WHITE);
            imgIconoPresupuesto.clearColorFilter();
            imgIconoPresupuesto.setImageResource(R.drawable.ic_wallet_oscuro);
            tarjetaIconoPresupuesto.setCardBackgroundColor(
                    Color.parseColor("#2A2A2A")
            );
        }

    }

    private void sincronizarBotonesSeleccionDespuesDeRestaurar() {
        if (btnCheckMovilidad != null) actualizarVistaBoton(btnCheckMovilidad, (Boolean) btnCheckMovilidad.getTag());
        if (btnCheckCortos != null) actualizarVistaBoton(btnCheckCortos, (Boolean) btnCheckCortos.getTag());
        if (btnCheckBanos != null) actualizarVistaBoton(btnCheckBanos, (Boolean) btnCheckBanos.getTag());
        if (btnCheckVisual != null) actualizarVistaBoton(btnCheckVisual, (Boolean) btnCheckVisual.getTag());
        if (btnCheckAuditiva != null) actualizarVistaBoton(btnCheckAuditiva, (Boolean) btnCheckAuditiva.getTag());
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

        if (vista.getId() != R.id.tarjetaIconoPresupuesto) {
            if (fondoOriginal != null) {
                Drawable copia = fondoOriginal.getConstantState() != null
                        ? fondoOriginal.getConstantState().newDrawable().mutate()
                        : fondoOriginal.mutate();
                vista.setBackground(copia);
            } else {
                vista.setBackground(null);
            }
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