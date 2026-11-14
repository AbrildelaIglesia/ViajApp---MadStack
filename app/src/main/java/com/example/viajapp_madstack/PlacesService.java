
package com.example.viajapp_madstack;

import android.content.Context;
import android.util.Log;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;

import java.util.Arrays;
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

        // Comprobar la clave sin mostrarla en Logcat
        boolean claveConfigurada =
                apiKey != null && !apiKey.trim().isEmpty();

        Log.d(TAG, "API Key configurada: " + claveConfigurada);

        if (!claveConfigurada) {
            throw new IllegalStateException(
                    "La clave de Google Places está vacía. "
                            + "Revisá PLACES_API_KEY en local.properties "
                            + "y su configuración en app/build.gradle."
            );
        }

        if (!Places.isInitialized()) {

            Places.initializeWithNewPlacesApiEnabled(
                    appContext,
                    apiKey
            );

            Log.d(TAG, "Google Places inicializado");
        }

        placesClient = Places.createClient(appContext);
    }

    public void buscarLugaresTuristicos(
            String destino,
            PlacesCallback callback
    ) {

        if (destino == null || destino.trim().isEmpty()) {
            callback.onError("No se seleccionó un destino.");
            return;
        }

        List<Place.Field> campos = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION
        );

        SearchByTextRequest request =
                SearchByTextRequest.builder(
                                "atracciones turísticas en "
                                        + destino.trim()
                                        + ", Argentina",
                                campos
                        )
                        .setMaxResultCount(15)
                        .setRegionCode("AR")
                        .build();

        Log.d(TAG, "Buscando lugares en: " + destino);

        placesClient.searchByText(request)
                .addOnSuccessListener(response -> {

                    StringBuilder resultado = new StringBuilder();

                    List<Place> lugares = response.getPlaces();

                    Log.d(TAG,
                            "Lugares encontrados: " + lugares.size());

                    if (lugares.isEmpty()) {
                        callback.onSuccess("");
                        return;
                    }

                    for (Place lugar : lugares) {

                        resultado.append("Lugar: ")
                                .append(lugar.getDisplayName())
                                .append("\n");

                        resultado.append("Dirección: ")
                                .append(lugar.getFormattedAddress())
                                .append("\n");

                        resultado.append("Place ID: ")
                                .append(lugar.getId())
                                .append("\n");

                        if (lugar.getLocation() != null) {

                            resultado.append("Latitud: ")
                                    .append(
                                            lugar.getLocation().latitude
                                    )
                                    .append("\n");

                            resultado.append("Longitud: ")
                                    .append(
                                            lugar.getLocation().longitude
                                    )
                                    .append("\n");
                        }

                        resultado.append(
                                "Accesibilidad del lugar: "
                                        + "pendiente de verificar"
                        ).append("\n\n");
                    }

                    callback.onSuccess(resultado.toString());
                })
                .addOnFailureListener(error -> {

                    Log.e(TAG,
                            "Error al buscar lugares",
                            error);

                    String mensaje = error.getMessage();

                    callback.onError(
                            mensaje != null
                                    ? mensaje
                                    : error.toString()
                    );
                });
    }
}
