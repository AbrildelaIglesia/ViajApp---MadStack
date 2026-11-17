package com.example.viajapp_madstack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class ItinerariosLocales {
    private static final String ARCHIVO = "viajapp_itinerarios_locales";
    private ItinerariosLocales() {}

    private static SharedPreferences preferencias(Context context) {
        return context.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    private static String claveUsuario() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        return usuario == null ? null : "itinerarios_" + usuario.getUid();
    }

    public static synchronized void guardar(Context context, String id, String destino,
                                            int dias, String itinerarioJson) {
        String clave = claveUsuario();
        if (clave == null || id == null || itinerarioJson == null) return;
        try {
            JSONArray anteriores = new JSONArray(preferencias(context).getString(clave, "[]"));
            JSONArray actualizados = new JSONArray();
            JSONObject itinerario = new JSONObject();
            itinerario.put("id", id);
            itinerario.put("destino", destino);
            itinerario.put("dias", dias);
            itinerario.put("json", itinerarioJson);
            actualizados.put(itinerario);
            for (int i = 0; i < anteriores.length(); i++) {
                JSONObject anterior = anteriores.optJSONObject(i);
                if (anterior != null && !id.equals(anterior.optString("id"))) actualizados.put(anterior);
            }
            preferencias(context).edit().putString(clave, actualizados.toString()).apply();
        } catch (Exception e) {
            Log.e("VIAJAPP_OFFLINE", "Error guardando itinerario local", e);
        }
    }

    public static synchronized List<JSONObject> listar(Context context) {
        List<JSONObject> resultado = new ArrayList<>();
        String clave = claveUsuario();
        if (clave == null) return resultado;
        try {
            JSONArray guardados = new JSONArray(preferencias(context).getString(clave, "[]"));
            for (int i = 0; i < guardados.length(); i++) {
                JSONObject itinerario = guardados.optJSONObject(i);
                if (itinerario != null) resultado.add(itinerario);
            }
        } catch (Exception e) {
            Log.e("VIAJAPP_OFFLINE", "Error leyendo itinerarios locales", e);
        }
        return resultado;
    }

    public static synchronized JSONObject buscar(Context context, String id) {
        for (JSONObject itinerario : listar(context)) {
            if (id != null && id.equals(itinerario.optString("id"))) return itinerario;
        }
        return null;
    }

    public static synchronized void eliminar(Context context, String id) {
        String clave = claveUsuario();
        if (clave == null || id == null) return;
        JSONArray actualizados = new JSONArray();
        for (JSONObject itinerario : listar(context)) {
            if (!id.equals(itinerario.optString("id"))) actualizados.put(itinerario);
        }
        preferencias(context).edit().putString(clave, actualizados.toString()).apply();
    }
}
