
package com.example.viajapp_madstack;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DestinoRepository {

    private final FirebaseFirestore db;

    public DestinoRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void buscarDestinos(String texto, DestinoCallback callback) {

        if (texto == null || texto.trim().isEmpty()) {
            callback.onResultadosVacios();
            return;
        }

        String busqueda = texto.trim();

        db.collection("destinos")
                .orderBy("nombre")
                .startAt(busqueda)
                .endAt(busqueda + "\uf8ff")
                .limit(15)
                .get()
                .addOnSuccessListener(resultado -> {

                    if (resultado.isEmpty()) {
                        callback.onResultadosVacios();
                        return;
                    }

                    callback.onResultadosEncontrados(
                            resultado.getDocuments()
                    );
                })
                .addOnFailureListener(callback::onError);
    }

    public interface DestinoCallback {

        void onResultadosEncontrados(
                List<DocumentSnapshot> destinos
        );

        void onResultadosVacios();

        void onError(Exception error);
    }
}
