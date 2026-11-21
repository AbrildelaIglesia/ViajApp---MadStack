package com.example.viajapp_madstack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.CheckBox;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.text.DateFormat;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest;
import java.util.Arrays;
import java.util.List;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.graphics.Typeface;

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

        // Guardar estilos originales antes de construir las tarjetas dinámicas.
        configurarAccesibilidad();
        mostrarItinerarioRecibido(savedInstanceState);

        configurarClima();
        if (diasGenerados != null && ConexionUtils.hayInternet(this)) consultarClimaDestino();

        // ===================================================
        // MENÚ DESPLEGABLE DEL PERFIL DE USUARIO
        // ===================================================

        View btnAbrirPerfil = findViewById(R.id.btnAbrirPerfil);

        btnAbrirPerfil.setOnClickListener(v -> {

            MenuPerfilUsuario.mostrar(
                    ItinerarioActivity.this,
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
                // Ya estamos en esta pantalla; conservar el itinerario visible.
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

    @Override
    protected void onResume() {
        super.onResume();

        modoContraste =
                PreferenciasAccesibilidad.obtenerContraste(this);

        escalaTexto =
                PreferenciasAccesibilidad.obtenerEscalaTexto(this);

        aplicarTemaContraste();

        actualizarTamanoTextos(scrollContenido);
        actualizarTamanoTextos(header);
        actualizarTamanoTextos(bottomNav);
        actualizarTamanoTextos(barraAccesibilidad);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (MenuSeguridad.manejarResultadoDictado(requestCode, resultCode, data)
                || MenuEditarPerfil.manejarResultadoDictado(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private final Map<String, Set<String>> actividadesDescartadas = new HashMap<>();
    private boolean reemplazoEnCurso = false;
    private GeminiService geminiService;

    private String itinerarioIdActual;

    private String textoItinerarioActual;
    private String destinoActual;
    private int diasActuales;
    private JSONArray diasGenerados;
    private LinearLayout barraDiasGenerados, tarjetasActividades;
    private int diaSeleccionado = 0;
    private PlacesClient clienteFotos;
    private final ExecutorService ejecutorClima = Executors.newSingleThreadExecutor();
    private TextView tvClimaTitulo, tvClimaDetalle;
    private View tarjetaClima;
    private TextView iconoClima;
    private String ultimoDestinoClima = null;
    private LinearLayout listaPreparativos;
    private TextView tvProgresoPreparativos;
    private static final String[] PREPARATIVOS_BASE = {
            "Agua para hidratarse", "Documento y medio de pago", "Celular cargado",
            "Calzado cómodo", "Medicación personal (si corresponde)",
            "Elementos de apoyo o accesibilidad (si los necesitás)"
    };
    private final List<String> preparativosActuales = new ArrayList<>();
    private Integer codigoClimaActual = null;
    private Double temperaturaClimaActual = null;
    private Double vientoClimaActual = null;
    private String destinoChecklistClima = null;


    private void mostrarItinerarioRecibido(Bundle estado) {
        if (estado != null && estado.containsKey("itinerario")) {
            itinerarioIdActual = estado.getString("itinerarioId");
            textoItinerarioActual = estado.getString("itinerario");
            destinoActual = estado.getString("destino");
            diasActuales = estado.getInt("dias", 0);
            diaSeleccionado = estado.getInt("diaSeleccionado", 0);
        } else if (textoItinerarioActual == null || textoItinerarioActual.trim().isEmpty()) {
            Intent datos = getIntent();
            itinerarioIdActual = datos.getStringExtra("itinerarioId");
            textoItinerarioActual = datos.getStringExtra("itinerario");
            destinoActual = datos.getStringExtra("destino");
            diasActuales = datos.getIntExtra("dias", 0);
        }

        LinearLayout contenidoGenerado = findViewById(R.id.contenidoGenerado);
        LinearLayout contenidoEjemplo = findViewById(R.id.contenidoEjemplo);
        if (textoItinerarioActual == null || textoItinerarioActual.trim().isEmpty()) {
            contenidoEjemplo.setVisibility(View.GONE);
            contenidoGenerado.setVisibility(View.VISIBLE);
            TextView tituloVacio = findViewById(R.id.tvTituloItinerarioGenerado);
            tituloVacio.setText("Cargando tus itinerarios guardados...");
            if (itinerarioIdActual != null && !itinerarioIdActual.isEmpty()) {
                cargarItinerarioPorId(itinerarioIdActual);
            } else {
                cargarUltimoItinerario();
            }
            return;
        }

        try {
            JSONObject raiz = new JSONObject(textoItinerarioActual);
            diasGenerados = raiz.getJSONArray("dias");
            if (diasGenerados.length() == 0) throw new JSONException("Sin días");
            for (int i = 0; i < diasGenerados.length(); i++) {
                JSONObject dia = diasGenerados.getJSONObject(i);
                if (dia.getInt("dia") != i + 1 || dia.getJSONArray("actividades").length() != 3)
                    throw new JSONException("El día no contiene exactamente 3 actividades");
            }
        } catch (JSONException error) {
            Toast.makeText(this, "El itinerario no tiene el formato esperado", Toast.LENGTH_LONG).show();
            contenidoEjemplo.setVisibility(View.GONE);
            contenidoGenerado.setVisibility(View.VISIBLE);
            TextView tituloError = findViewById(R.id.tvTituloItinerarioGenerado);
            tituloError.setText("No se pudo mostrar el itinerario. Generá uno nuevo desde Planear.");
            return;
        }

        contenidoEjemplo.setVisibility(View.GONE);
        contenidoGenerado.setVisibility(View.VISIBLE);
        String nombre = destinoActual == null || destinoActual.trim().isEmpty() ? "tu viaje" : destinoActual;
        TextView titulo = findViewById(R.id.tvTituloItinerarioGenerado);
        titulo.setText("Itinerario de " + nombre + " - " + diasGenerados.length() + " días");
        barraDiasGenerados = findViewById(R.id.barraDiasGenerados);
        tarjetasActividades = findViewById(R.id.tarjetasActividades);
        mostrarDia(Math.max(0, Math.min(diaSeleccionado, diasGenerados.length() - 1)));
        if (tarjetaClima != null && ConexionUtils.hayInternet(this)) consultarClimaDestino();
    }
    private void cargarUltimoItinerario() {
        if (!ConexionUtils.hayInternet(this)) {
            java.util.List<JSONObject> locales = ItinerariosLocales.listar(this);
            if (locales.isEmpty()) {
                mostrarMensajeItinerario("No hay itinerarios guardados en este dispositivo.");
            } else {
                mostrarItinerarioLocal(locales.get(0));
            }
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mostrarMensajeItinerario("Iniciá sesión para consultar tus itinerarios guardados.");
            return;
        }
        FirebaseFirestore.getInstance().collection("usuarios").document(usuario.getUid())
                .collection("itinerarios")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING).limit(1)
                .get().addOnSuccessListener(resultado -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (resultado.isEmpty()) {
                        mostrarMensajeItinerario("Todavía no tenés itinerarios guardados. Creá uno desde Planear.");
                    } else {
                        mostrarDocumentoGuardado(resultado.getDocuments().get(0));
                    }
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    mostrarMensajeItinerario("No se pudieron cargar tus viajes. Revisá tu conexión y permisos.");
                });
    }

    private void cargarItinerarioPorId(String id) {
        if (!ConexionUtils.hayInternet(this)) {
            JSONObject local = ItinerariosLocales.buscar(this, id);
            if (local != null) mostrarItinerarioLocal(local);
            else mostrarMensajeItinerario("Este itinerario todavía no está guardado offline.");
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mostrarMensajeItinerario("Iniciá sesión para consultar tu itinerario.");
            return;
        }
        FirebaseFirestore.getInstance().collection("usuarios").document(usuario.getUid())
                .collection("itinerarios").document(id).get()
                .addOnSuccessListener(documento -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (documento.exists()) mostrarDocumentoGuardado(documento);
                    else mostrarMensajeItinerario("No se encontró el itinerario guardado.");
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    mostrarMensajeItinerario("No se pudo recuperar el itinerario.");
                });
    }

    private void mostrarDocumentoGuardado(DocumentSnapshot documento) {
        String json = documento.getString("itinerarioJson");
        if (json == null || json.trim().isEmpty()) {
            mostrarMensajeItinerario("El itinerario guardado no tiene actividades disponibles.");
            return;
        }
        Long diasGuardados = documento.getLong("cantidadDias");
        ItinerariosLocales.guardar(this, documento.getId(), documento.getString("destino"),
                diasGuardados == null ? 0 : diasGuardados.intValue(), json);
        itinerarioIdActual = documento.getId();
        textoItinerarioActual = json;
        destinoActual = documento.getString("destino");
        Long cantidad = documento.getLong("cantidadDias");
        diasActuales = cantidad == null ? 0 : cantidad.intValue();
        diaSeleccionado = 0;
        mostrarItinerarioRecibido(null);
    }

    private void mostrarItinerarioLocal(JSONObject local) {
        itinerarioIdActual = local.optString("id", "");
        textoItinerarioActual = local.optString("json", "");
        destinoActual = local.optString("destino", "");
        diasActuales = local.optInt("dias", 0);
        diaSeleccionado = 0;
        mostrarItinerarioRecibido(null);
    }

    private void mostrarMensajeItinerario(String mensaje) {
        findViewById(R.id.contenidoEjemplo).setVisibility(View.GONE);
        findViewById(R.id.contenidoGenerado).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvTituloItinerarioGenerado)).setText(mensaje);
    }

    private void mostrarListaItinerarios() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Iniciá sesión para consultar tus viajes", Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseFirestore.getInstance().collection("usuarios").document(usuario.getUid())
                .collection("itinerarios")
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .limit(50).get()
                .addOnSuccessListener(resultado -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (resultado.isEmpty()) {
                        new AlertDialog.Builder(this).setTitle("Mis itinerarios")
                                .setMessage("Todavía no tenés viajes guardados.")
                                .setPositiveButton("Aceptar", null).show();
                        return;
                    }
                    List<DocumentSnapshot> documentos = resultado.getDocuments();
                    String[] titulos = new String[documentos.size()];
                    for (int i = 0; i < documentos.size(); i++) {
                        DocumentSnapshot documento = documentos.get(i);
                        String destino = documento.getString("destino");
                        Long dias = documento.getLong("cantidadDias");
                        String fecha = documento.getDate("fechaCreacion") == null ? "" :
                                " - " + DateFormat.getDateInstance(DateFormat.SHORT)
                                        .format(documento.getDate("fechaCreacion"));
                        titulos[i] = (destino == null ? "Viaje" : destino) + " - "
                                + (dias == null ? "?" : dias) + " días" + fecha;
                    }
                    new AlertDialog.Builder(this).setTitle("Mis itinerarios guardados")
                            .setItems(titulos, (dialogo, posicion) ->
                                    mostrarDocumentoGuardado(documentos.get(posicion)))
                            .setNegativeButton("Cerrar", null).show();
                }).addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "No se pudieron cargar tus itinerarios: "
                            + error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDia(int indice) {
        diaSeleccionado = indice;
        mostrarChecklistPreparativos();
        barraDiasGenerados.removeAllViews();
        tarjetasActividades.removeAllViews();
        for (int i = 0; i < diasGenerados.length(); i++) {
            final int posicion = i;
            TextView boton = new TextView(this);
            boton.setText("DÍA\n" + (i + 1));
            boton.setGravity(Gravity.CENTER);
            boton.setTextSize(14);
            boton.setTypeface(null, Typeface.BOLD);
            boton.setTextColor(i == indice ? Color.WHITE : Color.parseColor("#475569"));
            boton.setBackgroundColor(i == indice ? Color.parseColor("#F43F5E") : Color.parseColor("#F1F5F9"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(60), dp(60));
            lp.setMargins(0, 0, dp(10), 0);
            boton.setLayoutParams(lp);
            boton.setContentDescription("Mostrar actividades del día " + (i + 1));
            boton.setOnClickListener(v -> mostrarDia(posicion));
            barraDiasGenerados.addView(boton);
        }
        try {
            JSONArray actividades = diasGenerados.getJSONObject(indice).getJSONArray("actividades");
            for (int i = 0; i < 3; i++) {
                JSONObject actividad = actividades.getJSONObject(i);
                LinearLayout tarjeta = new LinearLayout(this);
                tarjeta.setOrientation(LinearLayout.VERTICAL);
                tarjeta.setPadding(dp(16), dp(16), dp(16), dp(16));
                tarjeta.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.topMargin = dp(14);
                tarjeta.setLayoutParams(params);
                String nombreLugar = actividad.getString("nombre");
                agregarTexto(tarjeta, actividad.getString("hora") + " - Actividad " + (i + 1), 13, "#2563EB", true);
                ImageView foto = new ImageView(this);
                foto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                foto.setBackgroundColor(Color.parseColor("#EFF6FF"));
                LinearLayout.LayoutParams fotoParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
                fotoParams.topMargin = dp(10);
                fotoParams.bottomMargin = dp(10);
                tarjeta.addView(foto, fotoParams);
                foto.setContentDescription("Fotografía pendiente de " + nombreLugar);
                cargarFotoActividad(foto, nombreLugar);
                agregarTexto(tarjeta, nombreLugar, 18, "#0F172A", true);
                agregarTexto(tarjeta, actividad.getString("descripcion"), 14, "#475569", false);
                agregarTexto(tarjeta, "Accesibilidad: " + actividad.getString("accesibilidad"), 13, "#166534", false);
                Button botonMapa = new Button(this);
                botonMapa.setText("Cómo llegar con Google Maps");
                botonMapa.setAllCaps(false);
                botonMapa.setTextColor(Color.WHITE);
                botonMapa.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                LinearLayout.LayoutParams mapaParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
                mapaParams.topMargin = dp(12);
                tarjeta.addView(botonMapa, mapaParams);
                botonMapa.setOnClickListener(v -> abrirIndicaciones(nombreLugar));

                final int indiceActividad = i;
                Button cambiar = new Button(this);
                cambiar.setText("Cambiar actividad");
                cambiar.setAllCaps(false);
                cambiar.setTextColor(Color.WHITE);
                cambiar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0F766E")));
                cambiar.setContentDescription("Cambiar actividad " + (i + 1) + ": " + nombreLugar);
                LinearLayout.LayoutParams cambiarParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
                cambiarParams.topMargin = dp(8);
                tarjeta.addView(cambiar, cambiarParams);
                cambiar.setEnabled(!reemplazoEnCurso);
                cambiar.setOnClickListener(v -> confirmarCambioActividad(indice, indiceActividad, cambiar));
                tarjetasActividades.addView(tarjeta);
            }
        } catch (JSONException error) {
            Toast.makeText(this, "Error al mostrar las actividades", Toast.LENGTH_SHORT).show();
        }
        agregarBotonRecorrido(indice);
        // Los nuevos TextView se agregan después de guardar los estilos originales.
        // Registrar sus estilos permite usar A+/A- y restaurar el contraste.
        guardarOriginales(tarjetasActividades);
        guardarOriginales(barraDiasGenerados);
        actualizarTamanoTextos(tarjetasActividades);
        actualizarTamanoTextos(barraDiasGenerados);
        if (modoContraste != 0) aplicarTemaContraste();
    }

    // Cambia una sola actividad y conserva el resto del itinerario.
    private void confirmarCambioActividad(int dia, int posicion, Button boton) {
        if (reemplazoEnCurso || diasGenerados == null) return;
        try {
            String nombre = diasGenerados.getJSONObject(dia).getJSONArray("actividades")
                    .getJSONObject(posicion).optString("nombre", "esta actividad");
            new AlertDialog.Builder(this)
                    .setTitle("Cambiar actividad")
                    .setMessage("¿Querés reemplazar " + nombre + " por otra sugerencia de Gemini?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Buscar alternativa", (dialogo, cual) ->
                            cambiarActividadConGemini(dia, posicion, boton))
                    .show();
        } catch (JSONException e) {
            Toast.makeText(this, "No se pudo seleccionar la actividad", Toast.LENGTH_SHORT).show();
        }
    }

    private void cambiarActividadConGemini(int dia, int posicion, Button boton) {
        if (!ConexionUtils.hayInternet(this)) {
            Toast.makeText(this, "Para cambiar actividades necesitás Internet.", Toast.LENGTH_LONG).show();
            return;
        }
        if (reemplazoEnCurso || diasGenerados == null) return;
        final String idItinerario = itinerarioIdActual;
        final String destino = destinoActual == null ? "" : destinoActual;
        final String clave = dia + "_" + posicion;
        final JSONObject original;
        final JSONArray actividades;
        try {
            actividades = diasGenerados.getJSONObject(dia).getJSONArray("actividades");
            original = actividades.getJSONObject(posicion);
        } catch (JSONException error) {
            Toast.makeText(this, "No se pudo leer la actividad", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> rechazadas = actividadesDescartadas.computeIfAbsent(clave, k -> new HashSet<>());
        Set<String> evitar = new HashSet<>(rechazadas);
        evitar.add(original.optString("nombre", ""));
        for (int i = 0; i < diasGenerados.length(); i++) {
            JSONArray lista = diasGenerados.optJSONObject(i).optJSONArray("actividades");
            if (lista == null) continue;
            for (int j = 0; j < lista.length(); j++) {
                JSONObject actividad = lista.optJSONObject(j);
                if (actividad != null) evitar.add(actividad.optString("nombre", ""));
            }
        }
        String instruccion = "Sos un asistente de turismo accesible en Argentina. "
                + "Proponé UNA actividad turística REAL y diferente para el destino: " + destino + ". "
                + "Día " + (dia + 1) + ", horario previsto: " + original.optString("hora") + ". "
                + "Actividad rechazada: " + original.optString("nombre") + ". "
                + "No repitas ninguno de estos lugares: " + evitar + ". "
                + "Conservá el horario, ofrecé una descripción breve y detalles de accesibilidad "
                + "sin afirmar que una instalación está verificada si no tenés evidencia. "
                + "Respondé SOLO un objeto JSON válido, sin markdown ni explicaciones, "
                + "con exactamente estas claves string: nombre, hora, descripcion, accesibilidad. "
                + "En hora usá exactamente: " + original.optString("hora") + ".";

        reemplazoEnCurso = true;
        boton.setText("Buscando alternativa...");
        boton.setEnabled(false);
        if (geminiService == null) geminiService = new GeminiService(this);
        geminiService.generarTexto(instruccion, new GeminiService.GeminiCallback() {
            @Override public void onSuccess(String respuesta) {
                if (isFinishing() || isDestroyed()) return;
                try {
                    String limpio = respuesta.trim();
                    if (limpio.startsWith("```")) {
                        limpio = limpio.replaceFirst("^```(?:json)?\\s*", "")
                                .replaceFirst("\\s*```$", "").trim();
                    }
                    JSONObject propuesta = new JSONObject(limpio);
                    String nombre = propuesta.optString("nombre", "").trim();
                    String descripcion = propuesta.optString("descripcion", "").trim();
                    String accesibilidad = propuesta.optString("accesibilidad", "").trim();
                    if (nombre.isEmpty() || descripcion.isEmpty() || accesibilidad.isEmpty())
                        throw new JSONException("Faltan datos en la propuesta");
                    for (String prohibida : evitar) {
                        if (nombre.equalsIgnoreCase(prohibida.trim()))
                            throw new JSONException("Gemini repitió una actividad existente o descartada");
                    }
                    JSONObject nueva = new JSONObject();
                    nueva.put("nombre", nombre);
                    nueva.put("hora", original.optString("hora"));
                    nueva.put("descripcion", descripcion);
                    nueva.put("accesibilidad", accesibilidad);

                    // Copia independiente: no modificar la pantalla si falla Firestore.
                    JSONObject copia = new JSONObject(textoItinerarioActual);
                    copia.getJSONArray("dias").getJSONObject(dia)
                            .getJSONArray("actividades").put(posicion, nueva);
                    String jsonActualizado = copia.toString();
                    if (idItinerario != null && !idItinerario.isEmpty()) {
                        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
                        if (usuario == null) {
                            finalizarCambio(boton, "Iniciá sesión para guardar el cambio.");
                            return;
                        }
                        FirebaseFirestore.getInstance().collection("usuarios")
                                .document(usuario.getUid()).collection("itinerarios")
                                .document(idItinerario).update("itinerarioJson", jsonActualizado)
                                .addOnSuccessListener(v -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    ItinerariosLocales.guardar(
                                            ItinerarioActivity.this,
                                            idItinerario,
                                            destinoActual,
                                            diasActuales,
                                            textoItinerarioActual
                                    );
                                    aplicarActividadReemplazada(jsonActualizado, dia, original.optString("nombre"), clave, boton);
                                })
                                .addOnFailureListener(error -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    finalizarCambio(boton, "No se pudo guardar el cambio en Firebase.");
                                });
                    } else {
                        aplicarActividadReemplazada(jsonActualizado, dia, original.optString("nombre"), clave, boton);
                    }
                } catch (Exception error) {
                    finalizarCambio(boton, "Gemini no devolvió una actividad válida o diferente. Intentá otra vez.");
                }
            }
            @Override public void onError(String error) {
                if (isFinishing() || isDestroyed()) return;
                finalizarCambio(boton, "No se pudo consultar Gemini. Revisá tu conexión.");
            }
        });
    }

    private void aplicarActividadReemplazada(String json, int dia, String descartada,
                                             String clave, Button boton) {
        try {
            textoItinerarioActual = json;
            diasGenerados = new JSONObject(json).getJSONArray("dias");
            actividadesDescartadas.computeIfAbsent(clave, k -> new HashSet<>()).add(descartada);
            reemplazoEnCurso = false;
            mostrarDia(dia);
            Toast.makeText(this, "Actividad reemplazada correctamente", Toast.LENGTH_SHORT).show();
        } catch (JSONException error) {
            finalizarCambio(boton, "No se pudo mostrar la actividad nueva.");
        }
    }

    private void finalizarCambio(Button boton, String mensaje) {
        reemplazoEnCurso = false;
        boton.setText("Cambiar actividad con Gemini");
        boton.setEnabled(true);
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
    }

    // Preparativos básicos y adicionales según el clima actual consultado.
    private void mostrarChecklistPreparativos() {
        listaPreparativos = findViewById(R.id.listaPreparativos);
        tvProgresoPreparativos = findViewById(R.id.tvProgresoPreparativos);
        if (listaPreparativos == null || tvProgresoPreparativos == null) return;

        preparativosActuales.clear();
        preparativosActuales.addAll(Arrays.asList(PREPARATIVOS_BASE));
        boolean climaValido = codigoClimaActual != null
                && destinoActual != null && destinoActual.equals(destinoChecklistClima);
        if (climaValido) {
            int codigo = codigoClimaActual;
            double temperatura = temperaturaClimaActual;
            double viento = vientoClimaActual;
            boolean tormenta = codigo >= 95;
            boolean nieve = codigo >= 71 && codigo <= 77 || codigo == 85 || codigo == 86;
            boolean lluvia = codigo >= 51 && codigo <= 67 || codigo >= 80 && codigo <= 82;
            if (temperatura >= 28) {
                agregarPreparativo("Agua extra para el calor");
                agregarPreparativo("Protector solar");
                agregarPreparativo("Gorra o sombrero");
                agregarPreparativo("Anteojos de sol");
            } else if (codigo <= 3) {
                agregarPreparativo("Protector solar");
                agregarPreparativo("Gorra o sombrero");
            }
            if (temperatura <= 12 || nieve) {
                agregarPreparativo("Campera de abrigo");
                agregarPreparativo("Guantes y bufanda");
                agregarPreparativo("Ropa en capas");
            }
            if (lluvia || tormenta) {
                agregarPreparativo("Piloto o campera impermeable");
                agregarPreparativo("Calzado antideslizante");
                agregarPreparativo("Protección impermeable para el celular");
            }
            if (nieve) {
                agregarPreparativo("Calzado adecuado para nieve o hielo");
                agregarPreparativo("Verificar hielo y estado de caminos");
            }
            if (viento >= 35) {
                agregarPreparativo("Rompevientos");
                agregarPreparativo("Asegurar objetos sueltos");
                agregarPreparativo("Evitar zonas muy expuestas");
            }
        }

        listaPreparativos.removeAllViews();
        SharedPreferences preferencias = getSharedPreferences("preparativos_itinerario", MODE_PRIVATE);
        String claveBase = claveChecklist();
        for (String elemento : preparativosActuales) {
            // La clave se basa en el texto, no en la posición: el estado no cambia
            // cuando el clima agrega o quita opciones del checklist.
            final String clave = claveBase + "_elemento_" + elemento;
            CheckBox opcion = new CheckBox(this);
            opcion.setText(elemento);
            opcion.setTextSize(14);
            opcion.setTextColor(Color.parseColor("#1E293B"));
            opcion.setPadding(dp(4), dp(5), dp(4), dp(5));
            opcion.setMinHeight(dp(48));
            opcion.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
            opcion.setChecked(preferencias.getBoolean(clave, false));
            opcion.setOnCheckedChangeListener((boton, marcado) -> {
                preferencias.edit().putBoolean(clave, marcado).apply();
                actualizarProgresoPreparativos();
            });
            listaPreparativos.addView(opcion);
        }
        actualizarProgresoPreparativos();
        guardarOriginales(listaPreparativos);
        actualizarTamanoTextos(listaPreparativos);
        if (modoContraste != 0) aplicarTemaContraste();
    }

    private void agregarPreparativo(String elemento) {
        if (!preparativosActuales.contains(elemento)) preparativosActuales.add(elemento);
    }

    private String claveChecklist() {
        String identificador = itinerarioIdActual;
        if (identificador == null || identificador.trim().isEmpty()) {
            identificador = textoItinerarioActual == null ? "sin_itinerario" :
                    Integer.toHexString(textoItinerarioActual.hashCode());
        }
        return identificador + "_dia_" + diaSeleccionado;
    }

    private void actualizarProgresoPreparativos() {
        if (listaPreparativos == null || tvProgresoPreparativos == null) return;
        int preparados = 0;
        for (int i = 0; i < listaPreparativos.getChildCount(); i++) {
            if (((CheckBox) listaPreparativos.getChildAt(i)).isChecked()) preparados++;
        }
        String estado = codigoClimaActual == null || !java.util.Objects.equals(destinoActual, destinoChecklistClima)
                ? " · Extras según clima al consultar" : " · Adaptado al clima actual";
        tvProgresoPreparativos.setText("Día " + (diaSeleccionado + 1) + " · " +
                preparados + " de " + preparativosActuales.size() + " preparados" + estado);
    }

    // Consulta del clima actual usando Open-Meteo (geocoding + current weather).
    private void configurarClima() {
        tvClimaTitulo = findViewById(R.id.tvClimaTitulo);
        tvClimaDetalle = findViewById(R.id.tvClimaDetalle);
        tarjetaClima = findViewById(R.id.tarjetaClima);
        iconoClima = findViewById(R.id.iconoClima);
        tarjetaClima.setOnClickListener(v -> consultarClimaDestino());
    }

    private void consultarClimaDestino() {
        if (tvClimaDetalle == null || tarjetaClima == null) return;
        final String destino = destinoActual == null ? "" : destinoActual.trim();
        if (destino.isEmpty()) {
            tvClimaTitulo.setText("Clima no disponible");
            tvClimaDetalle.setText("No hay un destino seleccionado.");
            return;
        }
        ultimoDestinoClima = destino;
        if (!destino.equals(destinoChecklistClima)) {
            codigoClimaActual = null;
            temperaturaClimaActual = null;
            vientoClimaActual = null;
            destinoChecklistClima = null;
            mostrarChecklistPreparativos();
        }
        tvClimaTitulo.setText("Consultando clima en " + destino + "...");
        tvClimaDetalle.setText("Obteniendo condiciones meteorológicas actuales.");
        tarjetaClima.setEnabled(false);
        ejecutorClima.execute(() -> {
            String mensaje;
            try {
                String localidad = destino.split(",")[0].trim();
                String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(localidad, "UTF-8")
                        + "&count=20&language=es&format=json&countryCode=AR";
                JSONObject geo = new JSONObject(leerUrlClima(geoUrl));
                JSONArray lugares = geo.optJSONArray("results");
                if (lugares == null || lugares.length() == 0) {
                    throw new Exception("No se encontró el destino en el servicio meteorológico");
                }
                // Preferir coincidencia de provincia si el destino viene como "ciudad, provincia".
                JSONObject lugar = lugares.getJSONObject(0);
                String destinoNormalizado = destino.toLowerCase(Locale.ROOT);
                for (int i = 0; i < lugares.length(); i++) {
                    JSONObject candidato = lugares.getJSONObject(i);
                    String provincia = candidato.optString("admin1", "").toLowerCase(Locale.ROOT);
                    if (!provincia.isEmpty() && destinoNormalizado.contains(provincia)) {
                        lugar = candidato;
                        break;
                    }
                }
                double lat = lugar.getDouble("latitude");
                double lon = lugar.getDouble("longitude");
                String pronosticoUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                        + lat + "&longitude=" + lon
                        + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                        + "&timezone=auto";
                JSONObject clima = new JSONObject(leerUrlClima(pronosticoUrl)).getJSONObject("current");
                double temperatura = clima.getDouble("temperature_2m");
                double sensacion = clima.getDouble("apparent_temperature");
                double viento = clima.getDouble("wind_speed_10m");
                int humedad = clima.getInt("relative_humidity_2m");
                int codigo = clima.getInt("weather_code");
                String estado = descripcionClima(codigo);
                String vientoTexto = viento < 2 ? "Sin viento" :
                        String.format(Locale.getDefault(), "Viento %.0f km/h", viento);
                final String titulo = String.format(Locale.getDefault(),
                        "%.0f°C • %s • %s", temperatura, estado, vientoTexto);
                final String detalle = String.format(Locale.getDefault(),
                        "Sensación térmica %.0f°C · Humedad %d%%. %s",
                        sensacion, humedad, recomendacionClima(codigo, viento, temperatura));
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || !destino.equals(ultimoDestinoClima)) return;
                    tvClimaTitulo.setText(titulo);
                    tvClimaDetalle.setText(detalle);
                    iconoClima.setText(iconoParaClima(codigo));
                    codigoClimaActual = codigo;
                    temperaturaClimaActual = temperatura;
                    vientoClimaActual = viento;
                    destinoChecklistClima = destino;
                    mostrarChecklistPreparativos();
                    tarjetaClima.setEnabled(true);
                });
                return;
            } catch (Exception error) {
                mensaje = "No se pudo consultar el clima actual. Revisá tu conexión o intentá nuevamente.";
                android.util.Log.w("VIAJAPP_CLIMA", "Error consultando clima", error);
            }
            final String resultado = mensaje;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !destino.equals(ultimoDestinoClima)) return;
                tvClimaTitulo.setText("Clima no disponible");
                tvClimaDetalle.setText(resultado);
                iconoClima.setText("☁");
                tarjetaClima.setEnabled(true);
            });
        });
    }

    private String iconoParaClima(int codigo) {
        if (codigo == 0) return "☀";
        if (codigo <= 3) return "⛅";
        if (codigo == 45 || codigo == 48) return "☁";
        if (codigo >= 71 && codigo <= 77) return "❄";
        if (codigo >= 95) return "⛈";
        return "☂";
    }

    private String recomendacionClima(int codigo, double viento, double temperatura) {
        if (codigo >= 95) return "Hay tormenta: consultá las alertas antes de salir.";
        if (codigo >= 51 && codigo <= 82) return "Puede haber lluvia o nieve: llevá abrigo y verificá el estado de los caminos.";
        if (viento >= 35) return "Hay viento fuerte: tomá precauciones en espacios abiertos.";
        if (temperatura <= 5) return "Hace frío: llevá ropa de abrigo.";
        if (temperatura >= 32) return "Hace calor: llevá agua y evitá la exposición prolongada al sol.";
        return "Condiciones generales del destino. Verificá la accesibilidad y el estado de los senderos antes de salir.";
    }

    private String leerUrlClima(String direccion) throws Exception {
        HttpURLConnection conexion = (HttpURLConnection) new URL(direccion).openConnection();
        conexion.setConnectTimeout(10000);
        conexion.setReadTimeout(10000);
        conexion.setRequestProperty("Accept", "application/json");
        try {
            if (conexion.getResponseCode() != 200) throw new Exception("HTTP " + conexion.getResponseCode());
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder contenido = new StringBuilder();
                String linea;
                while ((linea = lector.readLine()) != null) contenido.append(linea);
                return contenido.toString();
            }
        } finally {
            conexion.disconnect();
        }
    }

    private String descripcionClima(int codigo) {
        if (codigo == 0) return "Despejado";
        if (codigo <= 3) return "Parcialmente nublado";
        if (codigo == 45 || codigo == 48) return "Niebla";
        if (codigo >= 51 && codigo <= 67) return "Lluvia o llovizna";
        if (codigo >= 71 && codigo <= 77) return "Nieve";
        if (codigo >= 80 && codigo <= 82) return "Chaparrones";
        if (codigo >= 95) return "Tormenta";
        return "Condiciones variables";
    }

    @Override
    protected void onDestroy() {
        ejecutorClima.shutdownNow();
        super.onDestroy();
    }

    private String consultaLugar(String nombre) {
        String destino = destinoActual == null ? "" : destinoActual;
        return nombre + ", " + destino + ", Argentina";
    }

    private void cargarFotoActividad(ImageView imagen, String nombre) {
        if (!ConexionUtils.hayInternet(this)) return;

        // No mostrar fotos de otra localidad como reemplazo.
        try {
            if (!Places.isInitialized()) {
                String clave = BuildConfig.PLACES_API_KEY;
                if (clave == null || clave.trim().isEmpty()) return;
                Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), clave);
            }
            if (clienteFotos == null) clienteFotos = Places.createClient(this);
            List<Place.Field> campos = Arrays.asList(
                    Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.PHOTO_METADATAS);
            SearchByTextRequest solicitud = SearchByTextRequest.builder(consultaLugar(nombre), campos)
                    .setMaxResultCount(1).setRegionCode("AR").build();
            clienteFotos.searchByText(solicitud).addOnSuccessListener(resultado -> {
                if (isFinishing() || isDestroyed() || resultado.getPlaces().isEmpty()) return;
                Place lugar = resultado.getPlaces().get(0);
                List<PhotoMetadata> fotos = lugar.getPhotoMetadatas();
                if (fotos == null || fotos.isEmpty()) return;
                FetchResolvedPhotoUriRequest pedido = FetchResolvedPhotoUriRequest.builder(fotos.get(0))
                        .setMaxWidth(900).build();
                clienteFotos.fetchResolvedPhotoUri(pedido).addOnSuccessListener(foto -> {
                    if (isFinishing() || isDestroyed() || imagen.getParent() == null) return;
                    if (foto.getUri() != null) {
                        Glide.with(ItinerarioActivity.this).load(foto.getUri()).into(imagen);
                    }
                });
            });
        } catch (Exception error) {
            android.util.Log.w("VIAJAPP_FOTOS", "No se pudo buscar la foto de " + nombre, error);
        }
    }

    private void abrirIndicaciones(String lugar) {
        Uri url = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                + Uri.encode(consultaLugar(lugar)) + "&travelmode=walking");
        abrirMaps(url);
    }

    private void agregarBotonRecorrido(int indice) {
        try {
            JSONArray actividades = diasGenerados.getJSONObject(indice).getJSONArray("actividades");
            String[] paradas = new String[3];
            for (int i = 0; i < 3; i++)
                paradas[i] = consultaLugar(actividades.getJSONObject(i).getString("nombre"));
            Button recorrido = new Button(this);
            recorrido.setText("Ver recorrido del día en Google Maps");
            recorrido.setAllCaps(false);
            recorrido.setTextColor(Color.WHITE);
            recorrido.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F43F5E")));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
            params.topMargin = dp(16);
            tarjetasActividades.addView(recorrido, params);
            recorrido.setOnClickListener(v -> {
                Uri url = Uri.parse("https://www.google.com/maps/dir/?api=1&origin="
                        + Uri.encode(paradas[0]) + "&destination=" + Uri.encode(paradas[2])
                        + "&waypoints=" + Uri.encode(paradas[1]) + "&travelmode=walking");
                abrirMaps(url);
            });
        } catch (JSONException error) {
            Toast.makeText(this, "No se pudo preparar el recorrido", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirMaps(Uri url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, url));
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "No hay una aplicación para abrir Google Maps", Toast.LENGTH_LONG).show();
        }
    }

    private void agregarTexto(LinearLayout padre, String valor, int sp, String color, boolean negrita) {
        TextView texto = new TextView(this);
        texto.setText(valor);
        texto.setTextSize(sp);
        texto.setTextColor(Color.parseColor(color));
        if (negrita) texto.setTypeface(null, Typeface.BOLD);
        texto.setPadding(0, dp(5), 0, dp(5));
        padre.addView(texto);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("itinerarioId", itinerarioIdActual);
        outState.putString("itinerario", textoItinerarioActual);
        outState.putString("destino", destinoActual);
        outState.putInt("dias", diasActuales);
        outState.putInt("diaSeleccionado", diaSeleccionado);
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
            actualizarTamanoTextos(barraAccesibilidad);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
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
            actualizarTamanoTextos(barraAccesibilidad);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // BOTÓN CONTRASTE
        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();
            PreferenciasAccesibilidad.guardarContraste(this, modoContraste);

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


        if (modoContraste == 0) {
            btnContraste.setText("◐ Contraste");
        } else if (modoContraste == 1) {
            btnContraste.setText("◐ Crema");
        } else {
            btnContraste.setText("◐ Oscuro");
        }


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

            actualizarControlesDinamicos(scrollContenido);
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

        if (modoContraste != 0) {
            actualizarControlesDinamicos(scrollContenido);
        }
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



    private void actualizarControlesDinamicos(View vista) {
        if (vista == null) return;

        // CUADRADITOS DEL CHECKLIST
        if (vista instanceof CheckBox) {
            CheckBox check = (CheckBox) vista;

            int color;

            if (modoContraste == 1) {
                // CREMA
                color = Color.parseColor("#A88659");
            } else if (modoContraste == 2) {
                // OSCURO
                color = Color.parseColor("#BDBDBD");
            } else {
                // NORMAL
                color = Color.parseColor("#2563EB");
            }

            check.setButtonTintList(
                    ColorStateList.valueOf(color)
            );

        } else if (vista instanceof Button) {
            Button boton = (Button) vista;
            String nombre = boton.getText().toString();

            boolean esBotonActividad =
                    nombre.equals("Cómo llegar con Google Maps")
                            || nombre.equals("Cambiar actividad")
                            || nombre.equals("Ver recorrido del día en Google Maps");

            if (esBotonActividad) {

                if (modoContraste == 1 || modoContraste == 2) {

                    int fondo = modoContraste == 1
                            ? Color.parseColor("#E9D7B8")
                            : Color.parseColor("#303030");

                    int borde = modoContraste == 1
                            ? Color.parseColor("#B99768")
                            : Color.parseColor("#888888");

                    int texto = modoContraste == 1
                            ? Color.parseColor("#222222")
                            : Color.WHITE;

                    boton.setBackgroundTintList(null);
                    boton.setBackground(
                            fondoRedondeado(fondo, borde, 24)
                    );
                    boton.setTextColor(texto);

                } else {
                    // MODO NORMAL: recuperar colores originales
                    int colorOriginal;

                    if (nombre.equals("Cómo llegar con Google Maps")) {
                        colorOriginal = Color.parseColor("#2563EB");
                    } else if (nombre.equals("Cambiar actividad")) {
                        colorOriginal = Color.parseColor("#0F766E");
                    } else {
                        colorOriginal = Color.parseColor("#F43F5E");
                    }

                    boton.setBackgroundTintList(null);
                    boton.setBackground(fondosOriginales.get(boton));
                    boton.setBackgroundTintList(
                            ColorStateList.valueOf(colorOriginal)
                    );
                    boton.setTextColor(Color.WHITE);
                    boton.setForeground(null);
                }
            }
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarControlesDinamicos(
                        grupo.getChildAt(i)
                );
            }
        }
    }



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
