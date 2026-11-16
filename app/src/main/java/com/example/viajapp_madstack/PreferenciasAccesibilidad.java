
package com.example.viajapp_madstack;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenciasAccesibilidad {

    private static final String ARCHIVO =
            "viajapp_accesibilidad_global";

    private static final String CLAVE_CONTRASTE =
            "modo_contraste";

    private static final String CLAVE_TEXTO =
            "escala_texto";

    private static SharedPreferences obtenerPreferencias(
            Context context) {

        return context.getApplicationContext()
                .getSharedPreferences(
                        ARCHIVO,
                        Context.MODE_PRIVATE
                );
    }

    // CONTRASTE: 0 = NORMAL, 1 = CREMA, 2 = OSCURO

    public static int obtenerContraste(Context context) {
        return obtenerPreferencias(context)
                .getInt(CLAVE_CONTRASTE, 0);
    }

    public static void guardarContraste(
            Context context, int modo) {

        if (modo < 0 || modo > 2) return;

        obtenerPreferencias(context)
                .edit()
                .putInt(CLAVE_CONTRASTE, modo)
                .apply();
    }

    // TAMAÑO DE TEXTO: DE 0.9 A 1.3

    public static float obtenerEscalaTexto(Context context) {
        return obtenerPreferencias(context)
                .getFloat(CLAVE_TEXTO, 1.0f);
    }

    public static void guardarEscalaTexto(
            Context context, float escala) {

        float valor = Math.max(
                0.9f,
                Math.min(1.3f, escala)
        );

        obtenerPreferencias(context)
                .edit()
                .putFloat(CLAVE_TEXTO, valor)
                .apply();
    }
}
