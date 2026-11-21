
package com.example.viajapp_madstack;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;

public class HistorialPresupuestosActivity
        extends AppCompatActivity {

    private View pantallaPrincipal;
    private LinearLayout barraAccesibilidad;
    private LinearLayout contenedor;
    private TextView estado;

    private TextView btnAccesibilidad;
    private TextView btnDisminuirTexto;
    private TextView btnAumentarTexto;
    private TextView btnContraste;
    private TextView btnAudio;

    private int modoContraste = 0;
    private float escalaTexto = 1.0f;
    private boolean controlesVisibles = true;

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Integer> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<MaterialCardView, int[]> tarjetasOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historial_presupuestos);

        pantallaPrincipal = findViewById(R.id.main);
        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);
        contenedor = findViewById(R.id.contenedorHistorialViajes);
        estado = findViewById(R.id.tvEstadoHistorial);

        guardarOriginales(pantallaPrincipal);

        // VOLVER A LA PANTALLA ANTERIOR
        findViewById(R.id.btnVolverPresupuesto)
                .setOnClickListener(v -> finish());

        // PERFIL
        View botonPerfil = findViewById(R.id.btnAbrirPerfil);

        botonPerfil.setOnClickListener(v ->
                MenuPerfilUsuario.mostrar(
                        this,
                        botonPerfil,
                        modoContraste,
                        escalaTexto
                )
        );

        configurarAccesibilidad();
        cargarHistorial();
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

    // ==========================================
    // CARGAR LOS VIAJES DEL USUARIO
    // ==========================================

    private void cargarHistorial() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        contenedor.removeAllViews();
        estado.setVisibility(View.VISIBLE);

        if (usuario == null) {
            estado.setText("Iniciá sesión para consultar tus gastos.");
            return;
        }

        estado.setText("Cargando tus presupuestos y gastos...");

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .collection("itinerarios")
                .get()
                .addOnSuccessListener(resultado -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (resultado.isEmpty()) {
                        estado.setText(
                                "Todavía no tenés viajes guardados."
                        );
                        return;
                    }

                    List<DocumentSnapshot> viajes =
                            new ArrayList<>(resultado.getDocuments());

                    viajes.sort((a, b) -> {
                        java.util.Date fechaA =
                                a.getDate("fechaCreacion");
                        java.util.Date fechaB =
                                b.getDate("fechaCreacion");

                        if (fechaA == null && fechaB == null) return 0;
                        if (fechaA == null) return 1;
                        if (fechaB == null) return -1;

                        return fechaB.compareTo(fechaA);
                    });

                    estado.setVisibility(View.GONE);

                    for (DocumentSnapshot viaje : viajes) {
                        crearTarjetaViaje(viaje);
                    }

                    actualizarVista();

                })
                .addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) return;

                    estado.setVisibility(View.VISIBLE);
                    estado.setText(
                            "No se pudieron cargar los viajes. "
                                    + "Revisá tu conexión."
                    );
                });
    }

    // ==========================================
    // TARJETA DE CADA VIAJE
    // ==========================================

    private void crearTarjetaViaje(DocumentSnapshot viaje) {

        String destino = viaje.getString("destino");

        if (destino == null || destino.trim().isEmpty()) {
            destino = "Viaje sin destino";
        }

        Long valor = viaje.getLong("presupuestoTotal");
        long presupuesto = valor == null ? 0L : valor;

        MaterialCardView tarjeta = new MaterialCardView(this);
        tarjeta.setRadius(dp(18));
        tarjeta.setCardBackgroundColor(Color.WHITE);
        tarjeta.setStrokeColor(Color.parseColor("#93C5FD"));
        tarjeta.setStrokeWidth(dp(2));
        tarjeta.setCardElevation(dp(1));

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, dp(14));
        tarjeta.setLayoutParams(parametros);

        LinearLayout contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(
                dp(16), dp(16), dp(16), dp(16)
        );

        // ENCABEZADO DESPLEGABLE
        LinearLayout encabezado = new LinearLayout(this);
        encabezado.setOrientation(LinearLayout.HORIZONTAL);
        encabezado.setGravity(Gravity.CENTER_VERTICAL);
        encabezado.setMinimumHeight(dp(48));

        TextView titulo = crearTexto(destino, 18, true, false);
        titulo.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        TextView flecha = crearTexto("⌄", 24, true, false);
        flecha.setGravity(Gravity.CENTER);
        flecha.setMinWidth(dp(44));
        flecha.setMinHeight(dp(48));

        encabezado.addView(titulo);
        encabezado.addView(flecha);

        // RESUMEN
        TextView resumen = crearTexto(
                "Presupuesto: " + pesos(presupuesto)
                        + "\nCargando gastos...",
                14,
                false,
                true
        );

        resumen.setPadding(0, dp(10), 0, dp(10));

        // DETALLE OCULTO AL INICIO
        LinearLayout detalle = new LinearLayout(this);
        detalle.setOrientation(LinearLayout.VERTICAL);
        detalle.setVisibility(View.GONE);

        TextView tituloDetalle = crearTexto(
                "Detalle de gastos",
                16,
                true,
                false
        );

        tituloDetalle.setPadding(0, dp(12), 0, dp(8));

        TextView estadoDetalle = crearTexto(
                "Cargando gastos...",
                13,
                false,
                true
        );

        detalle.addView(tituloDetalle);
        detalle.addView(estadoDetalle);

        contenido.addView(encabezado);
        contenido.addView(resumen);
        contenido.addView(detalle);

        tarjeta.addView(contenido);
        contenedor.addView(tarjeta);

        // ABRIR O CERRAR EL DETALLE
        encabezado.setClickable(true);
        encabezado.setFocusable(true);
        encabezado.setContentDescription(
                "Mostrar u ocultar gastos de " + destino
        );

        encabezado.setOnClickListener(v -> {

            boolean abrir =
                    detalle.getVisibility() != View.VISIBLE;

            detalle.setVisibility(
                    abrir ? View.VISIBLE : View.GONE
            );

            flecha.setText(abrir ? "⌃" : "⌄");
        });

        guardarOriginales(tarjeta);

        // LEER LOS GASTOS DEL VIAJE
        viaje.getReference()
                .collection("gastos")
                .get()
                .addOnSuccessListener(resultado -> {

                    if (isFinishing() || isDestroyed()) return;

                    List<DocumentSnapshot> gastos =
                            new ArrayList<>(resultado.getDocuments());

                    gastos.sort((a, b) -> {
                        java.util.Date fechaA = a.getDate("fecha");
                        java.util.Date fechaB = b.getDate("fecha");

                        if (fechaA == null && fechaB == null) return 0;
                        if (fechaA == null) return 1;
                        if (fechaB == null) return -1;

                        return fechaB.compareTo(fechaA);
                    });

                    long gastado = 0L;

                    for (DocumentSnapshot gasto : gastos) {
                        Long monto = gasto.getLong("monto");

                        if (monto != null && monto > 0) {
                            try {
                                gastado = Math.addExact(gastado, monto);
                            } catch (ArithmeticException e) {
                                resumen.setText(
                                        "El total de gastos es demasiado grande."
                                );
                                estadoDetalle.setText(
                                        "No se pudo calcular el total."
                                );
                                return;
                            }
                        }
                    }

                    long disponible;

                    try {
                        disponible = Math.subtractExact(
                                presupuesto, gastado
                        );
                    } catch (ArithmeticException e) {
                        resumen.setText(
                                "No se pudo calcular el saldo."
                        );
                        estadoDetalle.setText(
                                "No se pudo calcular el saldo."
                        );
                        return;
                    }

                    resumen.setText(
                            "Presupuesto: " + pesos(presupuesto)
                                    + "\nGastado: " + pesos(gastado)
                                    + "\nDisponible: " + pesos(disponible)
                    );

                    detalle.removeAllViews();
                    detalle.addView(tituloDetalle);

                    if (gastos.isEmpty()) {

                        TextView vacio = crearTexto(
                                "Todavía no registraste gastos "
                                        + "para este viaje.",
                                13,
                                false,
                                true
                        );

                        detalle.addView(vacio);

                    } else {

                        for (DocumentSnapshot gasto : gastos) {
                            crearDetalleGasto(detalle, gasto);
                        }
                    }

                    guardarOriginales(tarjeta);
                    actualizarVista();

                })
                .addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) return;

                    estadoDetalle.setText(
                            "No se pudieron cargar los gastos "
                                    + "de este viaje."
                    );
                });
    }

    // ==========================================
    // DETALLE DE UN GASTO INDIVIDUAL
    // ==========================================

    private void crearDetalleGasto(
            LinearLayout contenedorDetalle,
            DocumentSnapshot gasto) {

        String concepto = gasto.getString("concepto");
        String categoria = gasto.getString("categoria");

        if (concepto == null || concepto.isEmpty()) {
            concepto = "Gasto";
        }

        if (categoria == null || categoria.isEmpty()) {
            categoria = "Sin categoría";
        }

        Long monto = gasto.getLong("monto");
        boolean imprevisto = Boolean.TRUE.equals(
                gasto.getBoolean("imprevisto")
        );

        java.util.Date fecha = gasto.getDate("fecha");

        String fechaTexto = "Fecha no disponible";

        if (fecha != null) {
            fechaTexto = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    new Locale("es", "AR")
            ).format(fecha);
        }

        LinearLayout tarjetaGasto = new LinearLayout(this);
        tarjetaGasto.setTag("detalleGastoHistorial");
        tarjetaGasto.setOrientation(LinearLayout.VERTICAL);
        tarjetaGasto.setPadding(
                dp(12), dp(12), dp(12), dp(12)
        );

        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(Color.parseColor("#EFF6FF"));
        fondo.setCornerRadius(dp(12));
        fondo.setStroke(dp(1), Color.parseColor("#BFDBFE"));

        tarjetaGasto.setBackground(fondo);

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, dp(9));
        tarjetaGasto.setLayoutParams(parametros);

        TextView titulo = crearTexto(
                concepto,
                15,
                true,
                false
        );

        TextView importe = crearTexto(
                "− " + pesos(monto == null ? 0 : monto),
                16,
                true,
                false
        );

        String descripcion = categoria + " · " + fechaTexto;

        if (imprevisto) {
            descripcion += "\nGasto imprevisto";
        }

        TextView informacion = crearTexto(
                descripcion,
                12,
                false,
                true
        );

        tarjetaGasto.addView(titulo);
        tarjetaGasto.addView(importe);
        tarjetaGasto.addView(informacion);

        contenedorDetalle.addView(tarjetaGasto);
    }

    // ==========================================
    // CREAR TEXTOS CON ESTILO VIAJAPP
    // ==========================================

    private TextView crearTexto(
            String texto,
            int tamano,
            boolean negrita,
            boolean secundario) {

        TextView vista = new TextView(this);

        vista.setText(texto);
        vista.setTextSize(tamano);

        vista.setTextColor(Color.parseColor(
                secundario ? "#475569" : "#0C2340"
        ));

        if (negrita) {
            vista.setTypeface(null, Typeface.BOLD);
        }

        vista.setTag(secundario ? "secundario" : "principal");

        return vista;
    }

    // ==========================================
    // ACCESIBILIDAD
    // ==========================================

    private void configurarAccesibilidad() {

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        btnAccesibilidad.setOnClickListener(v -> {

            controlesVisibles = !controlesVisibles;

            int visibilidad =
                    controlesVisibles ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams)
                            btnAccesibilidad.getLayoutParams();

            params.width = controlesVisibles
                    ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;

            params.weight = controlesVisibles ? 1f : 0f;

            btnAccesibilidad.setLayoutParams(params);

            ConstraintLayout.LayoutParams barraParams =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            barraParams.width = controlesVisibles
                    ? 0 : ViewGroup.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(barraParams);

            btnAccesibilidad.setContentDescription(
                    controlesVisibles
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        btnAumentarTexto.setOnClickListener(v -> {
            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            PreferenciasAccesibilidad.guardarEscalaTexto(
                    this, escalaTexto
            );

            actualizarTamanoTextos(pantallaPrincipal);
        });

        btnDisminuirTexto.setOnClickListener(v -> {
            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            PreferenciasAccesibilidad.guardarEscalaTexto(
                    this, escalaTexto
            );

            actualizarTamanoTextos(pantallaPrincipal);
        });

        btnContraste.setOnClickListener(v -> {
            modoContraste = (modoContraste + 1) % 3;

            PreferenciasAccesibilidad.guardarContraste(
                    this, modoContraste
            );

            aplicarTemaContraste();
        });

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
    }

    // ==========================================
    // RECORDAR COLORES Y TAMAÑOS ORIGINALES
    // ==========================================

    private void guardarOriginales(View vista) {

        if (vista == null) return;

        if (!fondosOriginales.containsKey(vista)) {
            Drawable fondo = vista.getBackground();

            fondosOriginales.put(
                    vista,
                    fondo == null ? null : copiarFondo(fondo)
            );
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            if (!tamanosOriginales.containsKey(texto)) {
                tamanosOriginales.put(texto, texto.getTextSize());
                coloresOriginales.put(
                        texto, texto.getCurrentTextColor()
                );
            }
        }

        if (vista instanceof MaterialCardView) {

            MaterialCardView tarjeta = (MaterialCardView) vista;

            if (!tarjetasOriginales.containsKey(tarjeta)) {
                tarjetasOriginales.put(
                        tarjeta,
                        new int[]{
                                tarjeta.getCardBackgroundColor()
                                        .getDefaultColor(),
                                tarjeta.getStrokeColor(),
                                tarjeta.getStrokeWidth()
                        }
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

    private Drawable copiarFondo(Drawable original) {
        if (original == null) return null;

        if (original.getConstantState() != null) {
            return original.getConstantState()
                    .newDrawable()
                    .mutate();
        }

        return original.mutate();
    }

    // ==========================================
    // APLICAR TAMAÑO DE TEXTO
    // ==========================================

    private void actualizarTamanoTextos(View vista) {

        if (vista == null) return;

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

    // ==========================================
    // MODOS NORMAL, CREMA Y OSCURO
    // ==========================================

    private void aplicarTemaContraste() {

        boolean normal = modoContraste == 0;
        boolean crema = modoContraste == 1;

        int fondo = Color.parseColor(
                normal ? "#F8FAFC"
                        : crema ? "#FFF1D6" : "#111111"
        );

        int fondoTarjeta = Color.parseColor(
                normal ? "#FFFFFF"
                        : crema ? "#EBD6B4" : "#222222"
        );

        int borde = Color.parseColor(
                normal ? "#93C5FD"
                        : crema ? "#96734D" : "#777777"
        );

        int principal = Color.parseColor(
                crema ? "#222222"
                        : normal ? "#0C2340" : "#FFFFFF"
        );

        int secundario = Color.parseColor(
                crema ? "#5A5146"
                        : normal ? "#475569" : "#D0D0D0"
        );

        // Restaurar fondos originales primero
        restaurarFondos(pantallaPrincipal);

        // Adaptar las tarjetas individuales de gastos
        actualizarFondoDetalles(pantallaPrincipal);

        for (MaterialCardView tarjeta : tarjetasOriginales.keySet()) {

            int[] original = tarjetasOriginales.get(tarjeta);

            tarjeta.setCardBackgroundColor(
                    normal ? original[0] : fondoTarjeta
            );

            tarjeta.setStrokeColor(
                    normal ? original[1] : borde
            );

            tarjeta.setStrokeWidth(
                    normal ? original[2] : dp(2)
            );
        }

        for (TextView texto : coloresOriginales.keySet()) {

            int original = coloresOriginales.get(texto);

            if (normal) {
                texto.setTextColor(original);
            } else {
                texto.setTextColor(
                        "secundario".equals(texto.getTag())
                                ? secundario : principal
                );
            }
        }

        pantallaPrincipal.setBackgroundColor(fondo);

        findViewById(R.id.scrollContenido)
                .setBackgroundColor(fondo);

        findViewById(R.id.header)
                .setBackgroundColor(
                        normal ? Color.parseColor("#FAF8FF")
                                : fondoTarjeta
                );

        View informativa =
                findViewById(R.id.tarjetaInformativaHistorial);

        if (!normal) {
            informativa.setBackground(
                    fondoRedondeado(fondoTarjeta, borde, 16)
            );
        }

        if (!normal) {

            barraAccesibilidad.setBackground(
                    fondoRedondeado(fondoTarjeta, borde, 16)
            );

            TextView[] botones = {
                    btnAccesibilidad,
                    btnDisminuirTexto,
                    btnAumentarTexto,
                    btnContraste,
                    btnAudio
            };

            for (TextView boton : botones) {
                boton.setBackground(
                        fondoRedondeado(fondoTarjeta, borde, 12)
                );
            }
        }

        btnContraste.setText(
                normal ? "◐ Contraste"
                        : crema ? "◐ Crema" : "◐ Oscuro"
        );

        ImageView imagenPerfil =
                findViewById(R.id.btnAbrirPerfil) != null
                        ? ((ViewGroup) findViewById(
                        R.id.btnAbrirPerfil
                )).getChildAt(0) instanceof ImageView
                          ? (ImageView) ((ViewGroup) findViewById(
                        R.id.btnAbrirPerfil
                )).getChildAt(0)
                          : null
                        : null;

        if (imagenPerfil != null) {
            imagenPerfil.setImageTintList(
                    ColorStateList.valueOf(
                            normal ? Color.parseColor("#1D4ED8")
                                    : principal
                    )
            );
        }
    }

    private void restaurarFondos(View vista) {

        if (vista == null) return;

        if (!(vista instanceof MaterialCardView)
                && fondosOriginales.containsKey(vista)) {

            vista.setBackground(
                    copiarFondo(fondosOriginales.get(vista))
            );
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                restaurarFondos(grupo.getChildAt(i));
            }
        }
    }

    // ==========================================
    // CONTRASTE EN LOS DETALLES DE GASTOS
    // ==========================================

    private void actualizarFondoDetalles(View vista) {

        if (vista == null) return;

        if ("detalleGastoHistorial".equals(vista.getTag())) {

            int colorFondo;
            int colorBorde;

            if (modoContraste == 1) {

                // MODO CREMA
                colorFondo = Color.parseColor("#F2E2BE");
                colorBorde = Color.parseColor("#A97C50");

            } else if (modoContraste == 2) {

                // MODO OSCURO
                colorFondo = Color.parseColor("#30343A");
                colorBorde = Color.parseColor("#777777");

            } else {

                // MODO NORMAL: CELESTE UN POCO MÁS MARCADO
                colorFondo = Color.parseColor("#E4F0FF");
                colorBorde = Color.parseColor("#A8C9FF");
            }

            vista.setBackground(
                    fondoRedondeado(
                            colorFondo,
                            colorBorde,
                            12
                    )
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarFondoDetalles(
                        grupo.getChildAt(i)
                );
            }
        }
    }

    private GradientDrawable fondoRedondeado(
            int color, int borde, int radio) {

        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(color);
        fondo.setCornerRadius(dp(radio));
        fondo.setStroke(dp(2), borde);

        return fondo;
    }

    // ==========================================
    // FORMATO DE PESOS
    // ==========================================

    private String pesos(long monto) {

        NumberFormat formato =
                NumberFormat.getNumberInstance(
                        new Locale("es", "AR")
                );

        formato.setMaximumFractionDigits(0);

        return "$" + formato.format(monto);
    }

    private int dp(int valor) {

        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }

    private void actualizarVista() {
        guardarOriginales(pantallaPrincipal);
        aplicarTemaContraste();
        actualizarTamanoTextos(pantallaPrincipal);
    }
}
