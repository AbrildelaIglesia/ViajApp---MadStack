package com.example.viajapp_madstack;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.ArrayList;
import android.speech.RecognizerIntent;
import android.content.ActivityNotFoundException;

import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

public class InicioActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private ImageView btnTogglePassword, btnMicCorreo, btnMicContrasena;
    private TextView txtRegistrarse;
    private LinearLayout btnIniciarSesion;
    private FirebaseAuth mAuth;

    private static final int REQUEST_CODE_VOICE_CORREO = 100;
    private static final int REQUEST_CODE_VOICE_PASSWORD = 101;

    // BARRA DE ACCESIBILIDAD
    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;
    private View pantallaInicio, scrollContenido;

    private final IdentityHashMap<View, Drawable> fondosOriginales = new IdentityHashMap<>();
    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales = new IdentityHashMap<>();
    private final IdentityHashMap<EditText, ColorStateList> pistasOriginales = new IdentityHashMap<>();
    private final IdentityHashMap<TextView, Float> tamanosOriginales = new IdentityHashMap<>();

    private float escalaTexto = 1.0f;
    private int modoContraste = 0; // 0 = NORMAL, 1 = CREMA, 2 = OSCURO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnMicCorreo = findViewById(R.id.btnMicCorreo);
        btnMicContrasena = findViewById(R.id.btnMicContrasena);
        mAuth = FirebaseAuth.getInstance();

        // ACCESIBILIDAD CAMPO CORREO
        ViewCompat.setAccessibilityDelegate(etCorreo, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setHintText(null);
                info.setClassName("android.widget.EditText");
                info.setContentDescription("Correo electrónico, campo de texto obligatorio");
                info.setShowingHintText(false);
            }
        });

        // ACCESIBILIDAD CAMPO CONTRASEÑA
        ViewCompat.setAccessibilityDelegate(etContrasena, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setHintText(null);
                info.setClassName("android.widget.EditText");
                info.setContentDescription("Contraseña, campo de texto seguro, obligatorio");
                info.setShowingHintText(false);
                info.setEditable(true);
            }
        });

        configurarAccesibilidad();
        mostrarBienvenidaInicial();

        // BOTÓN MOSTRAR / OCULTAR CONTRASEÑA
        final boolean[] isPasswordVisible = {false};
        if (etContrasena != null && btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> {
                isPasswordVisible[0] = !isPasswordVisible[0];

                if (isPasswordVisible[0]) {
                    etContrasena.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                    btnTogglePassword.setImageResource(R.drawable.visible_true);
                    btnTogglePassword.setContentDescription("Ocultar contraseña");
                } else {
                    etContrasena.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                    btnTogglePassword.setImageResource(R.drawable.visible_false);
                    btnTogglePassword.setContentDescription("Mostrar contraseña");
                }
                etContrasena.setSelection(etContrasena.getText().length());
            });
        }

        // DICTADO POR VOZ INDIVIDUAL - CORREO
        if (btnMicCorreo != null) {
            btnMicCorreo.setOnClickListener(v -> {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu correo electrónico...");
                try {
                    startActivityForResult(intent, REQUEST_CODE_VOICE_CORREO);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Dictado por voz no disponible.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // DICTADO POR VOZ INDIVIDUAL - CONTRASEÑA
        if (btnMicContrasena != null) {
            btnMicContrasena.setOnClickListener(v -> {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu contraseña...");
                try {
                    startActivityForResult(intent, REQUEST_CODE_VOICE_PASSWORD);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Dictado por voz no disponible.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // BOTÓN INICIAR SESIÓN
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim().replaceAll("\\s+", "");
            String contrasena = etContrasena.getText().toString();

            if (!validarCorreo(correo)) {
                return;
            }

            if (contrasena.isEmpty()) {
                etContrasena.setError("Ingrese su contraseña");
                etContrasena.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(InicioActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(InicioActivity.this, AccesibilidadActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(InicioActivity.this, "No se pudo iniciar sesión. Verificá tus datos.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // BOTÓN REGISTRO
        txtRegistrarse = findViewById(R.id.txtRegistrarse);
        txtRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    // CAPTURAR RESPUESTA DE VOZ Y APLICAR FILTRADO INTELIGENTE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> resultado = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (resultado != null && !resultado.isEmpty()) {
                String textoHablado = resultado.get(0).toLowerCase(); // Todo a minúsculas

                if (requestCode == REQUEST_CODE_VOICE_CORREO) {
                    // TRADUCCIÓN INTELIGENTE DE VOZ A FORMATO EMAIL
                    String correoLimpio = textoHablado
                            .replace(" arroba ", "@")
                            .replace(" a la arroba ", "@")
                            .replace(" punto ", ".")
                            .replaceAll("\\s+", ""); // Borra cualquier espacio sobrante

                    etCorreo.setText(correoLimpio);
                    etCorreo.setSelection(correoLimpio.length());

                } else if (requestCode == REQUEST_CODE_VOICE_PASSWORD) {
                    // Para la contraseña quitamos espacios por si dictó separada
                    String passLimpias = textoHablado.replaceAll("\\s+", "");
                    etContrasena.setText(passLimpias);
                    etContrasena.setSelection(passLimpias.length());
                }
            }
        }
    }

    private boolean validarCorreo(String correo) {
        if (correo.isEmpty()) {
            etCorreo.setError("Ingrese su correo");
            etCorreo.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("El formato de correo es inválido");
            etCorreo.requestFocus();
            return false;
        }
        return true;
    }

    private void configurarAccesibilidad() {
        pantallaInicio = findViewById(R.id.main);
        scrollContenido = findViewById(R.id.scrollContenido);
        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        guardarOriginales(pantallaInicio);

        btnAccesibilidad.setOnClickListener(v -> {
            boolean mostrar = btnDisminuirTexto.getVisibility() != View.VISIBLE;
            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            LinearLayout.LayoutParams parametrosBoton = (LinearLayout.LayoutParams) btnAccesibilidad.getLayoutParams();
            parametrosBoton.width = mostrar ? 0 : LinearLayout.LayoutParams.WRAP_CONTENT;
            parametrosBoton.weight = mostrar ? 1f : 0f;
            btnAccesibilidad.setLayoutParams(parametrosBoton);

            int espacio = dp(12);
            btnAccesibilidad.setPadding(mostrar ? 0 : espacio, 0, mostrar ? 0 : espacio, 0);

            ConstraintLayout.LayoutParams parametrosBarra = (ConstraintLayout.LayoutParams) barraAccesibilidad.getLayoutParams();
            parametrosBarra.width = mostrar ? 0 : ConstraintLayout.LayoutParams.WRAP_CONTENT;
            barraAccesibilidad.setLayoutParams(parametrosBarra);

            btnAccesibilidad.setContentDescription(mostrar ? "Cerrar menú de accesibilidad" : "Abrir menú de accesibilidad");
        });

        btnAumentarTexto.setOnClickListener(v -> {
            escalaTexto = Math.min(1.3f, Math.round((escalaTexto + 0.1f) * 10f) / 10f);
            actualizarTamanoTextos(scrollContenido);
        });

        btnDisminuirTexto.setOnClickListener(v -> {
            escalaTexto = Math.max(0.9f, Math.round((escalaTexto - 0.1f) * 10f) / 10f);
            actualizarTamanoTextos(scrollContenido);
        });

        btnContraste.setOnClickListener(v -> {
            modoContraste = (modoContraste + 1) % 3;
            aplicarTemaContraste();

            String nombreModo = (modoContraste == 0) ? "Normal" : (modoContraste == 1) ? "Crema" : "Oscuro";
            Toast.makeText(this, "Contraste: " + nombreModo, Toast.LENGTH_SHORT).show();
        });
    }

    private void guardarOriginales(View vista) {
        if (vista == null) return;
        if (!fondosOriginales.containsKey(vista)) {
            fondosOriginales.put(vista, vista.getBackground());
        }
        if (vista instanceof TextView) {
            TextView texto = (TextView) vista;
            if (!coloresOriginales.containsKey(texto)) {
                coloresOriginales.put(texto, texto.getTextColors());
                tamanosOriginales.put(texto, texto.getTextSize());
            }
        }
        if (vista instanceof EditText) {
            EditText campo = (EditText) vista;
            if (!pistasOriginales.containsKey(campo)) {
                pistasOriginales.put(campo, campo.getHintTextColors());
            }
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    private void actualizarTamanoTextos(View vista) {
        if (vista instanceof TextView) {
            TextView texto = (TextView) vista;
            Float original = tamanosOriginales.get(texto);
            if (original != null) {
                texto.setTextSize(TypedValue.COMPLEX_UNIT_PX, original * escalaTexto);
            }
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarTamanoTextos(grupo.getChildAt(i));
            }
        }
    }

    private void aplicarTemaContraste() {
        boolean normal = modoContraste == 0;
        boolean crema = modoContraste == 1;

        int fondoPantalla = Color.parseColor(crema ? "#FFF1D6" : "#111111");
        int fondoBarra = Color.parseColor(crema ? "#F6E8CF" : "#181818");
        int fondoControl = Color.parseColor(crema ? "#FFFAF0" : "#252525");
        int fondoCampo = Color.parseColor(crema ? "#F3DFC0" : "#222222");
        int borde = Color.parseColor(crema ? "#A6947F" : "#666666");
        int textoPrincipal = Color.parseColor(crema ? "#222222" : "#FFFFFF");
        int textoSecundario = Color.parseColor(crema ? "#5A5146" : "#D0D0D0");
        int textoPista = Color.parseColor(crema ? "#71665B" : "#BABABA");
        int colorIconos = Color.parseColor(crema ? "#272727" : "#FFFFFF");

        if (normal) {
            restaurarFondo(pantallaInicio);
            restaurarFondo(scrollContenido);
        } else {
            pantallaInicio.setBackgroundColor(fondoPantalla);
            scrollContenido.setBackgroundColor(fondoPantalla);
        }

        actualizarColoresTextos(scrollContenido, textoPrincipal, textoSecundario);

        TextView txtCompatibilidad = findViewById(R.id.txtCompatibilidadLectores);
        if (txtCompatibilidad != null) {
            if (normal) {
                restaurarFondo(txtCompatibilidad);
                restaurarColor(txtCompatibilidad);
            } else {
                txtCompatibilidad.setBackground(fondoRedondeado(fondoCampo, borde, 20));
                txtCompatibilidad.setTextColor(textoPrincipal);
            }
        }

        if (normal) {
            restaurarFondo(barraAccesibilidad);
        } else {
            barraAccesibilidad.setBackground(fondoRedondeado(fondoBarra, borde, 16));
        }

        TextView[] controles = {btnAccesibilidad, btnDisminuirTexto, btnAumentarTexto, btnContraste, btnAudio};
        for (TextView control : controles) {
            if (control != null) {
                if (normal) {
                    restaurarFondo(control);
                    restaurarColor(control);
                } else {
                    control.setBackground(fondoRedondeado(fondoControl, borde, 12));
                    control.setTextColor(textoPrincipal);
                }
            }
        }

        EditText[] campos = {etCorreo, etContrasena};
        for (EditText campo : campos) {
            if (campo != null) {
                if (normal) {
                    restaurarFondo(campo);
                    restaurarColor(campo);
                    if (pistasOriginales.containsKey(campo)) {
                        campo.setHintTextColor(pistasOriginales.get(campo));
                    }
                } else {
                    campo.setBackground(fondoRedondeado(fondoCampo, borde, 12));
                    campo.setTextColor(textoPrincipal);
                    campo.setHintTextColor(textoPista);
                }
            }
        }

        LinearLayout boton = findViewById(R.id.btnIniciarSesion);
        if (boton != null) {
            if (normal) {
                restaurarFondo(boton);
            } else {
                int fondoBoton = Color.parseColor(crema ? "#E9D7B8" : "#303030");
                boton.setBackground(fondoRedondeado(fondoBoton, borde, 20));
            }
            for (int i = 0; i < boton.getChildCount(); i++) {
                View hijo = boton.getChildAt(i);
                if (hijo instanceof TextView) {
                    TextView texto = (TextView) hijo;
                    if (normal) {
                        restaurarColor(texto);
                    } else {
                        texto.setTextColor(crema ? Color.parseColor("#222222") : Color.WHITE);
                    }
                }
            }
        }

        if (etCorreo != null) actualizarIconoInicio(etCorreo, R.drawable.ic_email, colorIconos);
        if (etContrasena != null) actualizarIconoInicio(etContrasena, R.drawable.ic_lock, colorIconos);
        if (btnTogglePassword != null) {
            btnTogglePassword.setImageTintList(ColorStateList.valueOf(colorIconos));
        }
        if (btnMicCorreo != null) {
            btnMicCorreo.setImageTintList(ColorStateList.valueOf(colorIconos));
        }
        if (btnMicContrasena != null) {
            btnMicContrasena.setImageTintList(ColorStateList.valueOf(colorIconos));
        }

        TextView enlace = findViewById(R.id.txtRegistrarse);
        if (enlace != null) {
            if (normal) {
                restaurarColor(enlace);
            } else {
                enlace.setTextColor(crema ? Color.parseColor("#664700") : Color.parseColor("#FFE16B"));
            }
        }
    }

    private void actualizarColoresTextos(View vista, int principal, int secundario) {
        if (vista instanceof TextView && !(vista instanceof EditText)) {
            TextView texto = (TextView) vista;
            ColorStateList original = coloresOriginales.get(texto);
            if (original != null) {
                if (modoContraste == 0) {
                    texto.setTextColor(original);
                } else {
                    int colorAnterior = original.getDefaultColor();
                    boolean esSecundario = colorAnterior == Color.parseColor("#4B5563")
                            || colorAnterior == Color.parseColor("#6B7280")
                            || colorAnterior == Color.parseColor("#52647A");
                    texto.setTextColor(esSecundario ? secundario : principal);
                }
            }
        }
        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarColoresTextos(grupo.getChildAt(i), principal, secundario);
            }
        }
    }

    private void restaurarFondo(View vista) {
        if (vista != null && fondosOriginales.containsKey(vista)) {
            vista.setBackground(fondosOriginales.get(vista));
        }
    }

    private void restaurarColor(TextView texto) {
        if (texto != null) {
            ColorStateList original = coloresOriginales.get(texto);
            if (original != null) {
                texto.setTextColor(original);
            }
        }
    }

    private void actualizarIconoInicio(EditText campo, int recurso, int color) {
        if (campo == null) return;
        Drawable icono = ContextCompat.getDrawable(this, recurso);
        if (icono == null) return;
        icono = icono.mutate();
        int colorFinal = modoContraste == 0 ? Color.parseColor("#111827") : color;
        DrawableCompat.setTint(icono, colorFinal);
        Drawable[] actuales = campo.getCompoundDrawablesRelative();
        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(icono, actuales[1], actuales[2], actuales[3]);
    }

    private Drawable fondoRedondeado(int color, int borde, int radioDp) {
        GradientDrawable forma = new GradientDrawable();
        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(color);
        forma.setCornerRadius(dp(radioDp));
        forma.setStroke(dp(1), borde);
        return forma;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void mostrarBienvenidaInicial() {
        SharedPreferences preferencias = getSharedPreferences("ViajAppPreferencias", MODE_PRIVATE);
        boolean bienvenidaVista = preferencias.getBoolean("bienvenidaVista", false);

        if (bienvenidaVista) return;

        new AlertDialog.Builder(this)
                .setTitle("¡Bienvenido a ViajAPP! 🌍")
                .setMessage("Tu próxima aventura comienza acá.\n\nEn ViajAPP queremos que todos puedan viajar sin barreras.\n\n¿Querés activar la asistencia por voz?\n\nPodrás escuchar las indicaciones de la aplicación y completar formularios mediante dictado.")
                .setPositiveButton("Activar asistencia por voz", (dialogo, opcion) -> {
                    preferencias.edit().putBoolean("bienvenidaVista", true).putBoolean("asistenciaVozSolicitada", true).apply();
                    Toast.makeText(this, "Preferencia guardada.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Ahora no, continuar", (dialogo, opcion) -> {
                    preferencias.edit().putBoolean("bienvenidaVista", true).putBoolean("asistenciaVozSolicitada", false).apply();
                })
                .setCancelable(false)
                .show();
    }
}