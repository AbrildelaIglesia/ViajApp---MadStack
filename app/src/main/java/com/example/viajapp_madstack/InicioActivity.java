package com.example.viajapp_madstack;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.content.Intent;
import android.provider.Settings;
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
import java.util.ArrayList;
import android.speech.RecognizerIntent;

import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;

public class InicioActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private View contenedorCorreo, contenedorContrasena;
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
    private final IdentityHashMap<ImageView, ColorStateList> tintsOriginales = new IdentityHashMap<>();

    private float escalaTexto = 1.0f;
    private int modoContraste = 0; // 0 = NORMAL, 1 = CREMA, 2 = OSCURO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // LEER LAS PREFERENCIAS AL INICIO PARA EVITAR ESTADOS DESINCRONIZADOS
        modoContraste = PreferenciasAccesibilidad.obtenerContraste(this);
        escalaTexto = PreferenciasAccesibilidad.obtenerEscalaTexto(this);

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
                aplicarTintBotonIcono(btnTogglePassword, modoContraste == 1
                        ? Color.parseColor("#272727")
                        : Color.parseColor("#FFFFFF"));
                etContrasena.setSelection(etContrasena.getText().length());
            });
        }

        // USANDO LA FUNCIÓN GLOBAL: DICTADO CORREO
        if (btnMicCorreo != null) {
            btnMicCorreo.setOnClickListener(v ->
                    VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_CORREO, "Dicta tu correo electrónico")
            );
        }

        // USANDO LA FUNCIÓN GLOBAL: DICTADO CONTRASEÑA
        if (btnMicContrasena != null) {
            btnMicContrasena.setOnClickListener(v ->
                    VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_PASSWORD, "Dicta tu contraseña")
            );
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

                            // REACTIVAR COMPROBACIÓN DEL CORREO PENDIENTE
                            ViajAppApplication aplicacion =
                                    (ViajAppApplication) getApplication();

                            aplicacion.restablecerAvisoSesion();

                            Toast.makeText(InicioActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(InicioActivity.this, AccesibilidadActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(InicioActivity.this, "No se pudo iniciar sesión. Verificá tus datos.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // BOTÓN RECUPERAR CONTRASEÑA
        TextView txtRecuperarContrasena = findViewById(R.id.txtRecuperarContrasena);
        if (txtRecuperarContrasena != null) {
            txtRecuperarContrasena.setOnClickListener(v -> mostrarDialogoRecuperacion());
        }

        // BOTÓN REGISTRO
        txtRegistrarse = findViewById(R.id.txtRegistrarse);
        txtRegistrarse.setOnClickListener(v -> {
            Intent intent = new Intent(InicioActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    // RECUPERACIÓN DE CONTRASEÑA MEDIANTE FIREBASE AUTHENTICATION
    private void mostrarDialogoRecuperacion() {
        EditText inputCorreo = new EditText(this);
        inputCorreo.setHint("Correo electrónico");
        inputCorreo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputCorreo.setSingleLine(true);

        if (etCorreo != null) {
            inputCorreo.setText(etCorreo.getText().toString().trim());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Ingresá tu correo electrónico para recibir un enlace de recuperación.")
                .setView(inputCorreo)
                .setPositiveButton("Enviar correo", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String correo = inputCorreo.getText().toString().trim();
                    if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                        inputCorreo.setError("Ingresá un correo válido");
                        inputCorreo.requestFocus();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(correo)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this,
                                            "Si el correo corresponde a una cuenta, recibirás un enlace de recuperación.",
                                            Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(this,
                                            "No se pudo procesar la solicitud. Intentá nuevamente.",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
        );
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        modoContraste = PreferenciasAccesibilidad.obtenerContraste(this);
        escalaTexto = PreferenciasAccesibilidad.obtenerEscalaTexto(this);

        aplicarTemaContraste();
        actualizarTamanoTextos(scrollContenido);
        actualizarTamanoTextos(barraAccesibilidad);
    }

    // CAPTURAR RESPUESTA DE VOZ USANDO EL PROCESADOR GLOBAL
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> resultado = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (resultado != null && !resultado.isEmpty()) {
                String textoHablado = resultado.get(0);

                if (requestCode == REQUEST_CODE_VOICE_CORREO) {
                    String correoLimpio = VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_CORREO);
                    etCorreo.setText(correoLimpio);
                    etCorreo.setSelection(correoLimpio.length());

                } else if (requestCode == REQUEST_CODE_VOICE_PASSWORD) {
                    String passLimpia = VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_PASSWORD);
                    etContrasena.setText(passLimpia);
                    etContrasena.setSelection(passLimpia.length());
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
        contenedorCorreo = findViewById(R.id.contenedorCorreo);
        contenedorContrasena = findViewById(R.id.contenedorContrasena);

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

        // APLICAR CAMBIOS DE TAMAÑO DINÁMICAMENTE Y GUARDAR PREFERENCIA
        btnAumentarTexto.setOnClickListener(v -> {
            escalaTexto = Math.min(1.3f, Math.round((escalaTexto + 0.1f) * 10f) / 10f);
            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(barraAccesibilidad);
            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        btnDisminuirTexto.setOnClickListener(v -> {
            escalaTexto = Math.max(0.9f, Math.round((escalaTexto - 0.1f) * 10f) / 10f);
            actualizarTamanoTextos(scrollContenido);
            actualizarTamanoTextos(barraAccesibilidad);
            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // CICLAR CONTRASTE Y GUARDAR PREFERENCIA

        btnContraste.setOnClickListener(v -> {
            modoContraste = (modoContraste + 1) % 3;
            aplicarTemaContraste();
            PreferenciasAccesibilidad.guardarContraste(this, modoContraste);
        });


        btnAudio.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "No se pudo abrir la configuración de accesibilidad",
                        Toast.LENGTH_SHORT
                ).show();
            }
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
        if (vista instanceof ImageView) {
            ImageView imagen = (ImageView) vista;
            if (!tintsOriginales.containsKey(imagen)) {
                tintsOriginales.put(imagen, imagen.getImageTintList());
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


        if (modoContraste == 0) {
            btnContraste.setText("◐ Contraste");
        } else if (modoContraste == 1) {
            btnContraste.setText("◐ Crema");
        } else {
            btnContraste.setText("◐ Oscuro");
        }


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

        // Aplicar fondo de campo a los contenedores para no cortar los íconos laterales
        int fondoNormalCampo = Color.parseColor("#FFFFFF");
        int bordeNormalCampo = Color.parseColor("#CBD5E1");

        View[] contenedoresCampos = {contenedorCorreo, contenedorContrasena};
        for (View contenedor : contenedoresCampos) {
            if (contenedor != null) {
                if (normal) {
                    contenedor.setBackground(fondoRedondeado(fondoNormalCampo, bordeNormalCampo, 12));
                } else {
                    contenedor.setBackground(fondoRedondeado(fondoCampo, borde, 12));
                }
            }
        }

        EditText[] campos = {etCorreo, etContrasena};
        for (EditText campo : campos) {
            if (campo != null) {
                campo.setBackground(null);
                if (normal) {
                    restaurarColor(campo);
                    if (pistasOriginales.containsKey(campo)) {
                        campo.setHintTextColor(pistasOriginales.get(campo));
                    }
                } else {
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

        aplicarTintBotonIcono(btnTogglePassword, colorIconos);
        aplicarTintBotonIcono(btnMicCorreo, colorIconos);
        aplicarTintBotonIcono(btnMicContrasena, colorIconos);

        TextView enlace = findViewById(R.id.txtRegistrarse);

        if (enlace != null) {
            if (normal) {
                // NORMAL: color original, sin subrayado
                restaurarColor(enlace);
                enlace.setPaintFlags(
                        enlace.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG
                );
            } else if (crema) {
                // CREMA: marrón original, sin subrayado
                enlace.setTextColor(Color.parseColor("#664700"));
                enlace.setPaintFlags(
                        enlace.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG
                );
            } else {
                // OSCURO: blanco puro y subrayado
                enlace.setTextColor(Color.WHITE);
                enlace.setPaintFlags(
                        enlace.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG
                );
            }
        }

    }

    private void aplicarTintBotonIcono(ImageView boton, int colorModoCremaOOscuro) {
        if (boton == null) return;
        Drawable d = boton.getDrawable();
        if (d != null) {
            d.mutate();
            DrawableCompat.setTintList(d, null); // Limpia cualquier tint previo pegado en el drawable
        }
        if (modoContraste == 0) {
            // Color oscuro visible sobre fondo claro en contraste normal
            boton.setImageTintList(ColorStateList.valueOf(Color.parseColor("#0B1F3A")));
        } else {
            boton.setImageTintList(ColorStateList.valueOf(colorModoCremaOOscuro));
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
        if (modoContraste != 0) {
            DrawableCompat.setTint(icono, color);
        }
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
                .setTitle("¡Bienvenido a ViajAPP!")
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
