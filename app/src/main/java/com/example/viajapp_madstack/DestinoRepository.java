package com.example.viajapp_madstack;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DestinoRepository {

    private final FirebaseFirestore db;

    public DestinoRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void buscarDestinos(String texto, DestinoCallback callback) {
        String busqueda = normalizar(texto);
        if (busqueda.isEmpty()) {
            callback.onResultadosVacios();
            return;
        }

        // Los fragmentos de 1 o 2 caracteres pueden producir demasiados resultados.
        // Este índice admite búsquedas desde 3 caracteres.
        if (busqueda.length() < 3) {
            callback.onResultadosVacios();
            return;
        }

        db.collection("destinos")
                .whereArrayContains("fragmentosBusqueda", busqueda)
                .limit(50)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<DocumentSnapshot> coincidencias = new ArrayList<>();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        String nombre = documento.getString("nombre");
                        if (normalizar(nombre).contains(busqueda)) {
                            coincidencias.add(documento);
                        }
                    }

                    coincidencias.sort(Comparator.comparing(
                            d -> normalizar(d.getString("nombre"))));
                    if (coincidencias.size() > 15) {
                        coincidencias = new ArrayList<>(coincidencias.subList(0, 15));
                    }

                    if (coincidencias.isEmpty()) callback.onResultadosVacios();
                    else callback.onResultadosEncontrados(coincidencias);
                })
                .addOnFailureListener(callback::onError);
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        String limpio = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return limpio.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    public interface DestinoCallback {
        void onResultadosEncontrados(List<DocumentSnapshot> destinos);
        void onResultadosVacios();
        void onError(Exception error);
    }
}
