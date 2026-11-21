package com.example.viajapp_madstack;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.speech.RecognizerIntent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOICE_NOMBRE = 200;
    private static final int REQUEST_CODE_VOICE_CORREO = 201;
    private static final int REQUEST_CODE_VOICE_PASSWORD = 202;
    private static final int REQUEST_CODE_VOICE_CONF_PASSWORD = 203;

    // CAMPOS DEL FORMULARIO
    private EditText etNombreApellidos, etCorreo,
            etContrasena, etConfirmarContrasena;

    // CONTENEDORES DE CAMPOS DEL FORMULARIO
    private View contenedorNombre, contenedorCorreo,
            contenedorContrasena, contenedorConfirmarContrasena;

    // BOTONES DE VISIBILIDAD DE CONTRASEÑA (OJITOS)
    private ImageView btnTogglePassword, btnToggleConfPassword;

    // BOTONES DE MICRÓFONO
    private ImageView btnMicNombre, btnMicCorreo,
            btnMicContrasena, btnMicConfirmacionContrasena;

    // CASILLAS DE ACCESIBILIDAD Y TÉRMINOS
    private CheckBox cbSillaRuedas, cbRecorridosCortos,
            cbVisionReducida,
            cbSinAdaptaciones, cbTerminos;

    // MENSAJES VISIBLES DE VALIDACIÓN DE CASILLAS
    private TextView txtErrorAccesibilidad, txtErrorTerminos;

    private AppCompatButton btnCrearCuenta;

    // BARRA DE ACCESIBILIDAD
    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;
    private ConstraintLayout pantallaRegistro;
    private Drawable fondoOriginalRegistro;

    // FIREBASE
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // EVITAR REGISTROS DUPLICADOS
    private boolean registrando = false;

    // AUMENTAR Y DISMINUIR TEXTO
    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private float escalaTexto = 1.0f;

    // CONTRASTE:
    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    // GUARDAR LOS ESTILOS ORIGINALES
    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<EditText, ColorStateList> pistasOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<CheckBox, ColorStateList> coloresOriginalesCasillas =
            new IdentityHashMap<>();

    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registro);

        // CONECTAR CAMPOS
        etNombreApellidos = findViewById(R.id.etNombreApellidos);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena);

        // CONECTAR CONTENEDORES DE CAMPOS
        contenedorNombre = findViewById(R.id.contenedorNombre);
        contenedorCorreo = findViewById(R.id.contenedorCorreo);
        contenedorContrasena = findViewById(R.id.contenedorContrasena);
        contenedorConfirmarContrasena = findViewById(R.id.contenedorConfirmarContrasena);

        // CONECTAR TOGGLES DE CONTRASEÑA
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfPassword = findViewById(R.id.btnToggleConfPassword);

        // CONECTAR MICRÓFONOS
        btnMicNombre = findViewById(R.id.btnMicNombre);
        btnMicCorreo = findViewById(R.id.btnMicCorreo);
        btnMicContrasena = findViewById(R.id.btnMicContrasena);
        btnMicConfirmacionContrasena = findViewById(R.id.btnMicConfirmacionContrasena);

        // CONECTAR CASILLAS
        cbSillaRuedas = findViewById(R.id.cbSillaRuedas);
        cbRecorridosCortos = findViewById(R.id.cbRecorridosCortos);
        cbVisionReducida = findViewById(R.id.cbVisionReducida);
        cbSinAdaptaciones = findViewById(R.id.cbSinAdaptaciones);
        cbTerminos = findViewById(R.id.cbTerminos);

        txtErrorAccesibilidad = findViewById(R.id.txtErrorAccesibilidad);
        txtErrorTerminos = findViewById(R.id.txtErrorTerminos);

        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        // CONECTAR BARRA DE ACCESIBILIDAD
        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);
        pantallaRegistro = findViewById(R.id.pantallaRegistro);

        // GUARDAR EL FONDO ORIGINAL
        fondoOriginalRegistro = pantallaRegistro.getBackground();

        // INICIALIZAR FIREBASE
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // CONFIGURAR PREFERENCIAS
        configurarAccesibilidad();
        configurarListenersContrasenasYMicros();

        // ABRIR Y CERRAR EL MENÚ
        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility() != View.VISIBLE;

            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            // AJUSTAR EL BOTÓN PRINCIPAL
            LinearLayout.LayoutParams parametrosBoton =
                    (LinearLayout.LayoutParams)
                            btnAccesibilidad.getLayoutParams();

            parametrosBoton.width = mostrar
                    ? 0
                    : LinearLayout.LayoutParams.WRAP_CONTENT;

            parametrosBoton.weight = mostrar ? 1f : 0f;

            btnAccesibilidad.setLayoutParams(parametrosBoton);

            int espacio = dp(12);

            btnAccesibilidad.setPadding(
                    mostrar ? 0 : espacio,
                    0,
                    mostrar ? 0 : espacio,
                    0
            );

            // DEJAR COMPACTA LA BARRA AL CERRAR
            ConstraintLayout.LayoutParams parametrosBarra =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = mostrar
                    ? 0
                    : ConstraintLayout.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(parametrosBarra);

            // ACCESIBILIDAD CON TALKBACK
            btnAccesibilidad.setContentDescription(
                    mostrar
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // BOTÓN A+
        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(
                    findViewById(R.id.scrollContenido)
            );

            actualizarTamanoTextos(barraAccesibilidad);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // BOTÓN A-
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(
                    findViewById(R.id.scrollContenido)
            );

            actualizarTamanoTextos(barraAccesibilidad);

            PreferenciasAccesibilidad.guardarEscalaTexto(this, escalaTexto);
        });

        // BOTÓN CONTRASTE
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

        cbVisionReducida.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                preguntarPorTalkBack();
            }
        });

        // BOTÓN CREAR CUENTA
        btnCrearCuenta.setOnClickListener(v -> {

            if (registrando) {
                return;
            }

            String nombre = etNombreApellidos.getText()
                    .toString()
                    .trim()
                    .replaceAll(" +", " ");

            nombre = formatearNombre(nombre);
            etNombreApellidos.setText(nombre);

            String correo = etCorreo.getText()
                    .toString()
                    .trim();

            String contrasena =
                    etContrasena.getText().toString();

            String confirmarContrasena =
                    etConfirmarContrasena.getText().toString();

            if (!validarNombre(nombre)) return;
            if (!validarCorreo(correo)) return;
            if (!validarContrasena(contrasena)) return;
            if (!validarConfirmacion(contrasena, confirmarContrasena)) return;
            if (!validarAccesibilidad()) return;
            if (!validarTerminos()) return;

            crearCuentaFirebase(nombre, correo, contrasena);
        });


        TextView txtLeerTerminos = findViewById(R.id.txtLeerTerminos);

        String texto = "Acepto los términos de servicio y las políticas de turismo accesible. ";
        String enlace = "Leer términos y condiciones";

        android.text.SpannableString contenido =
                new android.text.SpannableString(texto + enlace);

        int inicio = texto.length();
        int fin = contenido.length();

        contenido.setSpan(
                new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        mostrarTerminos();
                    }

                    @Override
                    public void updateDrawState(android.text.TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(
                                modoContraste == 1
                                        ? Color.parseColor("#664700")
                                        : modoContraste == 2
                                          ? Color.WHITE
                                          : Color.parseColor("#2563EB")
                        );
                        ds.setUnderlineText(true);
                    }
                },
                inicio, fin,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        txtLeerTerminos.setText(contenido);
        txtLeerTerminos.setMovementMethod(
                android.text.method.LinkMovementMethod.getInstance()
        );
        txtLeerTerminos.setHighlightColor(Color.TRANSPARENT);


        // IR A LA PANTALLA DE INICIO DE SESIÓN
        TextView txtIniciarSesion = findViewById(R.id.txtIniciarSesion);

        txtIniciarSesion.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RegistroActivity.this,
                    InicioActivity.class
            );

            startActivity(intent);
            finish();

        });
    }

    // ===================================================
    // CONFIGURACIÓN DE MOSTRAR/OCULTAR Y MICRÓFONOS
    // ===================================================

    private void mostrarTerminos() {
        try (java.io.BufferedReader lector =
                     new java.io.BufferedReader(
                             new java.io.InputStreamReader(
                                     getResources().openRawResource(
                                             R.raw.terminos_viajapp
                                     ),
                                     java.nio.charset.StandardCharsets.UTF_8
                             )
                     )) {

            StringBuilder contenido = new StringBuilder();
            String linea;

            while ((linea = lector.readLine()) != null) {
                contenido.append(linea).append("\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Términos y condiciones de ViajAPP")
                    .setMessage(contenido.toString())
                    .setPositiveButton("Cerrar", null)
                    .show();

        } catch (java.io.IOException e) {
            Toast.makeText(
                    this,
                    "No se pudieron abrir los términos",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void preguntarPorTalkBack() {
        new AlertDialog.Builder(this)
                .setTitle("Dificultad visual")
                .setMessage("¿Querés abrir los ajustes de Android para configurar TalkBack? "
                        + "Es opcional y no modifica tu preferencia de viaje.")
                .setPositiveButton("Configurar TalkBack", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "No se pudo abrir la configuración de accesibilidad",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Ahora no", null)
                .show();
    }

    private void configurarListenersContrasenasYMicros() {
        // MOSTRAR / OCULTAR CONTRASEÑA PRINCIPAL
        btnTogglePassword.setOnClickListener(v -> {
            boolean esVisible = etContrasena.getTransformationMethod() == null;
            if (esVisible) {
                etContrasena.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePassword.setImageResource(R.drawable.visible_false);
                btnTogglePassword.setContentDescription("Mostrar contraseña");
            } else {
                etContrasena.setTransformationMethod(null);
                btnTogglePassword.setImageResource(R.drawable.visible_true);
                btnTogglePassword.setContentDescription("Ocultar la contraseña");
            }
            aplicarTintIndividual(btnTogglePassword, modoContraste == 1
                    ? Color.parseColor("#272727")
                    : Color.parseColor("#FFFFFF"));
            etContrasena.setSelection(etContrasena.getText().length());
        });

        // MOSTRAR / OCULTAR CONFIRMACIÓN DE CONTRASEÑA
        btnToggleConfPassword.setOnClickListener(v -> {
            boolean esVisible = etConfirmarContrasena.getTransformationMethod() == null;
            if (esVisible) {
                etConfirmarContrasena.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleConfPassword.setImageResource(R.drawable.visible_false);
                btnToggleConfPassword.setContentDescription("Mostrar confirmación de contraseña");
            } else {
                etConfirmarContrasena.setTransformationMethod(null);
                btnToggleConfPassword.setImageResource(R.drawable.visible_true);
                btnToggleConfPassword.setContentDescription("Ocultar confirmación de contraseña");
            }
            aplicarTintIndividual(btnToggleConfPassword, modoContraste == 1
                    ? Color.parseColor("#272727")
                    : Color.parseColor("#FFFFFF"));
            etConfirmarContrasena.setSelection(etConfirmarContrasena.getText().length());
        });

        // FUNCIONALIDAD DE ACCESIBILIDAD PARA MICRÓFONOS
        btnMicNombre.setOnClickListener(v ->
                VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_NOMBRE, "Dicta tu nombre y apellidos")
        );

        btnMicCorreo.setOnClickListener(v ->
                VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_CORREO, "Dicta tu correo electrónico")
        );

        btnMicContrasena.setOnClickListener(v ->
                VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_PASSWORD, "Dicta tu contraseña")
        );

        btnMicConfirmacionContrasena.setOnClickListener(v ->
                VoiceUtils.iniciarDictado(this, REQUEST_CODE_VOICE_CONF_PASSWORD, "Dicta la confirmación de contraseña")
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        modoContraste =
                PreferenciasAccesibilidad.obtenerContraste(this);

        escalaTexto =
                PreferenciasAccesibilidad.obtenerEscalaTexto(this);

        aplicarTemaContraste();

        actualizarTamanoTextos(
                findViewById(R.id.scrollContenido)
        );

        actualizarTamanoTextos(barraAccesibilidad);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> resultado = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (resultado != null && !resultado.isEmpty()) {
                String textoHablado = resultado.get(0);

                if (requestCode == REQUEST_CODE_VOICE_NOMBRE) {
                    String nombreLimpio = formatearNombre(
                            VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_TEXTO_GENERAL)
                    );
                    etNombreApellidos.setText(nombreLimpio);
                    etNombreApellidos.setSelection(nombreLimpio.length());

                } else if (requestCode == REQUEST_CODE_VOICE_CORREO) {
                    String correoLimpio = VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_CORREO);
                    etCorreo.setText(correoLimpio);
                    etCorreo.setSelection(correoLimpio.length());

                } else if (requestCode == REQUEST_CODE_VOICE_PASSWORD) {
                    String passLimpia = VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_PASSWORD);
                    etContrasena.setText(passLimpia);
                    etContrasena.setSelection(passLimpia.length());

                } else if (requestCode == REQUEST_CODE_VOICE_CONF_PASSWORD) {
                    String confPassLimpia = VoiceUtils.procesarTextoDictado(textoHablado, VoiceUtils.TIPO_PASSWORD);
                    etConfirmarContrasena.setText(confPassLimpia);
                    etConfirmarContrasena.setSelection(confPassLimpia.length());
                }
            }
        }
    }

    // ===================================================
    // AUMENTAR Y DISMINUIR TAMAÑO DE TEXTOS
    // ===================================================

    private void actualizarTamanoTextos(View vista) {

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            Float tamanoOriginal = tamanosOriginales.get(texto);

            if (tamanoOriginal == null) {
                tamanoOriginal = texto.getTextSize();
                tamanosOriginales.put(texto, tamanoOriginal);
            }

            texto.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    tamanoOriginal * escalaTexto
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarTamanoTextos(grupo.getChildAt(i));
            }
        }
    }

    // ===================================================
    // CONTRASTE: NORMAL, CREMA Y OSCURO
    // ===================================================

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

        int fondoPantalla = Color.parseColor(
                crema ? "#FFF1D6" : "#111111"
        );

        int fondoBarra = Color.parseColor(
                crema ? "#F6E8CF" : "#181818"
        );

        int fondoControl = Color.parseColor(
                crema ? "#FFFAF0" : "#252525"
        );

        int fondoCampo = Color.parseColor(
                crema ? "#F3DFC0" : "#222222"
        );

        int borde = Color.parseColor(
                crema ? "#A6947F" : "#666666"
        );

        int textoPrincipal = Color.parseColor(
                crema ? "#222222" : "#FFFFFF"
        );

        int textoSecundario = Color.parseColor(
                crema ? "#5A5146" : "#D0D0D0"
        );

        int textoPista = Color.parseColor(
                crema ? "#71665B" : "#BABABA"
        );

        int fondoBoton = Color.parseColor(
                crema ? "#E9D7B8" : "#111111"
        );

        int fondoBotonPresionado = Color.parseColor(
                crema ? "#D8C5A8" : "#303030"
        );

        int colorIconos = Color.parseColor(
                crema ? "#272727" : "#FFFFFF"
        );

        // 1. FONDO GENERAL
        if (normal) {
            pantallaRegistro.setBackground(fondoOriginalRegistro);
        } else {
            pantallaRegistro.setBackgroundColor(fondoPantalla);
        }

        // 2. TODOS LOS TEXTOS DEL FORMULARIO
        actualizarColoresTextos(
                findViewById(R.id.scrollContenido),
                textoPrincipal,
                textoSecundario
        );

        // 3. CASILLAS
        actualizarColoresCasillas();

        // 4. BARRA SUPERIOR
        guardarFondoOriginal(barraAccesibilidad);

        if (normal) {
            restaurarFondo(barraAccesibilidad);
        } else {
            barraAccesibilidad.setBackground(
                    fondoRedondeado(fondoBarra, borde, 16, 1)
            );
        }

        // 5. BOTONES A+, A-, CONTRASTE Y AUDIO
        TextView[] controles = {
                btnAccesibilidad,
                btnDisminuirTexto,
                btnAumentarTexto,
                btnContraste,
                btnAudio
        };

        for (TextView control : controles) {

            guardarFondoOriginal(control);
            guardarColorOriginal(control);

            if (normal) {
                restaurarFondo(control);
                control.setTextColor(coloresOriginales.get(control));
            } else {
                control.setBackground(
                        fondoRedondeado(fondoControl, borde, 12, 1)
                );
                control.setTextColor(textoPrincipal);
            }
        }

        // 6. BOTÓN CREAR CUENTA
        guardarFondoOriginal(btnCrearCuenta);
        guardarColorOriginal(btnCrearCuenta);

        if (normal) {
            restaurarFondo(btnCrearCuenta);
            btnCrearCuenta.setTextColor(coloresOriginales.get(btnCrearCuenta));
        } else {
            btnCrearCuenta.setBackground(
                    fondoBotonTema(
                            fondoBoton,
                            fondoBotonPresionado,
                            crema ? borde : Color.WHITE
                    )
            );
            btnCrearCuenta.setTextColor(
                    crema ? Color.parseColor("#222222") : Color.WHITE
            );
        }

        // 7. CONTENEDORES Y CAMPOS DEL FORMULARIO
        int fondoNormalCampo = Color.parseColor("#FFFFFF");
        int bordeNormalCampo = Color.parseColor("#CBD5E1"); // O "#111827" si lo querés bien marcado

        View[] contenedoresCampos = {
                contenedorNombre,
                contenedorCorreo,
                contenedorContrasena,
                contenedorConfirmarContrasena
        };

        for (View contenedor : contenedoresCampos) {
            if (contenedor != null) {
                if (normal) {
                    contenedor.setBackground(fondoCampoTema(fondoNormalCampo, bordeNormalCampo));
                } else {
                    contenedor.setBackground(fondoCampoTema(fondoCampo, borde));
                }
            }
        }

        EditText[] campos = {
                etNombreApellidos,
                etCorreo,
                etContrasena,
                etConfirmarContrasena
        };

        for (EditText campo : campos) {
            if (campo != null) {
                campo.setBackground(null);
                guardarColorOriginal(campo);

                if (!pistasOriginales.containsKey(campo)) {
                    pistasOriginales.put(
                            campo,
                            campo.getHintTextColors()
                    );
                }

                if (normal) {
                    campo.setTextColor(coloresOriginales.get(campo));
                    campo.setHintTextColor(pistasOriginales.get(campo));
                } else {
                    campo.setTextColor(textoPrincipal);
                    campo.setHintTextColor(textoPista);
                }
            }
        }

        // 8. ICONOS VECTORIALES
        actualizarIconosContraste(
                pantallaRegistro,
                colorIconos
        );

        // 9. ICONOS DE LOS CAMPOS
        actualizarIconoInicio(etNombreApellidos, R.drawable.ic_person, colorIconos);
        actualizarIconoInicio(etCorreo, R.drawable.ic_email, colorIconos);
        actualizarIconoInicio(etContrasena, R.drawable.ic_lock, colorIconos);
        actualizarIconoInicio(etConfirmarContrasena, R.drawable.ic_lock, colorIconos);

        // 10. FLECHA DEL BOTÓN CREAR CUENTA
        actualizarIconoFinal(
                btnCrearCuenta,
                R.drawable.ic_arrow_forward,
                crema ? Color.parseColor("#222222") : Color.WHITE
        );

        // 11. OJITOS DE LAS CONTRASEÑAS (TINTES)
        aplicarTintIndividual(btnTogglePassword, colorIconos);
        aplicarTintIndividual(btnToggleConfPassword, colorIconos);


        // 12. ÍCONOS DECORATIVOS DEL REGISTRO
        int[] iconosRegistro = {
                R.id.imgEscudoRegistro,
                R.id.imgSillaRegistro,
                R.id.imgRecorridosRegistro,
                R.id.imgVisionRegistro,
                R.id.imgSinAdaptacionesRegistro,
                R.id.imgPrivacidadRegistro
        };

        int[] dibujosRegistro = {
                R.drawable.ic_shield_check,
                R.drawable.ic_wheelchair,
                R.drawable.ic_walk,
                R.drawable.ic_visibility,
                R.drawable.ic_travel_explore,
                R.drawable.ic_privacy
        };

        int colorDecorativo;

        if (modoContraste == 0) {
            colorDecorativo = Color.parseColor("#111827");
        } else if (modoContraste == 1) {
            colorDecorativo = Color.parseColor("#272727");
        } else {
            colorDecorativo = Color.WHITE;
        }

        for (int i = 0; i < iconosRegistro.length; i++) {
            ImageView imagen = findViewById(iconosRegistro[i]);

            if (imagen != null) {
                Drawable dibujo = ContextCompat.getDrawable(
                        this, dibujosRegistro[i]
                );

                if (dibujo != null) {
                    imagen.setImageTintList(null);
                    imagen.setImageDrawable(dibujo.mutate());
                    imagen.setImageTintList(
                            ColorStateList.valueOf(colorDecorativo)
                    );
                }
            }
        }

    }

    private void aplicarTintIndividual(ImageView boton, int colorModoCremaOOscuro) {
        if (boton == null) return;
        Drawable d = boton.getDrawable();
        if (d != null) {
            d.mutate();
            DrawableCompat.setTintList(d, null);
        }
        int colorFinal = (modoContraste == 0)
                ? Color.parseColor("#111827")
                : colorModoCremaOOscuro;
        boton.setImageTintList(ColorStateList.valueOf(colorFinal));
    }

    // ===================================================
    // TEXTOS SEGÚN EL CONTRASTE
    // ===================================================

    private void actualizarColoresTextos(
            View vista,
            int principal,
            int secundario) {

        if (vista instanceof TextView
                && !(vista instanceof EditText)
                && !(vista instanceof CheckBox)
                && vista != btnCrearCuenta) {

            TextView texto = (TextView) vista;

            guardarColorOriginal(texto);

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (modoContraste == 0) {
                texto.setTextColor(original);

                if (texto.getId() == R.id.txtIniciarSesion) {
                    texto.setPaintFlags(
                            texto.getPaintFlags()
                                    & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    );
                }
            } else if (texto.getId() == R.id.txtErrorAccesibilidad
                    || texto.getId() == R.id.txtErrorTerminos) {
                texto.setTextColor(Color.parseColor("#D32F2F"));

            } else if (texto.getId() == R.id.txtIniciarSesion) {

                texto.setTextColor(
                        modoContraste == 1
                                ? Color.parseColor("#664700")
                                : Color.WHITE
                );

                if (modoContraste == 2) {
                    texto.setPaintFlags(
                            texto.getPaintFlags()
                                    | android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    );
                } else {
                    texto.setPaintFlags(
                            texto.getPaintFlags()
                                    & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    );
                }

            } else {
                int colorAnterior = original.getDefaultColor();
                boolean esSecundario =
                        colorAnterior == Color.parseColor("#6B7280")
                                || colorAnterior == Color.parseColor("#4B5563");

                texto.setTextColor(
                        esSecundario ? secundario : principal
                );
            }
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarColoresTextos(
                        grupo.getChildAt(i),
                        principal,
                        secundario
                );
            }
        }
    }

    // ===================================================
    // COLORES DE LAS CASILLAS
    // ===================================================

    private void actualizarColoresCasillas() {

        CheckBox[] casillas = {
                cbSillaRuedas,
                cbRecorridosCortos,
                cbVisionReducida,
                cbSinAdaptaciones,
                cbTerminos
        };

        for (CheckBox casilla : casillas) {

            if (!coloresOriginalesCasillas.containsKey(casilla)) {
                coloresOriginalesCasillas.put(
                        casilla,
                        casilla.getButtonTintList()
                );
            }

            if (modoContraste == 0) {
                casilla.setButtonTintList(
                        coloresOriginalesCasillas.get(casilla)
                );
            } else {
                int marcado = Color.parseColor(
                        modoContraste == 1
                                ? "#785500"
                                : "#FFE16B"
                );

                int desmarcado = Color.parseColor(
                        modoContraste == 1
                                ? "#252017"
                                : "#F1F1F1"
                );

                ColorStateList tintes = new ColorStateList(
                        new int[][] {
                                new int[] { android.R.attr.state_checked },
                                new int[] {}
                        },
                        new int[] { marcado, desmarcado }
                );

                casilla.setButtonTintList(tintes);
            }
        }
    }

    // ===================================================
    // CONSERVAR Y RESTAURAR EL ASPECTO ORIGINAL
    // ===================================================

    private void guardarFondoOriginal(View vista) {
        if (vista != null && !fondosOriginales.containsKey(vista)) {
            fondosOriginales.put(vista, vista.getBackground());
        }
    }

    private void restaurarFondo(View vista) {
        if (vista != null && fondosOriginales.containsKey(vista)) {
            vista.setBackground(fondosOriginales.get(vista));
        }
    }

    private void guardarColorOriginal(TextView texto) {
        if (texto != null && !coloresOriginales.containsKey(texto)) {
            coloresOriginales.put(texto, texto.getTextColors());
        }
    }

    // ===================================================
    // ICONOS DE LA PANTALLA
    // ===================================================

    private void actualizarIconosContraste(
            View vista,
            int colorIconos) {

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;
            CharSequence descripcion = imagen.getContentDescription();
            int recurso = 0;

            if (descripcion != null) {
                switch (descripcion.toString()) {
                    case "Escudo de accesibilidad":
                        recurso = R.drawable.ic_shield_check;
                        break;
                    case "Silla de ruedas":
                        recurso = R.drawable.ic_wheelchair;
                        break;
                    case "Recorridos cortos":
                        recurso = R.drawable.ic_walk;
                        break;
                    case "Visión reducida":
                        recurso = R.drawable.ic_visibility;
                        break;
                    case "Accesibilidad auditiva":
                        recurso = R.drawable.ic_hearing;
                        break;
                    case "Sin adaptaciones específicas":
                        recurso = R.drawable.ic_travel_explore;
                        break;
                    case "Dictar nombre y apellidos":
                    case "Dictar correo electrónico":
                    case "Dictar contraseña":
                    case "Dictar la confirmación de contraseña":
                    case "Micrófono":
                        recurso = R.drawable.ic_mic;
                        break;
                    case "Privacidad":
                        recurso = R.drawable.ic_privacy;
                        break;
                }
            }

            if (recurso != 0) {
                imagen.setImageTintList(null);
                imagen.setImageResource(recurso);
                Drawable d = imagen.getDrawable();
                if (d != null) {
                    d.mutate();
                    DrawableCompat.setTintList(d, null);
                }

                int colorFinal = (modoContraste == 0) ? Color.parseColor("#111827") : colorIconos;
                imagen.setImageTintList(ColorStateList.valueOf(colorFinal));
            }
        }

        if (vista instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) vista;
            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarIconosContraste(grupo.getChildAt(i), colorIconos);
            }
        }
    }

    private void actualizarIconoInicio(
            EditText campo,
            int recurso,
            int color) {

        if (campo == null) return;
        Drawable icono = obtenerIconoTema(recurso, color);
        Drawable[] actuales = campo.getCompoundDrawablesRelative();

        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                icono,
                actuales[1],
                actuales[2],
                actuales[3]
        );
    }

    private void actualizarIconoFinal(
            TextView boton,
            int recurso,
            int color) {

        if (boton == null) return;
        Drawable icono = obtenerIconoTema(recurso, color);
        Drawable[] actuales = boton.getCompoundDrawablesRelative();

        boton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                actuales[0],
                actuales[1],
                icono,
                actuales[3]
        );
    }

    private Drawable obtenerIconoTema(
            int recurso,
            int color) {

        Drawable icono = ContextCompat.getDrawable(this, recurso);
        if (icono != null) {
            icono = icono.mutate();
            DrawableCompat.setTintList(icono, null);
            int colorAplicar = (modoContraste == 0)
                    ? Color.parseColor("#111827")
                    : color;
            DrawableCompat.setTint(icono, colorAplicar);
        }
        return icono;
    }

    // ===================================================
    // FONDOS REDONDEADOS SEGÚN EL CONTRASTE
    // ===================================================

    private Drawable fondoRedondeado(
            int colorFondo,
            int colorBorde,
            int radioDp,
            int grosorDp) {

        GradientDrawable forma = new GradientDrawable();
        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(colorFondo);
        forma.setCornerRadius(dp(radioDp));
        forma.setStroke(dp(grosorDp), colorBorde);
        return forma;
    }

    private Drawable fondoCampoTema(
            int colorFondo,
            int colorBorde) {

        StateListDrawable estados = new StateListDrawable();

        estados.addState(
                new int[] { android.R.attr.state_focused },
                fondoRedondeado(colorFondo, colorBorde, 12, 2)
        );

        estados.addState(
                new int[] {},
                fondoRedondeado(colorFondo, colorBorde, 12, 1)
        );

        return estados;
    }

    private Drawable fondoBotonTema(
            int normal,
            int presionado,
            int borde) {

        StateListDrawable estados = new StateListDrawable();

        estados.addState(
                new int[] { android.R.attr.state_pressed },
                fondoRedondeado(presionado, borde, 28, 2)
        );

        estados.addState(
                new int[] {},
                fondoRedondeado(normal, borde, 28, 2)
        );

        return estados;
    }

    private int dp(int valor) {
        return Math.round(
                valor * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    // ===================================================
    // VALIDACIÓN DE NOMBRE Y APELLIDO
    // ===================================================

    private boolean validarNombre(String nombre) {

        if (nombre.isEmpty()) {
            etNombreApellidos.setError("Ingrese su nombre y apellido");
            etNombreApellidos.requestFocus();
            return false;
        }

        if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+")) {
            etNombreApellidos.setError("El nombre y apellido solo pueden contener letras");
            etNombreApellidos.requestFocus();
            return false;
        }

        if (nombre.length() < 2 || nombre.length() > 100) {
            etNombreApellidos.setError("El nombre y apellido deben tener entre 2 y 100 caracteres");
            etNombreApellidos.requestFocus();
            return false;
        }

        if (nombre.split(" ").length < 2) {
            etNombreApellidos.setError("Ingrese su nombre y al menos un apellido");
            etNombreApellidos.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // FORMATEAR NOMBRE Y APELLIDO
    // ===================================================

    private String formatearNombre(String nombre) {

        String[] palabras = nombre.toLowerCase().split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                if (resultado.length() > 0) {
                    resultado.append(" ");
                }

                if (palabra.equals("de")
                        || palabra.equals("del")
                        || palabra.equals("la")
                        || palabra.equals("las")
                        || palabra.equals("los")) {
                    resultado.append(palabra);
                } else {
                    resultado.append(
                            palabra.substring(0, 1).toUpperCase()
                    ).append(palabra.substring(1));
                }
            }
        }

        return resultado.toString();
    }

    // ===================================================
    // VALIDACIÓN DE CORREO ELECTRÓNICO
    // ===================================================

    private boolean validarCorreo(String correo) {

        if (correo.isEmpty()) {
            etCorreo.setError("Ingrese su correo electrónico");
            etCorreo.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Ingrese un correo electrónico válido");
            etCorreo.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // VALIDACIÓN DE CONTRASEÑA
    // ===================================================

    private boolean validarContrasena(String contrasena) {

        if (contrasena.isEmpty()) {
            etContrasena.setError("Ingrese una contraseña");
            etContrasena.requestFocus();
            return false;
        }

        if (contrasena.length() < 8) {
            etContrasena.setError("La contraseña debe tener al menos 8 caracteres");
            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[A-Z].*")) {
            etContrasena.setError("Debe contener al menos una letra mayúscula");
            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[a-z].*")) {
            etContrasena.setError("Debe contener al menos una letra minúscula");
            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[0-9].*")) {
            etContrasena.setError("Debe contener al menos un número");
            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[^a-zA-Z0-9].*")) {
            etContrasena.setError("Debe contener al menos un carácter especial");
            etContrasena.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // CONFIRMAR CONTRASEÑA
    // ===================================================

    private boolean validarConfirmacion(
            String contrasena,
            String confirmarContrasena) {

        if (confirmarContrasena.isEmpty()) {
            etConfirmarContrasena.setError("Confirme su contraseña");
            etConfirmarContrasena.requestFocus();
            return false;
        }

        if (!contrasena.equals(confirmarContrasena)) {
            etConfirmarContrasena.setError("Las contraseñas no coinciden");
            etConfirmarContrasena.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // VALIDAR PREFERENCIAS DE ACCESIBILIDAD
    // ===================================================

    private boolean validarAccesibilidad() {

        boolean seleccionada = cbSillaRuedas.isChecked()
                || cbRecorridosCortos.isChecked()
                || cbVisionReducida.isChecked()
                || cbSinAdaptaciones.isChecked();

        txtErrorAccesibilidad.setVisibility(seleccionada ? View.GONE : View.VISIBLE);

        if (!seleccionada) {
            cbSillaRuedas.requestFocus();
            txtErrorAccesibilidad.announceForAccessibility(
                    "Seleccioná al menos una opción de accesibilidad");
            return false;
        }

        cbSillaRuedas.setError(null);
        return true;
    }

    // ===================================================
    // VALIDAR TÉRMINOS Y CONDICIONES
    // ===================================================

    private boolean validarTerminos() {

        boolean aceptados = cbTerminos.isChecked();
        txtErrorTerminos.setVisibility(aceptados ? View.GONE : View.VISIBLE);

        if (!aceptados) {
            cbTerminos.requestFocus();
            txtErrorTerminos.announceForAccessibility(
                    "Debés aceptar los términos y condiciones");
            Toast.makeText(
                    this,
                    "Debe aceptar los términos y condiciones",
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        cbTerminos.setError(null);
        return true;
    }

    // ===================================================
    // CONFIGURAR CASILLAS DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        cbSinAdaptaciones.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        cbSillaRuedas.setChecked(false);
                        cbRecorridosCortos.setChecked(false);
                        cbVisionReducida.setChecked(false);
                        cbSillaRuedas.setError(null);
                        txtErrorAccesibilidad.setVisibility(View.GONE);
                    }
                }
        );

        CompoundButton.OnCheckedChangeListener listener =
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        cbSinAdaptaciones.setChecked(false);
                        cbSillaRuedas.setError(null);
                        txtErrorAccesibilidad.setVisibility(View.GONE);
                    }
                };

        cbSillaRuedas.setOnCheckedChangeListener(listener);
        cbRecorridosCortos.setOnCheckedChangeListener(listener);
        cbVisionReducida.setOnCheckedChangeListener(listener);

        cbTerminos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                txtErrorTerminos.setVisibility(View.GONE);
                cbTerminos.setError(null);
            }
        });
    }

    // ===================================================
    // CREAR CUENTA EN FIREBASE AUTHENTICATION
    // ===================================================

    private void crearCuentaFirebase(
            String nombre,
            String correo,
            String contrasena) {

        registrando = true;

        btnCrearCuenta.setEnabled(false);
        btnCrearCuenta.setAlpha(0.6f);

        firebaseAuth.createUserWithEmailAndPassword(
                correo,
                contrasena
        ).addOnCompleteListener(this, task -> {

            if (task.isSuccessful()) {

                FirebaseUser usuario =
                        firebaseAuth.getCurrentUser();

                if (usuario != null) {
                    guardarUsuarioFirestore(
                            usuario.getUid(),
                            nombre,
                            correo
                    );
                } else {
                    finalizarRegistroEnCurso();
                    Toast.makeText(
                            this,
                            "No se pudo obtener el usuario creado",
                            Toast.LENGTH_LONG
                    ).show();
                }

            } else {

                finalizarRegistroEnCurso();
                Exception error = task.getException();

                if (error instanceof FirebaseAuthUserCollisionException) {
                    etCorreo.setError("Este correo electrónico ya está registrado");
                    etCorreo.requestFocus();
                } else {
                    Toast.makeText(
                            this,
                            "No se pudo crear la cuenta. Verificá la conexión e intentá nuevamente.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        });
    }

    // ===================================================
    // GUARDAR USUARIO EN CLOUD FIRESTORE
    // ===================================================

    private void guardarUsuarioFirestore(
            String uid,
            String nombre,
            String correo) {

        Map<String, Object> datosUsuario = new HashMap<>();

        datosUsuario.put("uid", uid);
        datosUsuario.put("nombreApellidos", nombre);
        datosUsuario.put("correo", correo);

        Map<String, Object> accesibilidad = new HashMap<>();

        accesibilidad.put("sillaRuedas", cbSillaRuedas.isChecked());
        accesibilidad.put("recorridosCortos", cbRecorridosCortos.isChecked());
        accesibilidad.put("visionReducida", cbVisionReducida.isChecked());


        datosUsuario.put("accesibilidad", accesibilidad);
        datosUsuario.put("fechaRegistro", FieldValue.serverTimestamp());

        firestore.collection("usuarios")
                .document(uid)
                .set(datosUsuario)
                .addOnSuccessListener(unused -> {

                    finalizarRegistroEnCurso();

                    Toast.makeText(
                            RegistroActivity.this,
                            "¡Cuenta creada correctamente!",
                            Toast.LENGTH_LONG
                    ).show();

                    firebaseAuth.signOut();

                    Intent intent = new Intent(
                            RegistroActivity.this,
                            InicioActivity.class
                    );

                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {

                    finalizarRegistroEnCurso();

                    Toast.makeText(
                            this,
                            "La cuenta fue creada, pero no se pudo guardar el perfil en Firebase.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ===================================================
    // REACTIVAR EL BOTÓN DE REGISTRO
    // ===================================================

    private void finalizarRegistroEnCurso() {
        registrando = false;
        btnCrearCuenta.setEnabled(true);
        btnCrearCuenta.setAlpha(1.0f);
    }
}

