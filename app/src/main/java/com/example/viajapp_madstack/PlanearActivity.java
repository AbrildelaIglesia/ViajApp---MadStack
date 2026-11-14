package com.example.viajapp_madstack;

import android.content.Intent;
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
import java.util.Map;

import java.util.List;

public class PlanearActivity extends AppCompatActivity {

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
                    + "sin evidencia específica. No inventes lugares ni direcciones.\n";

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
                        if (isFinishing() || isDestroyed()) return;
                        restaurarBotonCrearViaje(boton);
                        new AlertDialog.Builder(PlanearActivity.this)
                                .setTitle("Itinerario accesible generado")
                                .setMessage(respuesta)
                                .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                                .show();
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
}
