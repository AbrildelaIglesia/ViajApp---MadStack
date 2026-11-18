package com.example.viajapp_madstack;

import android.content.Context;
import android.util.Log;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AccessibilityOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PlacesService {
    private static final String TAG = "VIAJAPP_PLACES";
    private final PlacesClient placesClient;

    public interface PlacesCallback {
        void onSuccess(String lugares);
        void onError(String error);
    }

    public PlacesService(Context context) {
        Context appContext = context.getApplicationContext();
        String apiKey = BuildConfig.PLACES_API_KEY;
        boolean claveConfigurada = apiKey != null && !apiKey.trim().isEmpty();
        Log.d(TAG, "API Key configurada: " + claveConfigurada);
        if (!claveConfigurada) {
            throw new IllegalStateException(
                    "La clave de Google Places está vacía. Revisá PLACES_API_KEY "
                            + "en local.properties y app/build.gradle.");
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(appContext, apiKey);
            Log.d(TAG, "Google Places inicializado");
        }
        placesClient = Places.createClient(appContext);
    }

    public void buscarLugaresTuristicos(String destino, PlacesCallback callback) {
        if (destino == null || destino.trim().isEmpty()) {
            callback.onError("No se seleccionó un destino.");
            return;
        }

        List<Place.Field> campos = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
                Place.Field.ACCESSIBILITY_OPTIONS
        );

        SearchByTextRequest request = SearchByTextRequest.builder(
                        "atracciones turísticas en " + destino.trim() + ", Argentina", campos)
                .setMaxResultCount(20)
                .setRegionCode("AR")
                .build();

        Log.d(TAG, "Buscando lugares en: " + destino);
        placesClient.searchByText(request)
                .addOnSuccessListener(response -> {
                    List<Place> lugares = new ArrayList<>(response.getPlaces());
                    if (lugares.isEmpty()) {
                        callback.onSuccess("");
                        return;
                    }
                    // Solo reordena lo que Google Places devolvió: no elimina lugares sin datos.
                    lugares.sort(Comparator.comparingInt(this::puntajeAccesibilidad).reversed());
                    StringBuilder resultado = new StringBuilder();
                    resultado.append("Datos de accesibilidad informados por Google Places. ")
                            .append("No equivalen a una certificación integral; confirmar antes de viajar.\n\n");

                    for (Place lugar : lugares) {
                        resultado.append("Lugar: ").append(valor(lugar.getDisplayName())).append("\n");
                        resultado.append("Dirección: ").append(valor(lugar.getFormattedAddress())).append("\n");
                        resultado.append("Place ID: ").append(valor(lugar.getId())).append("\n");
                        if (lugar.getLocation() != null) {
                            resultado.append("Latitud: ").append(lugar.getLocation().latitude).append("\n");
                            resultado.append("Longitud: ").append(lugar.getLocation().longitude).append("\n");
                        }
                        resultado.append("Prioridad por accesibilidad: ")
                                .append(puntajeAccesibilidad(lugar) > 0 ? "alta (hay características positivas)" : "normal")
                                .append("\n");
                        resultado.append("Accesibilidad informada por Google Places:\n");
                        AccessibilityOptions opciones = lugar.getAccessibilityOptions();
                        if (opciones == null) {
                            resultado.append("- Sin información disponible; verificar antes de viajar.\n");
                        } else {
                            agregarAtributo(resultado, "Entrada accesible para silla de ruedas", opciones.getWheelchairAccessibleEntrance());
                            agregarAtributo(resultado, "Estacionamiento accesible", opciones.getWheelchairAccessibleParking());
                            agregarAtributo(resultado, "Baño accesible", opciones.getWheelchairAccessibleRestroom());
                            agregarAtributo(resultado, "Asientos accesibles", opciones.getWheelchairAccessibleSeating());
                        }
                        resultado.append("\n");
                    }
                    Log.d(TAG, "Lugares encontrados: " + lugares.size());
                    callback.onSuccess(resultado.toString());
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Error al buscar lugares", error);
                    callback.onError(error.getMessage() != null ? error.getMessage() : error.toString());
                });
    }

    private int puntajeAccesibilidad(Place lugar) {
        AccessibilityOptions opciones = lugar.getAccessibilityOptions();
        if (opciones == null) return 0;
        int puntos = 0;
        if (esPositivo(opciones.getWheelchairAccessibleEntrance())) puntos += 4;
        if (esPositivo(opciones.getWheelchairAccessibleRestroom())) puntos += 2;
        if (esPositivo(opciones.getWheelchairAccessibleParking())) puntos += 1;
        if (esPositivo(opciones.getWheelchairAccessibleSeating())) puntos += 1;
        return puntos;
    }

    private boolean esPositivo(Place.BooleanPlaceAttributeValue atributo) {
        return atributo == Place.BooleanPlaceAttributeValue.TRUE;
    }

    private void agregarAtributo(StringBuilder texto, String nombre,
                                 Place.BooleanPlaceAttributeValue atributo) {
        String estado;
        if (atributo == Place.BooleanPlaceAttributeValue.TRUE) {
            estado = "Sí, según Google Places";
        } else if (atributo == Place.BooleanPlaceAttributeValue.FALSE) {
            estado = "No disponible, según Google Places";
        } else {
            estado = "Sin información; por verificar";
        }
        texto.append("- ").append(nombre).append(": ").append(estado).append("\n");
    }

    private String valor(String texto) {
        return texto == null ? "No informado" : texto;
    }
}
