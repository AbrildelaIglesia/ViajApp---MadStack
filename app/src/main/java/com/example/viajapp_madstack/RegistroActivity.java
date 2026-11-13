package com.example.viajapp_madstack;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    // CAMPOS DEL FORMULARIO
    private EditText etNombreApellidos, etCorreo,
            etContrasena, etConfirmarContrasena;

    // CASILLAS DE ACCESIBILIDAD Y TÉRMINOS
    private CheckBox cbSillaRuedas, cbRecorridosCortos,
            cbVisionReducida, cbSubtitulos,
            cbSinAdaptaciones, cbTerminos;

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

    private final IdentityHashMap<ImageView, ColorStateList> tintesOriginales =
            new IdentityHashMap<>();


    // Guardar los dibujos originales para restaurarlos
    private final IdentityHashMap<ImageView, Drawable.ConstantState> iconosOriginales =
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

        // CONECTAR CASILLAS
        cbSillaRuedas = findViewById(R.id.cbSillaRuedas);
        cbRecorridosCortos = findViewById(R.id.cbRecorridosCortos);
        cbVisionReducida = findViewById(R.id.cbVisionReducida);
        cbSubtitulos = findViewById(R.id.cbSubtitulos);
        cbSinAdaptaciones = findViewById(R.id.cbSinAdaptaciones);
        cbTerminos = findViewById(R.id.cbTerminos);

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
        });

        // BOTÓN CONTRASTE
        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();

            String nombreModo;

            if (modoContraste == 0) {
                nombreModo = "Normal";
            } else if (modoContraste == 1) {
                nombreModo = "Crema";
            } else {
                nombreModo = "Oscuro";
            }

            Toast.makeText(
                    this,
                    "Contraste: " + nombreModo,
                    Toast.LENGTH_SHORT
            ).show();
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

            // NO MODIFICAR LAS CONTRASEÑAS CON trim()
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

        boolean normal = modoContraste == 0;
        boolean crema = modoContraste == 1;


        // COLORES DE CADA TEMA
        // Inspirados en los modos oscuro y crema de Wattpad

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

                control.setTextColor(
                        coloresOriginales.get(control)
                );

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

            btnCrearCuenta.setTextColor(
                    coloresOriginales.get(btnCrearCuenta)
            );


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


        // 7. CAMPOS DEL FORMULARIO
        EditText[] campos = {
                etNombreApellidos,
                etCorreo,
                etContrasena,
                etConfirmarContrasena
        };

        for (EditText campo : campos) {

            guardarFondoOriginal(campo);
            guardarColorOriginal(campo);

            if (!pistasOriginales.containsKey(campo)) {
                pistasOriginales.put(
                        campo,
                        campo.getHintTextColors()
                );
            }

            if (normal) {

                restaurarFondo(campo);

                campo.setTextColor(
                        coloresOriginales.get(campo)
                );

                campo.setHintTextColor(
                        pistasOriginales.get(campo)
                );

            } else {

                campo.setBackground(
                        fondoCampoTema(fondoCampo, borde)
                );

                campo.setTextColor(textoPrincipal);
                campo.setHintTextColor(textoPista);
            }
        }

        // 8. ICONOS VECTORIALES
        actualizarIconosContraste(
                pantallaRegistro,
                colorIconos
        );

        // 9. ICONOS DE LOS CAMPOS
        actualizarIconoInicio(
                etNombreApellidos,
                R.drawable.ic_person,
                colorIconos
        );

        actualizarIconoInicio(
                etCorreo,
                R.drawable.ic_email,
                colorIconos
        );

        actualizarIconoInicio(
                etContrasena,
                R.drawable.ic_lock,
                colorIconos
        );

        actualizarIconoInicio(
                etConfirmarContrasena,
                R.drawable.ic_lock,
                colorIconos
        );

        // 10. FLECHA DEL BOTÓN CREAR CUENTA
        actualizarIconoFinal(
                btnCrearCuenta,
                R.drawable.ic_arrow_forward,
                crema ? Color.parseColor("#222222") : Color.WHITE
        );


        // 11. OJITOS DE LAS CONTRASEÑAS
        actualizarIconosContrasenas(
                pantallaRegistro,
                normal ? 0 : colorIconos
        );
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

            } else if (texto.getId() == R.id.txtIniciarSesion) {

                // EL ENLACE DE INICIO DE SESIÓN SE DESTACA
                texto.setTextColor(
                        Color.parseColor(
                                modoContraste == 1
                                        ? "#664700"
                                        : "#FFE16B"
                        )
                );

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
                cbSubtitulos,
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
                        new int[] {
                                marcado,
                                desmarcado
                        }
                );

                casilla.setButtonTintList(tintes);
            }
        }
    }

    // ===================================================
    // CONSERVAR Y RESTAURAR EL ASPECTO ORIGINAL
    // ===================================================

    private void guardarFondoOriginal(View vista) {

        if (!fondosOriginales.containsKey(vista)) {
            fondosOriginales.put(
                    vista,
                    vista.getBackground()
            );
        }
    }

    private void restaurarFondo(View vista) {

        vista.setBackground(
                fondosOriginales.get(vista)
        );
    }

    private void guardarColorOriginal(TextView texto) {

        if (!coloresOriginales.containsKey(texto)) {
            coloresOriginales.put(
                    texto,
                    texto.getTextColors()
            );
        }
    }

    // ===================================================
    // ICONOS DE LA PANTALLA
    // ===================================================


    // CAMBIAR Y RESTAURAR LOS ICONOS

    // CAMBIAR LOS ICONOS SEGÚN EL CONTRASTE
    private void actualizarIconosContraste(
            View vista,
            int colorIconos) {

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;

            CharSequence descripcion =
                    imagen.getContentDescription();

            int recurso = 0;

            // Identificar solamente nuestros iconos
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

                    case "Micrófono":
                        recurso = R.drawable.ic_mic;
                        break;

                    case "Huella digital":
                        recurso = R.drawable.ic_fingerprint;
                        break;

                    case "Privacidad":
                        recurso = R.drawable.ic_privacy;
                        break;
                }
            }

            if (recurso != 0) {

                // Volver a cargar el dibujo original
                imagen.setImageTintList(null);
                imagen.setImageResource(recurso);

                // Aplicar siempre un color visible
                int colorFinal;

                if (modoContraste == 0) {

                    // NORMAL: iconos oscuros
                    colorFinal = Color.parseColor("#111827");

                } else {

                    // CREMA Y OSCURO
                    colorFinal = colorIconos;
                }

                imagen.setImageTintList(
                        ColorStateList.valueOf(colorFinal)
                );
            }
        }

        // Recorrer todos los elementos de la pantalla
        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarIconosContraste(
                        grupo.getChildAt(i),
                        colorIconos
                );
            }
        }
    }



    // ICONOS A LA IZQUIERDA DE LOS CAMPOS
    private void actualizarIconoInicio(
            EditText campo,
            int recurso,
            int color) {

        Drawable icono = obtenerIconoTema(recurso, color);

        Drawable[] actuales =
                campo.getCompoundDrawablesRelative();

        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                icono,
                actuales[1],
                actuales[2],
                actuales[3]
        );
    }

    // FLECHA DEL BOTÓN CREAR CUENTA
    private void actualizarIconoFinal(
            TextView boton,
            int recurso,
            int color) {

        Drawable icono = obtenerIconoTema(recurso, color);

        Drawable[] actuales =
                boton.getCompoundDrawablesRelative();

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

        Drawable icono =
                ContextCompat.getDrawable(this, recurso);

        if (icono != null && modoContraste != 0) {

            icono = icono.mutate();

            DrawableCompat.setTint(icono, color);
        }

        return icono;
    }


    // OJITOS PARA MOSTRAR Y OCULTAR CONTRASEÑAS
    private void actualizarIconosContrasenas(
            View vista,
            int colorIconos) {

        if (vista instanceof TextInputLayout) {

            TextInputLayout campo =
                    (TextInputLayout) vista;

            if (modoContraste == 0) {

                // NORMAL: ojitos oscuros
                campo.setEndIconTintList(
                        ColorStateList.valueOf(
                                Color.parseColor("#111827")
                        )
                );

            } else {

                // CREMA Y OSCURO: color correspondiente
                campo.setEndIconTintList(
                        ColorStateList.valueOf(colorIconos)
                );
            }
        }

        // Recorrer todos los elementos
        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                actualizarIconosContrasenas(
                        grupo.getChildAt(i),
                        colorIconos
                );
            }
        }
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

        forma.setStroke(
                dp(grosorDp),
                colorBorde
        );

        return forma;
    }

    private Drawable fondoCampoTema(
            int colorFondo,
            int colorBorde) {

        StateListDrawable estados =
                new StateListDrawable();

        // CAMPO SELECCIONADO
        estados.addState(
                new int[] { android.R.attr.state_focused },
                fondoRedondeado(
                        colorFondo,
                        colorBorde,
                        12,
                        2
                )
        );

        // CAMPO NORMAL
        estados.addState(
                new int[] {},
                fondoRedondeado(
                        colorFondo,
                        colorBorde,
                        12,
                        1
                )
        );

        return estados;
    }

    private Drawable fondoBotonTema(
            int normal,
            int presionado,
            int borde) {

        StateListDrawable estados =
                new StateListDrawable();

        // BOTÓN PRESIONADO
        estados.addState(
                new int[] { android.R.attr.state_pressed },
                fondoRedondeado(
                        presionado,
                        borde,
                        28,
                        2
                )
        );

        // BOTÓN NORMAL
        estados.addState(
                new int[] {},
                fondoRedondeado(
                        normal,
                        borde,
                        28,
                        2
                )
        );

        return estados;
    }

    // CONVERTIR DP A PÍXELES
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

            etNombreApellidos.setError(
                    "Ingrese su nombre y apellido"
            );

            etNombreApellidos.requestFocus();
            return false;
        }

        if (!nombre.matches(
                "[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+")) {

            etNombreApellidos.setError(
                    "El nombre y apellido solo pueden contener letras"
            );

            etNombreApellidos.requestFocus();
            return false;
        }

        if (nombre.length() < 2 || nombre.length() > 100) {

            etNombreApellidos.setError(
                    "El nombre y apellido deben tener entre 2 y 100 caracteres"
            );

            etNombreApellidos.requestFocus();
            return false;
        }

        if (nombre.split(" ").length < 2) {

            etNombreApellidos.setError(
                    "Ingrese su nombre y al menos un apellido"
            );

            etNombreApellidos.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // FORMATEAR NOMBRE Y APELLIDO
    // ===================================================

    private String formatearNombre(String nombre) {

        String[] palabras =
                nombre.toLowerCase().split(" ");

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

            etCorreo.setError(
                    "Ingrese su correo electrónico"
            );

            etCorreo.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

            etCorreo.setError(
                    "Ingrese un correo electrónico válido"
            );

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

            etContrasena.setError(
                    "Ingrese una contraseña"
            );

            etContrasena.requestFocus();
            return false;
        }

        if (contrasena.length() < 8) {

            etContrasena.setError(
                    "La contraseña debe tener al menos 8 caracteres"
            );

            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[A-Z].*")) {

            etContrasena.setError(
                    "Debe contener al menos una letra mayúscula"
            );

            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[a-z].*")) {

            etContrasena.setError(
                    "Debe contener al menos una letra minúscula"
            );

            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[0-9].*")) {

            etContrasena.setError(
                    "Debe contener al menos un número"
            );

            etContrasena.requestFocus();
            return false;
        }

        if (!contrasena.matches(".*[^a-zA-Z0-9].*")) {

            etContrasena.setError(
                    "Debe contener al menos un carácter especial"
            );

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

            etConfirmarContrasena.setError(
                    "Confirme su contraseña"
            );

            etConfirmarContrasena.requestFocus();
            return false;
        }

        if (!contrasena.equals(confirmarContrasena)) {

            etConfirmarContrasena.setError(
                    "Las contraseñas no coinciden"
            );

            etConfirmarContrasena.requestFocus();
            return false;
        }

        return true;
    }

    // ===================================================
    // VALIDAR PREFERENCIAS DE ACCESIBILIDAD
    // ===================================================

    private boolean validarAccesibilidad() {

        if (!cbSillaRuedas.isChecked()
                && !cbRecorridosCortos.isChecked()
                && !cbVisionReducida.isChecked()
                && !cbSubtitulos.isChecked()
                && !cbSinAdaptaciones.isChecked()) {

            cbSillaRuedas.setError(
                    "Seleccioná al menos una opción de accesibilidad"
            );

            cbSillaRuedas.requestFocus();
            return false;
        }

        cbSillaRuedas.setError(null);
        return true;
    }

    // ===================================================
    // VALIDAR TÉRMINOS Y CONDICIONES
    // ===================================================

    private boolean validarTerminos() {

        if (!cbTerminos.isChecked()) {

            cbTerminos.setError(
                    "Debe aceptar los términos y condiciones"
            );

            cbTerminos.requestFocus();

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

        // SI ELIGE SIN ADAPTACIONES
        cbSinAdaptaciones.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (isChecked) {

                        cbSillaRuedas.setChecked(false);
                        cbRecorridosCortos.setChecked(false);
                        cbVisionReducida.setChecked(false);
                        cbSubtitulos.setChecked(false);

                        cbSillaRuedas.setError(null);
                    }
                }
        );

        // SI ELIGE ALGUNA ADAPTACIÓN
        CompoundButton.OnCheckedChangeListener listener =
                (buttonView, isChecked) -> {

                    if (isChecked) {

                        cbSinAdaptaciones.setChecked(false);

                        cbSillaRuedas.setError(null);
                    }
                };

        cbSillaRuedas.setOnCheckedChangeListener(listener);
        cbRecorridosCortos.setOnCheckedChangeListener(listener);
        cbVisionReducida.setOnCheckedChangeListener(listener);
        cbSubtitulos.setOnCheckedChangeListener(listener);
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

                    etCorreo.setError(
                            "Este correo electrónico ya está registrado"
                    );

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

        Map<String, Object> datosUsuario =
                new HashMap<>();

        datosUsuario.put("uid", uid);
        datosUsuario.put("nombreApellidos", nombre);
        datosUsuario.put("correo", correo);

        // GUARDAR PREFERENCIAS
        Map<String, Object> accesibilidad =
                new HashMap<>();

        accesibilidad.put(
                "sillaRuedas",
                cbSillaRuedas.isChecked()
        );

        accesibilidad.put(
                "recorridosCortos",
                cbRecorridosCortos.isChecked()
        );

        accesibilidad.put(
                "visionReducida",
                cbVisionReducida.isChecked()
        );

        accesibilidad.put(
                "subtitulos",
                cbSubtitulos.isChecked()
        );

        accesibilidad.put(
                "sinAdaptaciones",
                cbSinAdaptaciones.isChecked()
        );

        datosUsuario.put(
                "accesibilidad",
                accesibilidad
        );

        datosUsuario.put(
                "fechaRegistro",
                FieldValue.serverTimestamp()
        );

        firestore.collection("usuarios")
                .document(uid)
                .set(datosUsuario)
                .addOnSuccessListener(unused -> {

                    finalizarRegistroEnCurso();

                    Toast.makeText(
                            this,
                            "¡Cuenta creada correctamente!",
                            Toast.LENGTH_LONG
                    ).show();

                    // LA NAVEGACIÓN SE INTEGRARÁ
                    // CON LA PANTALLA DE INICIO DE SESIÓN

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
