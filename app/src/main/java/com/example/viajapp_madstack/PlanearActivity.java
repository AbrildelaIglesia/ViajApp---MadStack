package com.example.viajapp_madstack;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.IdentityHashMap;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.Switch;
import android.widget.ImageView;
import android.view.inputmethod.InputMethodManager;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest;
import java.util.Arrays;
import java.util.ArrayList;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

import java.util.List;

public class PlanearActivity extends AppCompatActivity {

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


    // Navegacion
    private LinearLayout navPlanear;
    private LinearLayout navAccesibilidad;
    private LinearLayout navItinerario;
    private LinearLayout navLugares;

    // Buscador de destinos
    private EditText etBuscarDestino;
    private LinearLayout contenedorResultadosDestinos;
    private TextView tvDestinoSeleccionado;
    private ImageView imgDestino;
    private TextView tvCantidadDias, tvCantidadViajeros, tvInteresesActivos;
    private int cantidadDias = 5;
    private int cantidadViajeros = 2;
    private final boolean[] interesesSeleccionados = {true, true, true};
    private LinearLayout interesNaturaleza, interesGastronomia, interesTrekking;
    private PlacesClient placesFotosClient;
    private int solicitudFoto = 0;

    // Firebase
    private DestinoRepository destinoRepository;

    // Control de escritura
    private final Handler handlerBusqueda =
            new Handler(Looper.getMainLooper());

    private Runnable consultaPendiente;

    // Destino seleccionado
    private String destinoId;
    private Long destinoIdNumerico;
    private String destinoNombre;
    private String destinoProvincia;

    // Evita borrar el destino al limpiar el buscador
    private boolean seleccionandoDestino = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_planear_viaje);

        inicializarVistas();
        configurarAccesibilidad();


        configurarNavegacion();

        destinoRepository = new DestinoRepository();

        configurarBuscadorDestinos();
        configurarControlesPlanear();
        configurarIntereses();

        configurarBotonCrearViaje();
    }

    // --------------------------------------------------
    // CREAR VIAJE CON GEMINI
    // --------------------------------------------------

    private void configurarBotonCrearViaje() {
        Button btnCrearViajeIA = findViewById(R.id.btnCrearViajeIA);

        btnCrearViajeIA.setOnClickListener(v -> {
            if (destinoNombre == null || destinoNombre.trim().isEmpty()) {
                Toast.makeText(this, "Primero seleccioná un destino desde el buscador.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            TextView tvDias = findViewById(R.id.tvCantidadDias);
            TextView tvViajeros = findViewById(R.id.tvCantidadViajeros);
            EditText etPresupuesto = findViewById(R.id.etPresupuesto);
            Switch swTramosCortos = findViewById(R.id.swTramosCortos);

            String ubicacion = destinoNombre +
                    (destinoProvincia == null || destinoProvincia.isEmpty()
                            ? "" : ", " + destinoProvincia);
            String dias = tvDias.getText().toString().trim();
            String viajeros = tvViajeros.getText().toString().trim();
            String presupuesto = etPresupuesto.getText().toString().trim();
            String intereses = obtenerInteresesSeleccionados();
            if (intereses.isEmpty()) {
                Toast.makeText(this, "Seleccioná al menos un interés de viaje.", Toast.LENGTH_LONG).show();
                return;
            }

            if (presupuesto.isEmpty()) {
                etPresupuesto.setError("Ingresá un presupuesto");
                return;
            }

            String promptBase = "Sos un asistente especializado en turismo accesible en Argentina.\n\n"
                    + "Creá un itinerario turístico orientativo con estos datos:\n"
                    + "Destino: " + ubicacion + "\n"
                    + "Duración: " + dias + "\n"
                    + "Viajeros: " + viajeros + "\n"
                    + "Presupuesto estimado en pesos argentinos: " + presupuesto + "\n"
                    + "Priorizar tramos cortos: " + (swTramosCortos.isChecked() ? "Sí" : "No") + "\n"
                    + "Intereses seleccionados: " + intereses + "\n\n"
                    + "Organizá el itinerario por días. Proponé actividades, descansos y "
                    + "recomendaciones para personas con movilidad reducida. "
                    + "No inventes certificaciones de accesibilidad, rampas, pendientes, "
                    + "distancias ni precios. Diferenciá la accesibilidad verificada "
                    + "de la que requiere confirmación. Si no hay datos comprobados, "
                    + "indicá que deben verificarse antes de viajar. Respondé en español claro.\n"
                    + "Los datos de Google Places son referencias de lugares reales, no "
                    + "pruebas de accesibilidad. No afirmes que un lugar es accesible "
                    + "sin evidencia específica. No inventes lugares ni direcciones.\n"
                    + "FORMATO OBLIGATORIO: devolvé solamente un objeto JSON válido, sin markdown ni texto fuera del JSON. "
                    + "Estructura: {\"dias\":[{\"dia\":1,\"actividades\":[{\"hora\":\"10:00\",\"nombre\":\"Nombre del lugar\",\"descripcion\":\"Descripción breve\",\"accesibilidad\":\"Por verificar: información faltante\"}]}]}. "
                    + "Incluí exactamente " + cantidadDias + " objetos en dias, numerados del 1 al " + cantidadDias + ". "
                    + "Cada día debe tener EXACTAMENTE 3 actividades, ordenadas por hora. "
                    + "Cada actividad debe incluir hora, nombre, descripcion y accesibilidad como strings. "
                    + "No inventes verificaciones, reservas, clima, fotos ni servicios. "
                    + "Si no hay tres lugares distintos comprobables, podés proponer descansos o actividades genéricas "
                    + "sin atribuirles un establecimiento inventado.\n";

            btnCrearViajeIA.setEnabled(false);
            btnCrearViajeIA.setText("Buscando lugares turísticos...");

            FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
            if (usuario == null) {
                restaurarBotonCrearViaje(btnCrearViajeIA);
                mostrarErrorViaje("Iniciá sesión para cargar tus preferencias de accesibilidad.");
                return;
            }

            btnCrearViajeIA.setText("Cargando preferencias...");
            FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(usuario.getUid())
                    .get()
                    .addOnSuccessListener(documento -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (!documento.exists()) {
                            restaurarBotonCrearViaje(btnCrearViajeIA);
                            mostrarErrorViaje("No se encontró tu perfil en Firebase.");
                            return;
                        }

                        Object datos = documento.get("accesibilidad");
                        if (!(datos instanceof Map)) {
                            restaurarBotonCrearViaje(btnCrearViajeIA);
                            mostrarErrorViaje("Tu perfil no tiene preferencias de accesibilidad registradas.");
                            return;
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> preferencias = (Map<String, Object>) datos;
                        String preferenciasTexto = construirPreferencias(preferencias);
                        consultarPlacesYGenerar(ubicacion, promptBase, preferenciasTexto, btnCrearViajeIA);
                    })
                    .addOnFailureListener(error -> {
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(btnCrearViajeIA);
                        mostrarErrorViaje("No se pudieron cargar tus preferencias: " + error.getMessage());
                    });
        });
    }

    private String construirPreferencias(Map<String, Object> preferencias) {
        StringBuilder texto = new StringBuilder();
        String[] campos = {"recorridosCortos", "sillaRuedas", "subtitulos", "visionReducida", "sinAdaptaciones"};
        for (String campo : campos) {
            Object valor = preferencias.get(campo);
            texto.append(campo).append(": ")
                    .append(valor instanceof Boolean ? ((Boolean) valor ? "Sí" : "No") : "No informado")
                    .append("\n");
        }
        return texto.toString();
    }

    private void consultarPlacesYGenerar(String ubicacion, String promptBase,
                                         String preferencias, Button boton) {
        boton.setText("Buscando lugares turísticos...");
        try {
            PlacesService placesService = new PlacesService(this);
            placesService.buscarLugaresTuristicos(ubicacion, new PlacesService.PlacesCallback() {
                @Override
                public void onSuccess(String lugares) {
                    runOnUiThread(() -> {

                        if (isFinishing() || isDestroyed()) return;
                        if (lugares == null || lugares.trim().isEmpty()) {
                            restaurarBotonCrearViaje(boton);
                            mostrarErrorViaje("Google Places no encontró lugares para este destino.");
                            return;
                        }
                        boton.setText("Generando itinerario...");
                        String prompt = promptBase
                                + "\nPREFERENCIAS DEL USUARIO (de Firebase):\n" + preferencias
                                + "\nUsá estas preferencias para adaptar el itinerario. "
                                + "El campo sinAdaptaciones no tiene una interpretación definida: "
                                + "no supongas su significado. "
                                + "No confundas necesidades del usuario con características verificadas de lugares.\n"
                                + "\nLUGARES OBTENIDOS DE GOOGLE PLACES:\n" + lugares
                                + "\nUsá únicamente estos lugares como propuestas concretas. "
                                + "Si son insuficientes, indicalo. "
                                + "No afirmes que son accesibles sin datos comprobados.";
                        generarItinerarioConGemini(prompt, boton);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("VIAJAPP_PLACES", "Error al buscar lugares: " + error);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        mostrarErrorViaje("No se pudieron consultar los lugares: " + error);
                    });
                }
            });
        } catch (Exception error) {
            Log.e("VIAJAPP_PLACES", "Error al iniciar Places", error);
            restaurarBotonCrearViaje(boton);
            mostrarErrorViaje(error.toString());
        }
    }

    private void generarItinerarioConGemini(String prompt, Button boton) {
        try {
            GeminiService geminiService = new GeminiService(this);
            geminiService.generarTexto(prompt, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String respuesta) {
                    runOnUiThread(() -> {
                        Log.d("VIAJAPP_GEMINI", "Respuesta completa: " + respuesta);
                        Log.d("VIAJAPP_GEMINI", "Longitud: " +
                                (respuesta == null ? 0 : respuesta.length()));
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        if (respuesta == null || respuesta.trim().isEmpty()) {
                            mostrarErrorViaje("Gemini no devolvió un itinerario.");
                            return;
                        }
                        String intentJson;
                        try {
                            String limpio = respuesta.trim();
                            if (limpio.startsWith("```")) {
                                limpio = limpio.replaceFirst("(?s)^```(?:json)?\\s*", "")
                                        .replaceFirst("(?s)\\s*```$", "").trim();
                            }
                            org.json.JSONObject raiz = new org.json.JSONObject(limpio);
                            org.json.JSONArray diasJson = raiz.getJSONArray("dias");
                            if (diasJson.length() != cantidadDias) throw new org.json.JSONException("Cantidad de días incorrecta");
                            for (int i = 0; i < diasJson.length(); i++) {
                                org.json.JSONObject dia = diasJson.getJSONObject(i);
                                if (dia.getInt("dia") != i + 1 || dia.getJSONArray("actividades").length() != 3)
                                    throw new org.json.JSONException("Se requieren tres actividades por día");
                                org.json.JSONArray actividades = dia.getJSONArray("actividades");
                                for (int j = 0; j < 3; j++) {
                                    org.json.JSONObject actividad = actividades.getJSONObject(j);
                                    actividad.getString("hora"); actividad.getString("nombre");
                                    actividad.getString("descripcion"); actividad.getString("accesibilidad");
                                }
                            }
                            intentJson = limpio;
                        } catch (org.json.JSONException ex) {
                            mostrarErrorViaje("Gemini devolvió un formato incompleto. Intentá generar el viaje nuevamente.");
                            Log.e("VIAJAPP_GEMINI", "JSON inválido", ex);
                            return;
                        }
                        guardarItinerarioYMostrar(intentJson, boton);
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("VIAJAPP_GEMINI", "Error al generar itinerario: " + error);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        mostrarErrorViaje(error);
                    });
                }
            });
        } catch (Exception error) {
            Log.e("VIAJAPP_GEMINI", "Error al iniciar Gemini", error);
            restaurarBotonCrearViaje(boton);
            mostrarErrorViaje(error.toString());
        }
    }

    private void guardarItinerarioYMostrar(String itinerarioJson, Button boton) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            mostrarErrorViaje("Tu sesión terminó. Iniciá sesión nuevamente para guardar el viaje.");
            return;
        }

        boton.setEnabled(false);
        boton.setText("Guardando itinerario...");
        try {
            JSONObject raiz = new JSONObject(itinerarioJson);
            Map<String, Object> datos = new HashMap<>();
            datos.put("destino", destinoNombre);
            datos.put("provincia", destinoProvincia == null ? "" : destinoProvincia);
            datos.put("cantidadDias", cantidadDias);
            datos.put("cantidadViajeros", cantidadViajeros);
            datos.put("itinerarioJson", itinerarioJson);
            // Se guarda también la estructura de días y actividades para futuras consultas.
            datos.put("dias", jsonArrayALista(raiz.getJSONArray("dias")));
            datos.put("fechaCreacion", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance()
                    .collection("usuarios").document(usuario.getUid())
                    .collection("itinerarios").add(datos)
                    .addOnSuccessListener(documento -> {
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        Intent intent = new Intent(PlanearActivity.this, ItinerarioActivity.class);
                        intent.putExtra("itinerarioId", documento.getId());
                        intent.putExtra("itinerario", itinerarioJson);
                        intent.putExtra("destino", destinoNombre);
                        intent.putExtra("dias", cantidadDias);
                        startActivity(intent);
                    })
                    .addOnFailureListener(error -> {
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        Log.e("VIAJAPP_FIRESTORE", "Error al guardar itinerario", error);
                        mostrarErrorViaje("No se pudo guardar el itinerario en Firebase. "
                                + "Revisá tu conexión y las reglas de Firestore. " + error.getMessage());
                    });
        } catch (org.json.JSONException error) {
            restaurarBotonCrearViaje(boton);
            Log.e("VIAJAPP_FIRESTORE", "Error al preparar el itinerario", error);
            mostrarErrorViaje("El itinerario no pudo prepararse para guardar.");
        }
    }

    private List<Object> jsonArrayALista(JSONArray arreglo) throws org.json.JSONException {
        List<Object> lista = new ArrayList<>();
        for (int i = 0; i < arreglo.length(); i++) {
            lista.add(valorJsonAObjeto(arreglo.get(i)));
        }
        return lista;
    }

    private Object valorJsonAObjeto(Object valor) throws org.json.JSONException {
        if (valor == JSONObject.NULL) return null;
        if (valor instanceof JSONArray) return jsonArrayALista((JSONArray) valor);
        if (valor instanceof JSONObject) {
            JSONObject objeto = (JSONObject) valor;
            Map<String, Object> mapa = new HashMap<>();
            java.util.Iterator<String> claves = objeto.keys();
            while (claves.hasNext()) {
                String clave = claves.next();
                mapa.put(clave, valorJsonAObjeto(objeto.get(clave)));
            }
            return mapa;
        }
        return valor;
    }

    private void restaurarBotonCrearViaje(Button boton) {
        boton.setEnabled(true);
        boton.setText("Crear viaje accesible con IA");
    }

    private void mostrarErrorViaje(String mensaje) {
        new AlertDialog.Builder(this)
                .setTitle("Error al generar itinerario")
                .setMessage(mensaje)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    // --------------------------------------------------
    // --------------------------------------------------
    // INICIALIZAR VISTAS
    // --------------------------------------------------

    private void inicializarVistas() {

        // Navegacion inferior
        navPlanear = findViewById(R.id.navPlanear);
        navAccesibilidad = findViewById(R.id.navAccesibilidad);
        navItinerario = findViewById(R.id.navItinerario);
        navLugares = findViewById(R.id.navLugares);

        // Buscador
        etBuscarDestino = findViewById(R.id.etBuscarDestino);

        contenedorResultadosDestinos =
                findViewById(R.id.contenedorResultadosDestinos);

        tvDestinoSeleccionado =
                findViewById(R.id.tvDestinoSeleccionado);
        imgDestino = findViewById(R.id.imgDestino);
        imgDestino.setImageDrawable(null);
        tvDestinoSeleccionado.setText("");
        tvCantidadDias = findViewById(R.id.tvCantidadDias);
        tvCantidadViajeros = findViewById(R.id.tvCantidadViajeros);
        tvInteresesActivos = findViewById(R.id.tvInteresesActivos);
        interesNaturaleza = findViewById(R.id.interesNaturaleza);
        interesGastronomia = findViewById(R.id.interesGastronomia);
        interesTrekking = findViewById(R.id.interesTrekking);
    }

    // --------------------------------------------------
    // NAVEGACION
    // --------------------------------------------------

    private void configurarNavegacion() {

        navPlanear.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PlanearActivity.this,
                    PlanearActivity.class
            );

            startActivity(intent);
        });

        navAccesibilidad.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PlanearActivity.this,
                    AccesibilidadActivity.class
            );

            startActivity(intent);
        });

        navItinerario.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PlanearActivity.this,
                    ItinerarioActivity.class
            );

            startActivity(intent);
        });

        navLugares.setOnClickListener(v -> {

            Intent intent = new Intent(
                    PlanearActivity.this,
                    LugaresActivity.class
            );

            startActivity(intent);
        });
    }

    // --------------------------------------------------
    // CONFIGURAR BUSCADOR
    // --------------------------------------------------

    private void configurarBuscadorDestinos() {

        etBuscarDestino.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                if (seleccionandoDestino) {
                    return;
                }

                // Cancelar consulta pendiente
                if (consultaPendiente != null) {
                    handlerBusqueda.removeCallbacks(
                            consultaPendiente
                    );
                }

                // Una nueva busqueda invalida la seleccion anterior
                destinoId = null;
                destinoIdNumerico = null;
                destinoNombre = null;
                destinoProvincia = null;

                String texto = s.toString().trim();

                if (texto.isEmpty()) {

                    contenedorResultadosDestinos.removeAllViews();

                    contenedorResultadosDestinos.setVisibility(
                            View.GONE
                    );

                    return;
                }

                // Esperar antes de consultar Firebase
                consultaPendiente = () -> consultarDestinos(texto);

                handlerBusqueda.postDelayed(
                        consultaPendiente,
                        350
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // --------------------------------------------------
    // CONSULTAR DESTINOS EN FIREBASE
    // --------------------------------------------------

    private void consultarDestinos(String texto) {

        destinoRepository.buscarDestinos(
                texto,
                new DestinoRepository.DestinoCallback() {

                    @Override
                    public void onResultadosEncontrados(
                            List<DocumentSnapshot> destinos) {

                        if (!texto.equals(
                                etBuscarDestino.getText()
                                        .toString().trim())) {
                            return;
                        }

                        mostrarResultadosDestinos(destinos);
                    }

                    @Override
                    public void onResultadosVacios() {

                        if (!texto.equals(
                                etBuscarDestino.getText()
                                        .toString().trim())) {
                            return;
                        }

                        mostrarMensajeSinResultados();
                    }

                    @Override
                    public void onError(Exception error) {

                        if (!texto.equals(
                                etBuscarDestino.getText()
                                        .toString().trim())) {
                            return;
                        }

                        Toast.makeText(
                                PlanearActivity.this,
                                "Error al consultar Firebase: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    // --------------------------------------------------
    // MOSTRAR RESULTADOS
    // --------------------------------------------------

    private void mostrarResultadosDestinos(
            List<DocumentSnapshot> destinos) {

        contenedorResultadosDestinos.removeAllViews();

        for (DocumentSnapshot documento : destinos) {

            String nombre = documento.getString("nombre");

            String provincia = documento.getString(
                    "provincia_nombre"
            );

            if (nombre == null) {
                continue;
            }

            if (provincia == null) {
                provincia = "";
            }

            TextView resultado = new TextView(this);

            resultado.setText(nombre + "\n" + provincia);

            resultado.setTextColor(
                    Color.parseColor("#0C2340")
            );

            resultado.setTextSize(14);

            resultado.setGravity(
                    Gravity.CENTER_VERTICAL
            );

            resultado.setPadding(
                    14, 12, 14, 12
            );

            resultado.setBackgroundResource(
                    android.R.drawable.list_selector_background
            );

            resultado.setOnClickListener(v ->
                    seleccionarDestino(documento)
            );

            contenedorResultadosDestinos.addView(resultado);
        }

        contenedorResultadosDestinos.setVisibility(
                View.VISIBLE
        );
    }

    // --------------------------------------------------
    // MENSAJE SIN RESULTADOS
    // --------------------------------------------------

    private void mostrarMensajeSinResultados() {

        contenedorResultadosDestinos.removeAllViews();

        TextView mensaje = new TextView(this);

        mensaje.setText("No se encontraron localidades.");

        mensaje.setTextColor(
                Color.parseColor("#64748B")
        );

        mensaje.setTextSize(13);

        mensaje.setPadding(
                12, 16, 12, 16
        );

        contenedorResultadosDestinos.addView(mensaje);

        contenedorResultadosDestinos.setVisibility(
                View.VISIBLE
        );
    }

    // --------------------------------------------------
    // SELECCIONAR DESTINO
    // --------------------------------------------------

    private void seleccionarDestino(
            DocumentSnapshot documento) {

        destinoId = documento.getId();

        destinoIdNumerico = documento.getLong("id");

        destinoNombre = documento.getString("nombre");

        destinoProvincia = documento.getString(
                "provincia_nombre"
        );

        String ubicacion = destinoNombre;

        if (destinoProvincia != null
                && !destinoProvincia.isEmpty()) {

            ubicacion += ", " + destinoProvincia;
        }

        // Actualizar destino seleccionado
        tvDestinoSeleccionado.setText(ubicacion);
        cargarFotoDestino(ubicacion);

        // Limpiar buscador sin borrar el destino seleccionado
        seleccionandoDestino = true;

        etBuscarDestino.setText("");

        seleccionandoDestino = false;

        // Cancelar cualquier consulta pendiente
        if (consultaPendiente != null) {
            handlerBusqueda.removeCallbacks(
                    consultaPendiente
            );
        }

        // Ocultar resultados
        contenedorResultadosDestinos.removeAllViews();

        contenedorResultadosDestinos.setVisibility(
                View.GONE
        );

        Toast.makeText(
                this,
                "Destino seleccionado: " + ubicacion,
                Toast.LENGTH_SHORT
        ).show();
    }


    private void configurarControlesPlanear() {
        findViewById(R.id.btnMenosDias).setOnClickListener(v -> {
            cantidadDias = Math.max(1, cantidadDias - 1);
            actualizarContadores();
        });
        findViewById(R.id.btnMasDias).setOnClickListener(v -> {
            cantidadDias = Math.min(30, cantidadDias + 1);
            actualizarContadores();
        });
        findViewById(R.id.btnMenosViajeros).setOnClickListener(v -> {
            cantidadViajeros = Math.max(1, cantidadViajeros - 1);
            actualizarContadores();
        });
        findViewById(R.id.btnMasViajeros).setOnClickListener(v -> {
            cantidadViajeros = Math.min(20, cantidadViajeros + 1);
            actualizarContadores();
        });
        findViewById(R.id.btnCambiarDestino).setOnClickListener(v -> {
            etBuscarDestino.requestFocus();
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(etBuscarDestino, InputMethodManager.SHOW_IMPLICIT);
        });
        actualizarContadores();
    }

    private void actualizarContadores() {
        tvCantidadDias.setText(cantidadDias == 1 ? "1 día" : cantidadDias + " días");
        tvCantidadViajeros.setText(cantidadViajeros == 1 ? "1 persona" : cantidadViajeros + " personas");
    }

    private void configurarIntereses() {
        LinearLayout[] tarjetas = {interesNaturaleza, interesGastronomia, interesTrekking};
        for (int i = 0; i < tarjetas.length; i++) {
            final int indice = i;
            tarjetas[i].setOnClickListener(v -> {
                interesesSeleccionados[indice] = !interesesSeleccionados[indice];
                actualizarIntereses();
            });
        }
        actualizarIntereses();
    }

    private void actualizarIntereses() {
        LinearLayout[] tarjetas = {interesNaturaleza, interesGastronomia, interesTrekking};
        int activos = 0;
        for (int i = 0; i < tarjetas.length; i++) {
            boolean activo = interesesSeleccionados[i];
            if (activo) activos++;
            TextView tilde = tarjetas[i].findViewById(R.id.tvCheckInteres);
            tilde.setVisibility(activo ? View.VISIBLE : View.INVISIBLE);
            tarjetas[i].setAlpha(activo ? 1f : 0.55f);
            tarjetas[i].setSelected(activo);
        }
        tvInteresesActivos.setText(activos + (activos == 1 ? " activo" : " activos"));
    }

    private String obtenerInteresesSeleccionados() {
        List<String> intereses = new ArrayList<>();
        if (interesesSeleccionados[0]) intereses.add("Naturaleza accesible");
        if (interesesSeleccionados[1]) intereses.add("Gastronomía regional");
        if (interesesSeleccionados[2]) intereses.add("Trekking adaptado");
        return android.text.TextUtils.join(", ", intereses);
    }

    private void cargarFotoDestino(String ubicacion) {
        final int peticionActual = ++solicitudFoto;
        Glide.with(this).clear(imgDestino);
        imgDestino.setImageDrawable(null);
        imgDestino.setContentDescription("Buscando fotografía de " + ubicacion);

        try {
            if (!Places.isInitialized()) {
                String clave = BuildConfig.PLACES_API_KEY;
                if (clave == null || clave.trim().isEmpty()) {
                    Log.e("VIAJAPP_FOTOS", "La clave de Places está vacía");
                    return;
                }
                Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), clave);
            }
            if (placesFotosClient == null) placesFotosClient = Places.createClient(this);

            List<Place.Field> campos = Arrays.asList(
                    Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.PHOTO_METADATAS);
            SearchByTextRequest solicitud = SearchByTextRequest.builder(
                            ubicacion + ", Argentina", campos)
                    .setMaxResultCount(1)
                    .setRegionCode("AR")
                    .build();

            placesFotosClient.searchByText(solicitud)
                    .addOnSuccessListener(respuesta -> {
                        if (peticionActual != solicitudFoto || isFinishing() || isDestroyed()) return;
                        if (respuesta.getPlaces().isEmpty()) return;
                        Place lugar = respuesta.getPlaces().get(0);
                        List<PhotoMetadata> fotos = lugar.getPhotoMetadatas();
                        if (fotos == null || fotos.isEmpty()) return;
                        FetchResolvedPhotoUriRequest foto =
                                FetchResolvedPhotoUriRequest.builder(fotos.get(0))
                                        .setMaxWidth(900)
                                        .build();
                        placesFotosClient.fetchResolvedPhotoUri(foto)
                                .addOnSuccessListener(resultado -> {
                                    if (peticionActual != solicitudFoto || isFinishing() || isDestroyed()) return;
                                    if (resultado.getUri() == null) return;
                                    Glide.with(PlanearActivity.this)
                                            .load(resultado.getUri())
                                            .placeholder(R.drawable.bariloche)
                                            .error(R.drawable.bariloche)
                                            .into(imgDestino);
                                    imgDestino.setContentDescription("Fotografía de " + ubicacion + " obtenida de Google Places");
                                })
                                .addOnFailureListener(error -> Log.w("VIAJAPP_FOTOS", "No se pudo descargar la fotografía", error));
                    })
                    .addOnFailureListener(error -> Log.w("VIAJAPP_FOTOS", "No se encontró fotografía para el destino", error));
        } catch (Exception error) {
            Log.e("VIAJAPP_FOTOS", "Error al consultar fotografía", error);
        }
    }

    // --------------------------------------------------
    // CICLO DE VIDA
    // --------------------------------------------------

    @Override
    protected void onDestroy() {

        if (consultaPendiente != null) {
            handlerBusqueda.removeCallbacks(consultaPendiente);
        }
        solicitudFoto++;
        super.onDestroy();
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

