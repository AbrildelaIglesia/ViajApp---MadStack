
package com.example.viajapp_madstack;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import androidx.appcompat.widget.AppCompatButton;

import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private EditText etNombreApellidos, etCorreo, etContrasena, etConfirmarContrasena;
    private CheckBox cbSillaRuedas, cbRecorridosCortos, cbVisionReducida,
            cbSubtitulos, cbSinAdaptaciones, cbTerminos;
    private AppCompatButton btnCrearCuenta;

    // Variables de Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    // Evita presionar varias veces el botón mientras se registra
    private boolean registrando = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);


        etNombreApellidos = findViewById(R.id.etNombreApellidos);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena);
        cbSillaRuedas = findViewById(R.id.cbSillaRuedas);
        cbRecorridosCortos = findViewById(R.id.cbRecorridosCortos);
        cbVisionReducida = findViewById(R.id.cbVisionReducida);
        cbSubtitulos = findViewById(R.id.cbSubtitulos);
        cbSinAdaptaciones = findViewById(R.id.cbSinAdaptaciones);
        cbTerminos = findViewById(R.id.cbTerminos);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Configurar las preferencias de accesibilidad
        configurarAccesibilidad();

        btnCrearCuenta.setOnClickListener(v ->
        {
            if (registrando) {
                return;
            }

            String nombre = etNombreApellidos.getText().toString().trim().replaceAll(" +", " ");
            nombre = formatearNombre(nombre);
            etNombreApellidos.setText(nombre);

            String correo = etCorreo.getText().toString().trim();

            // No usamos trim() para no modificar las contraseñas
            String contrasena = etContrasena.getText().toString();
            String confirmarContrasena = etConfirmarContrasena.getText().toString();

            if (!validarNombre(nombre)) {
                return;
            }

            if (!validarCorreo(correo)) {
                return;
            }

            if (!validarContrasena(contrasena)) {
                return;
            }

            if (!validarConfirmacion(contrasena, confirmarContrasena)) {
                return;
            }


            if (!validarAccesibilidad()) {
                return;
            }

            if (!validarTerminos()) {
                return;
            }

            // Si todas las validaciones son correctas,
            // se crea la cuenta en Firebase
            crearCuentaFirebase(nombre, correo, contrasena);
        });
    }



    // VALIDACIÓN DE NOMBRE Y APELLIDO

    private boolean validarNombre(String nombre)
    {
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

        // Comprobar que haya al menos un nombre y un apellido
        if (nombre.split(" ").length < 2) {
            etNombreApellidos.setError("Ingrese su nombre y al menos un apellido");
            etNombreApellidos.requestFocus();
            return false;
        }

        return true;
    }

    // FORMATEAR NOMBRE Y APELLIDO

    private String formatearNombre(String nombre) {

        String[] palabras = nombre.toLowerCase().split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                if (resultado.length() > 0) {
                    resultado.append(" ");
                }

                if (palabra.equals("de") || palabra.equals("del")
                        || palabra.equals("la") || palabra.equals("las")
                        || palabra.equals("los")) {
                    resultado.append(palabra);
                } else {
                    resultado.append(palabra.substring(0, 1).toUpperCase())
                            .append(palabra.substring(1));
                }
            }
        }

        return resultado.toString();
    }

    // VALIDACIÓN DEL CORREO ELECTRÓNICO

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

    // VALIDACIÓN DE CONTRASEÑA

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

    // VALIDACIÓN DE CONFIRMAR CONTRASEÑA

    private boolean validarConfirmacion(String contrasena, String confirmarContrasena) {

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




    // VALIDACIÓN DE TÉRMINOS Y CONDICIONES

    private boolean validarTerminos() {

        if (!cbTerminos.isChecked()) {
            cbTerminos.setError("Debe aceptar los términos y condiciones");
            cbTerminos.requestFocus();

            Toast.makeText(this,
                    "Debe aceptar los términos y condiciones",
                    Toast.LENGTH_LONG).show();

            return false;
        }

        cbTerminos.setError(null);
        return true;
    }

    // CONFIGURAR PREFERENCIAS DE ACCESIBILIDAD


    private void configurarAccesibilidad() {

        // Si selecciona "Sin adaptaciones",
        // se desmarcan las otras opciones
        cbSinAdaptaciones.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbSillaRuedas.setChecked(false);
                cbRecorridosCortos.setChecked(false);
                cbVisionReducida.setChecked(false);
                cbSubtitulos.setChecked(false);

                // Quitar el error de accesibilidad
                cbSillaRuedas.setError(null);
            }
        });

        // Si selecciona alguna adaptación,
        // se desmarca "Sin adaptaciones"
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (isChecked) {
                cbSinAdaptaciones.setChecked(false);

                // Quitar el error de accesibilidad
                cbSillaRuedas.setError(null);
            }
        };

        cbSillaRuedas.setOnCheckedChangeListener(listener);
        cbRecorridosCortos.setOnCheckedChangeListener(listener);
        cbVisionReducida.setOnCheckedChangeListener(listener);
        cbSubtitulos.setOnCheckedChangeListener(listener);
    }


    // CREAR CUENTA EN FIREBASE AUTHENTICATION

    private void crearCuentaFirebase(String nombre, String correo, String contrasena) {

        registrando = true;
        btnCrearCuenta.setEnabled(false);
        btnCrearCuenta.setAlpha(0.6f);

        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser usuario = firebaseAuth.getCurrentUser();

                        if (usuario != null) {
                            guardarUsuarioFirestore(usuario.getUid(), nombre, correo);
                        } else {
                            finalizarRegistroEnCurso();

                            Toast.makeText(this,
                                    "No se pudo obtener el usuario creado",
                                    Toast.LENGTH_LONG).show();
                        }

                    } else {

                        finalizarRegistroEnCurso();

                        Exception error = task.getException();

                        if (error instanceof FirebaseAuthUserCollisionException) {
                            etCorreo.setError("Este correo electrónico ya está registrado");
                            etCorreo.requestFocus();
                        } else {
                            Toast.makeText(this,
                                    "No se pudo crear la cuenta. Verificá la conexión e intentá nuevamente.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // GUARDAR USUARIO EN CLOUD FIRESTORE

    private void guardarUsuarioFirestore(String uid, String nombre, String correo) {

        Map<String, Object> datosUsuario = new HashMap<>();

        datosUsuario.put("uid", uid);
        datosUsuario.put("nombreApellidos", nombre);
        datosUsuario.put("correo", correo);

        // Guardar las opciones de accesibilidad seleccionadas
        Map<String, Object> accesibilidad = new HashMap<>();

        accesibilidad.put("sillaRuedas", cbSillaRuedas.isChecked());
        accesibilidad.put("recorridosCortos", cbRecorridosCortos.isChecked());
        accesibilidad.put("visionReducida", cbVisionReducida.isChecked());
        accesibilidad.put("subtitulos", cbSubtitulos.isChecked());
        accesibilidad.put("sinAdaptaciones", cbSinAdaptaciones.isChecked());

        datosUsuario.put("accesibilidad", accesibilidad);

        // Guardar la fecha de registro
        datosUsuario.put("fechaRegistro", FieldValue.serverTimestamp());

        // Guardar los datos en la colección usuarios
        firestore.collection("usuarios")
                .document(uid)
                .set(datosUsuario)
                .addOnSuccessListener(unused -> {

                    finalizarRegistroEnCurso();

                    Toast.makeText(this,
                            "¡Cuenta creada correctamente!",
                            Toast.LENGTH_LONG).show();

                    // Más adelante agregaremos la navegación
                    // a la pantalla principal de ViajAPP
                })
                .addOnFailureListener(e -> {

                    finalizarRegistroEnCurso();

                    Toast.makeText(this,
                            "La cuenta fue creada, pero no se pudo guardar el perfil en Firebase.",
                            Toast.LENGTH_LONG).show();
                });
    }

    // REACTIVAR EL BOTÓN DE REGISTRO

    private void finalizarRegistroEnCurso() {

        registrando = false;
        btnCrearCuenta.setEnabled(true);
        btnCrearCuenta.setAlpha(1.0f);
    }
}
