package com.example.viajapp_madstack;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.Locale;

public class VoiceUtils {

    // Tipos de campos soportados para aplicar el formateo correcto
    public static final int TIPO_CORREO = 1;
    public static final int TIPO_PASSWORD = 2;
    public static final int TIPO_TEXTO_GENERAL = 3;

    // Lanza el reconocedor de voz genérico
    public static void iniciarDictado(Activity activity, int requestCode, String promptMensaje) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, promptMensaje);

        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "Tu dispositivo no soporta dictado por voz.", Toast.LENGTH_SHORT).show();
        }
    }

    // Procesa el texto dictado según el tipo de campo que corresponda
    public static String procesarTextoDictado(String textoHablado, int tipoCampo) {
        if (textoHablado == null) return "";

        String textoMinus = textoHablado.toLowerCase();

        switch (tipoCampo) {
            case TIPO_CORREO:
                return textoMinus
                        .replace(" arroba ", "@")
                        .replace(" a la arroba ", "@")
                        .replace(" punto ", ".")
                        .replaceAll("\\s+", ""); // Borra cualquier espacio sobrante

            case TIPO_PASSWORD:
                // Solo elimina los espacios en blanco tal cual se dictó
                return textoHablado.replaceAll("\\s+", "");

            case TIPO_TEXTO_GENERAL:
            default:
                return textoHablado.trim();
        }
    }
}