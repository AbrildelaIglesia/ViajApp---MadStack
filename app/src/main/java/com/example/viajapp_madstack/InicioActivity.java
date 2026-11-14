
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

// IMPORTACIONES NUEVAS PARA ACCESIBILIDAD
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.IdentityHashMap;

public class InicioActivity extends AppCompatActivity {

    private EditText etCorreo, etContrasena;
    private TextView txtRegistrarse;
    private LinearLayout btnIniciarSesion;
    private FirebaseAuth mAuth;

    // ===================================================
    // NUEVO: BARRA DE ACCESIBILIDAD
    // ===================================================

    private TextView btnAccesibilidad, btnDisminuirTexto,
            btnAumentarTexto, btnContraste, btnAudio;

    private LinearLayout barraAccesibilidad;
    private View pantallaInicio, scrollContenido;

    // GUARDAR DISEÑO ORIGINAL
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<EditText, ColorStateList> pistasOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private float escalaTexto = 1.0f;

    // 0 = NORMAL
    // 1 = CREMA
    // 2 = OSCURO
    private int modoContraste = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // NUEVO: INICIAR ACCESIBILIDAD
        configurarAccesibilidad();

        // ===================================================
        // CÓDIGO ORIGINAL - SIN MODIFICACIONES
        // ===================================================

        mAuth = FirebaseAuth.getInstance();
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);

        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIniciarSesion.setOnClickListener(v -> {

            String correo = etCorreo.getText().toString().trim();
            String contrasena = etContrasena.getText().toString();

            // Validar correo
            if (!validarCorreo(correo)) {
                return;
            }

            // Validar contraseña
            if (contrasena.isEmpty()) {
                etContrasena.setError("Ingrese su contraseña");
                etContrasena.requestFocus();
                return;
            }

            // Consultar Firebase Authentication
            mAuth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    InicioActivity.this,
                                    "¡Bienvenido!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            Intent intent = new Intent(InicioActivity.this, AccesibilidadActivity.class);
                            startActivity(intent);

                        } else {

                            Toast.makeText(
                                    InicioActivity.this,
                                    "No se pudo iniciar sesión. Verificá tu correo y contraseña.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // BOTÓN "REGISTRATE ACÁ"
        txtRegistrarse = findViewById(R.id.txtRegistrarse);

        txtRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InicioActivity.this, RegistroActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validarCorreo(String correo)
    {
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

    // ===================================================
    // NUEVO: CONFIGURACIÓN DEL MENÚ DE ACCESIBILIDAD
    // ===================================================

    private void configurarAccesibilidad() {

        pantallaInicio = findViewById(R.id.main);
        scrollContenido = findViewById(R.id.scrollContenido);

        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        // GUARDAR DISEÑO ORIGINAL
        guardarOriginales(pantallaInicio);

        // ABRIR Y CERRAR MENÚ
        btnAccesibilidad.setOnClickListener(v -> {

            boolean mostrar =
                    btnDisminuirTexto.getVisibility() != View.VISIBLE;

            int visibilidad = mostrar ? View.VISIBLE : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

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

            ConstraintLayout.LayoutParams parametrosBarra =
                    (ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = mostrar
                    ? 0
                    : ConstraintLayout.LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(parametrosBarra);

            btnAccesibilidad.setContentDescription(
                    mostrar
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // AUMENTAR TEXTO
        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
        });

        // DISMINUIR TEXTO
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(scrollContenido);
        });

        // CAMBIAR CONTRASTE
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

        // AUDIO SE PROGRAMARÁ DESPUÉS
    }

    // ===================================================
    // NUEVO: GUARDAR COLORES Y FONDOS ORIGINALES
    // ===================================================

    private void guardarOriginales(View vista) {

        if (vista == null) return;

        if (!fondosOriginales.containsKey(vista)) {
            fondosOriginales.put(
                    vista,
                    vista.getBackground()
            );
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            if (!coloresOriginales.containsKey(texto)) {

                coloresOriginales.put(
                        texto,
                        texto.getTextColors()
                );

                tamanosOriginales.put(
                        texto,
                        texto.getTextSize()
                );
            }
        }

        if (vista instanceof EditText) {

            EditText campo = (EditText) vista;

            if (!pistasOriginales.containsKey(campo)) {

                pistasOriginales.put(
                        campo,
                        campo.getHintTextColors()
                );
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    // ===================================================
    // NUEVO: AUMENTAR Y DISMINUIR TEXTOS
    // ===================================================

    private void actualizarTamanoTextos(View vista) {

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            Float original = tamanosOriginales.get(texto);

            if (original != null) {

                texto.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        original * escalaTexto
                );
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                actualizarTamanoTextos(grupo.getChildAt(i));
            }
        }
    }

    // ===================================================
    // NUEVO: CONTRASTE NORMAL, CREMA Y OSCURO
    // ===================================================

    private void aplicarTemaContraste() {

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

        int colorIconos = Color.parseColor(
                crema ? "#272727" : "#FFFFFF"
        );

        // 1. FONDO GENERAL
        if (normal) {

            restaurarFondo(pantallaInicio);
            restaurarFondo(scrollContenido);

        } else {

            pantallaInicio.setBackgroundColor(fondoPantalla);
            scrollContenido.setBackgroundColor(fondoPantalla);
        }

        // 2. TEXTOS
        actualizarColoresTextos(
                scrollContenido,
                textoPrincipal,
                textoSecundario
        );



        // ===================================================
        // RECUADRO DE LECTORES DE PANTALLA
        // NORMAL: VERDE / CREMA Y OSCURO: COLORES DEL TEMA
        // ===================================================

        TextView txtCompatibilidad =
                findViewById(R.id.txtCompatibilidadLectores);

        if (normal) {

            // CONSERVAR EL VERDE ORIGINAL
            restaurarFondo(txtCompatibilidad);
            restaurarColor(txtCompatibilidad);

        } else {

            // CREMA Y OSCURO: USAR LOS COLORES DEL TEMA
            txtCompatibilidad.setBackground(
                    fondoRedondeado(
                            fondoCampo,
                            borde,
                            20
                    )
            );

            txtCompatibilidad.setTextColor(textoPrincipal);
        }



        // 3. BARRA DE ACCESIBILIDAD
        if (normal) {

            restaurarFondo(barraAccesibilidad);

        } else {

            barraAccesibilidad.setBackground(
                    fondoRedondeado(fondoBarra, borde, 16)
            );
        }

        // 4. CONTROLES DEL MENÚ
        TextView[] controles = {
                btnAccesibilidad,
                btnDisminuirTexto,
                btnAumentarTexto,
                btnContraste,
                btnAudio
        };

        for (TextView control : controles) {

            if (normal) {

                restaurarFondo(control);
                restaurarColor(control);

            } else {

                control.setBackground(
                        fondoRedondeado(fondoControl, borde, 12)
                );

                control.setTextColor(textoPrincipal);
            }
        }

        // 5. CAMPOS
        EditText[] campos = {
                findViewById(R.id.etCorreo),
                findViewById(R.id.etContrasena)
        };

        for (EditText campo : campos) {

            if (normal) {

                restaurarFondo(campo);
                restaurarColor(campo);

                campo.setHintTextColor(
                        pistasOriginales.get(campo)
                );

            } else {

                campo.setBackground(
                        fondoRedondeado(fondoCampo, borde, 12)
                );

                campo.setTextColor(textoPrincipal);
                campo.setHintTextColor(textoPista);
            }
        }

        // 6. BOTÓN INICIAR SESIÓN
        LinearLayout boton =
                findViewById(R.id.btnIniciarSesion);

        if (normal) {

            restaurarFondo(boton);

        } else {

            int fondoBoton = Color.parseColor(
                    crema ? "#E9D7B8" : "#303030"
            );

            boton.setBackground(
                    fondoRedondeado(fondoBoton, borde, 20)
            );
        }

        // 7. TEXTO DEL BOTÓN INICIAR SESIÓN
        for (int i = 0; i < boton.getChildCount(); i++) {

            View hijo = boton.getChildAt(i);

            if (hijo instanceof TextView) {

                TextView texto = (TextView) hijo;

                if (normal) {

                    restaurarColor(texto);

                } else {

                    texto.setTextColor(
                            crema ? Color.parseColor("#222222") : Color.WHITE
                    );
                }
            }
        }

        // 8. CONTENEDORES DE HUELLA Y MICRÓFONO
        ImageView huella = buscarImagen(
                pantallaInicio,
                "Huella digital"
        );

        ImageView microfono = buscarImagen(
                pantallaInicio,
                "Micrófono"
        );

        if (huella != null) {

            actualizarFondoContenedor(
                    (View) huella.getParent(),
                    normal,
                    fondoCampo,
                    borde
            );
        }

        if (microfono != null) {

            actualizarFondoContenedor(
                    (View) microfono.getParent(),
                    normal,
                    fondoCampo,
                    borde
            );
        }

        // 9. ICONOS DE CORREO Y CONTRASEÑA
        actualizarIconoInicio(
                findViewById(R.id.etCorreo),
                R.drawable.ic_email,
                colorIconos
        );

        actualizarIconoInicio(
                findViewById(R.id.etContrasena),
                R.drawable.ic_lock,
                colorIconos
        );

        // 10. ICONOS DE HUELLA Y MICRÓFONO
        actualizarIconoImagen(
                "Huella digital",
                R.drawable.ic_fingerprint,
                colorIconos
        );

        actualizarIconoImagen(
                "Micrófono",
                R.drawable.ic_mic,
                colorIconos
        );

        // 11. ENLACE DE REGISTRO
        TextView enlace = findViewById(R.id.txtRegistrarse);

        if (normal) {

            restaurarColor(enlace);

        } else {

            enlace.setTextColor(
                    crema
                            ? Color.parseColor("#664700")
                            : Color.parseColor("#FFE16B")
            );
        }
    }

    // ===================================================
    // NUEVO: CAMBIAR COLORES DE TEXTOS
    // ===================================================

    private void actualizarColoresTextos(
            View vista,
            int principal,
            int secundario) {

        if (vista instanceof TextView
                && !(vista instanceof EditText)) {

            TextView texto = (TextView) vista;

            ColorStateList original =
                    coloresOriginales.get(texto);

            if (original != null) {

                if (modoContraste == 0) {

                    texto.setTextColor(original);

                } else {

                    int colorAnterior = original.getDefaultColor();

                    boolean esSecundario =
                            colorAnterior == Color.parseColor("#4B5563")
                                    || colorAnterior == Color.parseColor("#6B7280")
                                    || colorAnterior == Color.parseColor("#52647A");

                    texto.setTextColor(
                            esSecundario ? secundario : principal
                    );
                }
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
    // NUEVO: RESTAURAR DISEÑO ORIGINAL
    // ===================================================

    private void restaurarFondo(View vista) {

        if (vista != null && fondosOriginales.containsKey(vista)) {

            vista.setBackground(
                    fondosOriginales.get(vista)
            );
        }
    }

    private void restaurarColor(TextView texto) {

        ColorStateList original = coloresOriginales.get(texto);

        if (original != null) {
            texto.setTextColor(original);
        }
    }

    // ===================================================
    // NUEVO: FONDOS DE LOS CONTENEDORES
    // ===================================================

    private void actualizarFondoContenedor(
            View contenedor,
            boolean normal,
            int fondo,
            int borde) {

        if (contenedor == null) return;

        if (normal) {

            restaurarFondo(contenedor);

        } else {

            contenedor.setBackground(
                    fondoRedondeado(fondo, borde, 12)
            );
        }
    }

    // ===================================================
    // NUEVO: ICONOS DE LOS CAMPOS
    // ===================================================

    private void actualizarIconoInicio(
            EditText campo,
            int recurso,
            int color) {

        Drawable icono = ContextCompat.getDrawable(this, recurso);

        if (icono == null) return;

        icono = icono.mutate();

        int colorFinal = modoContraste == 0
                ? Color.parseColor("#111827")
                : color;

        DrawableCompat.setTint(icono, colorFinal);

        Drawable[] actuales = campo.getCompoundDrawablesRelative();

        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(
                icono,
                actuales[1],
                actuales[2],
                actuales[3]
        );
    }

    // ===================================================
    // NUEVO: ICONOS DE HUELLA Y MICRÓFONO
    // ===================================================

    private void actualizarIconoImagen(
            String descripcion,
            int recurso,
            int color) {

        ImageView imagen = buscarImagen(
                pantallaInicio,
                descripcion
        );

        if (imagen == null) return;

        // VOLVER A CARGAR EL ICONO ORIGINAL
        imagen.setImageTintList(null);
        imagen.setImageResource(recurso);

        int colorFinal = modoContraste == 0
                ? Color.parseColor("#111827")
                : color;

        imagen.setImageTintList(
                ColorStateList.valueOf(colorFinal)
        );
    }

    // ===================================================
    // NUEVO: BUSCAR IMÁGENES
    // ===================================================

    private ImageView buscarImagen(
            View vista,
            String descripcion) {

        if (vista instanceof ImageView) {

            ImageView imagen = (ImageView) vista;

            CharSequence contenido = imagen.getContentDescription();

            if (contenido != null
                    && contenido.toString().equals(descripcion)) {

                return imagen;
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                ImageView encontrada = buscarImagen(
                        grupo.getChildAt(i),
                        descripcion
                );

                if (encontrada != null) return encontrada;
            }
        }

        return null;
    }

    // ===================================================
    // NUEVO: FONDOS REDONDEADOS
    // ===================================================

    private Drawable fondoRedondeado(
            int color,
            int borde,
            int radioDp) {

        GradientDrawable forma = new GradientDrawable();

        forma.setShape(GradientDrawable.RECTANGLE);
        forma.setColor(color);
        forma.setCornerRadius(dp(radioDp));
        forma.setStroke(dp(1), borde);

        return forma;
    }

    private int dp(int valor) {

        return Math.round(
                valor * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
