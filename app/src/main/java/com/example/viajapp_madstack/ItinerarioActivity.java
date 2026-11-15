package com.example.viajapp_madstack;

import android.content.Intent;
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

    private String textoItinerarioActual;
    private String destinoActual;
    private int diasActuales;
    private JSONArray diasGenerados;
    private LinearLayout barraDiasGenerados, tarjetasActividades;
    private int diaSeleccionado = 0;
    private PlacesClient clienteFotos;

    private void mostrarItinerarioRecibido(Bundle estado) {
        if (estado != null && estado.containsKey("itinerario")) {
            textoItinerarioActual = estado.getString("itinerario");
            destinoActual = estado.getString("destino");
            diasActuales = estado.getInt("dias", 0);
            diaSeleccionado = estado.getInt("diaSeleccionado", 0);
        } else {
            Intent datos = getIntent();
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
            tituloVacio.setText("Todavía no generaste un itinerario. Creá un viaje desde Planear.");
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
    }

    private void mostrarDia(int indice) {
        diaSeleccionado = indice;
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

    private String consultaLugar(String nombre) {
        String destino = destinoActual == null ? "" : destinoActual;
        return nombre + ", " + destino + ", Argentina";
    }

    private void cargarFotoActividad(ImageView imagen, String nombre) {
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
                        imagen.setContentDescription("Imagen de referencia de Google Places para " + nombre);
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

        // AUDIO SE PROGRAMARÁ DESPUÉS
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
