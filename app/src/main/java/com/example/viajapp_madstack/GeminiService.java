
package com.example.viajapp_madstack;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

public class GeminiService {

    private final GenerativeModelFutures model;
    private final Context context;

    public interface GeminiCallback {
        void onSuccess(String respuesta);
        void onError(String error);
    }

    public GeminiService(Context context) {
        this.context = context.getApplicationContext();

        GenerativeModel ai = FirebaseAI
                .getInstance(GenerativeBackend.agentPlatform("global"))
                .generativeModel("gemini-3.5-flash-lite");

        model = GenerativeModelFutures.from(ai);
    }

    public void generarTexto(String instruccion, GeminiCallback callback) {

        Content prompt = new Content.Builder()
                .addText(instruccion)
                .build();

        ListenableFuture<GenerateContentResponse> response =
                model.generateContent(prompt);

        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {

                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String texto = result.getText();

                        if (texto != null && !texto.isEmpty()) {
                            callback.onSuccess(texto);
                        } else {
                            callback.onError(
                                    "Gemini devolvió una respuesta vacía."
                            );
                        }
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        callback.onError(error.toString());
                    }
                },
                ContextCompat.getMainExecutor(context)
        );
    }
}
