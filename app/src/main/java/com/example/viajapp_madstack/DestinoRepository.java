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
        if (busqueda.length() < 3) {
            callback.onResultadosVacios();
            return;
        }

        // El índice fragmentosBusqueda debe incluir fragmentos de nombre Y provincia_nombre.
        // Se consulta un índice de fragmentos, sin descargar la colección completa.
        db.collection("destinos")
                .whereArrayContains("fragmentosBusqueda", busqueda)
                .limit(100)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<DocumentSnapshot> coincidencias = new ArrayList<>();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        String nombre = normalizar(documento.getString("nombre"));
                        String provincia = normalizar(documento.getString("provincia_nombre"));
                        if (nombre.contains(busqueda) || provincia.contains(busqueda)) {
                            coincidencias.add(documento);
                        }
                    }

                    coincidencias.sort(Comparator
                            .comparingInt((DocumentSnapshot d) -> {
                                String nombre = normalizar(d.getString("nombre"));
                                return nombre.startsWith(busqueda) ? 0
                                        : nombre.contains(busqueda) ? 1 : 2;
                            })
                            .thenComparing(d -> normalizar(d.getString("provincia_nombre")))
                            .thenComparing(d -> normalizar(d.getString("nombre")))
                            .thenComparing(DocumentSnapshot::getId));

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
        String limpio = Normalizer.normalize(
                texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return limpio.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    public interface DestinoCallback {
        void onResultadosEncontrados(List<DocumentSnapshot> destinos);
        void onResultadosVacios();
        void onError(Exception error);
    }
}
