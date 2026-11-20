
package com.example.viajapp_madstack;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import java.util.IdentityHashMap;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.google.firebase.firestore.FieldValue;

public class PresupuestoActivity extends AppCompatActivity {

    private View pantallaPrincipal;
    private LinearLayout barraAccesibilidad;

    private TextView btnAccesibilidad;
    private TextView btnDisminuirTexto;
    private TextView btnAumentarTexto;
    private TextView btnContraste;
    private TextView btnAudio;

    private float escalaTexto = 1.0f;
    private int modoContraste = 0;
    private boolean controlesVisibles = true;

    // VIAJE SELECCIONADO PARA CONTROLAR GASTOS
    private DocumentReference viajeSeleccionado;
    private String destinoPresupuesto = "";
    private long limitePresupuesto = 0L;

    // GASTOS DEL VIAJE SELECCIONADO
    private long totalGastado = 0L;
    private boolean guardandoGasto = false;


    // Guardar el PDF generado
    private byte[] pdfPendiente;

    // Abrir el selector de archivos de Android
    private final androidx.activity.result.ActivityResultLauncher<Intent>
            guardarPdfLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts
                    .StartActivityForResult(),
            resultado -> {

                if (resultado.getResultCode() == RESULT_OK
                        && resultado.getData() != null
                        && resultado.getData().getData() != null
                        && pdfPendiente != null) {

                    try (java.io.OutputStream salida =
                                 getContentResolver().openOutputStream(
                                         resultado.getData().getData())) {

                        if (salida == null) {
                            throw new java.io.IOException(
                                    "No se pudo abrir el archivo");
                        }

                        salida.write(pdfPendiente);

                        Toast.makeText(this,
                                "PDF guardado correctamente",
                                Toast.LENGTH_LONG).show();

                    } catch (Exception e) {

                        Toast.makeText(this,
                                "No se pudo guardar el PDF",
                                Toast.LENGTH_LONG).show();
                    }
                }

                pdfPendiente = null;
            }
    );


    // Guardamos el diseño original de la pantalla
    private final IdentityHashMap<View, Drawable> fondosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, ColorStateList> coloresOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<TextView, Float> tamanosOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<MaterialCardView, int[]> tarjetasOriginales =
            new IdentityHashMap<>();

    private final IdentityHashMap<Button, ColorStateList> tintesOriginales =
            new IdentityHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_presupuesto);

        pantallaPrincipal = findViewById(R.id.main);
        barraAccesibilidad = findViewById(R.id.barraAccesibilidad);

        // Guardar la apariencia antes de modificarla
        guardarOriginales(pantallaPrincipal);

        configurarNavegacion();
        configurarBarraAccesibilidad();
        configurarPerfil();
        configurarCategorias();

        // CARGAR PRESUPUESTO DEL VIAJE
        configurarPresupuestoViaje();

        // ACTIVAR REGISTRO DE GASTOS
        configurarRegistroGastos();

        // ACTIVAR DESCARGA DEL PDF
        findViewById(R.id.btnDescargarPdf)
                .setOnClickListener(v -> prepararPdfPresupuesto());

        // ABRIR HISTORIAL GENERAL DE PRESUPUESTOS Y GASTOS
        findViewById(R.id.btnVerHistorialGeneral)
                .setOnClickListener(v -> {

                    Intent intent = new Intent(
                            PresupuestoActivity.this,
                            HistorialPresupuestosActivity.class
                    );

                    startActivity(intent);
                });

    }

    @Override
    protected void onResume() {
        super.onResume();

        modoContraste =
                PreferenciasAccesibilidad.obtenerContraste(this);

        escalaTexto =
                PreferenciasAccesibilidad.obtenerEscalaTexto(this);

        aplicarTemaContraste();
        actualizarTamanoTextos(pantallaPrincipal);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (MenuSeguridad.manejarResultadoDictado(requestCode, resultCode, data)
                || MenuEditarPerfil.manejarResultadoDictado(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ==========================================
    // NAVEGACIÓN INFERIOR
    // ==========================================

    // ==========================================
    // SELECCIONAR UNA SOLA CATEGORÍA
    // ==========================================

    private void configurarCategorias() {

        RadioButton[] categorias = {
                findViewById(R.id.rbComida),
                findViewById(R.id.rbTransporte),
                findViewById(R.id.rbTours),
                findViewById(R.id.rbAlojamiento),
                findViewById(R.id.rbSouvenirs),
                findViewById(R.id.rbOtros),
                findViewById(R.id.rbImprevisto)
        };

        for (RadioButton categoria : categorias) {

            categoria.setOnClickListener(v -> {

                for (RadioButton otra : categorias) {
                    otra.setChecked(otra == categoria);
                }

            });
        }
    }

    // ==========================================
    // PRESUPUESTO ASOCIADO A CADA VIAJE
    // ==========================================

    private void configurarPresupuestoViaje() {

        findViewById(R.id.btnEditarPresupuesto)
                .setOnClickListener(v -> editarLimiteViaje());

        cargarViajesDisponibles();
    }

    // ==========================================
    // OBTENER LOS VIAJES DESDE FIREBASE
    // ==========================================

    private void cargarViajesDisponibles() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            Toast.makeText(
                    this,
                    "Iniciá sesión para consultar tus viajes",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.getUid())
                .collection("itinerarios")
                .get()
                .addOnSuccessListener(resultado -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (resultado.isEmpty()) {
                        mostrarMensajePresupuesto(
                                "Todavía no tenés viajes guardados. " +
                                        "Creá uno desde Planear."
                        );
                        return;
                    }

                    List<DocumentSnapshot> documentos =
                            new ArrayList<>(resultado.getDocuments());

                    // Mostrar primero los viajes más recientes
                    documentos.sort((a, b) -> {
                        java.util.Date fa =
                                a.getDate("fechaCreacion");
                        java.util.Date fb =
                                b.getDate("fechaCreacion");

                        if (fa == null && fb == null) return 0;
                        if (fa == null) return 1;
                        if (fb == null) return -1;

                        return fb.compareTo(fa);
                    });

                    String[] opciones =
                            new String[documentos.size()];

                    for (int i = 0; i < documentos.size(); i++) {

                        DocumentSnapshot documento =
                                documentos.get(i);

                        String destino =
                                documento.getString("destino");

                        if (destino == null || destino.isEmpty()) {
                            destino = "Viaje sin destino";
                        }

                        opciones[i] = destino;
                    }


                    // CARGAR LOS VIAJES EN EL DESPLEGABLE
                    com.google.android.material.textfield.MaterialAutoCompleteTextView selector =
                            findViewById(R.id.selectorViajes);

                    TextView ayudaSelector =
                            findViewById(R.id.tvAyudaSelectorViaje);


                    android.widget.ArrayAdapter<String> adaptador =
                            new android.widget.ArrayAdapter<String>(
                                    this,
                                    android.R.layout.simple_list_item_1,
                                    opciones
                            ) {
                                @Override
                                public View getDropDownView(
                                        int position, View convertView, ViewGroup parent) {

                                    View fila = super.getDropDownView(
                                            position, convertView, parent
                                    );

                                    int colorFondo = Color.parseColor(
                                            modoContraste == 1 ? "#F3DFC0" :
                                                    modoContraste == 2 ? "#2A2A2A" : "#EFF6FF"
                                    );

                                    fila.setBackgroundColor(colorFondo);

                                    if (fila instanceof TextView) {
                                        ((TextView) fila).setTextColor(
                                                modoContraste == 2
                                                        ? Color.WHITE
                                                        : Color.parseColor("#222222")
                                        );
                                    }

                                    return fila;
                                }


                                @Override
                                public View getView(int position, View convertView, ViewGroup parent) {

                                    View fila = super.getView(position, convertView, parent);

                                    TextView texto = fila.findViewById(android.R.id.text1);

                                    if (texto != null) {
                                        texto.setTextColor(
                                                modoContraste == 2
                                                        ? Color.WHITE
                                                        : Color.parseColor("#222222")
                                        );
                                    }

                                    return fila;
                                }


                            };


                    selector.setAdapter(adaptador);

                    // Mostrar el menú al tocar el selector
                    selector.setOnClickListener(v ->
                            selector.showDropDown()
                    );

                    // Seleccionar el viaje elegido
                    selector.setOnItemClickListener(
                            (parent, view, posicion, id) -> {

                                if (posicion >= 0
                                        && posicion < documentos.size()) {

                                    seleccionarViaje(
                                            documentos.get(posicion)
                                    );

                                    ayudaSelector.setText(
                                            "Podés cambiar de viaje cuando quieras."
                                    );
                                }
                            }
                    );

                    ayudaSelector.setText(
                            "Elegí un viaje para consultar sus gastos."
                    );

                })
                .addOnFailureListener(error -> {

                    mostrarMensajePresupuesto(
                            "No pudimos cargar tus viajes. " +
                                    "Revisá tu conexión."
                    );

                });


    }

    // ==========================================
    // LEER PRESUPUESTO DE UN VIAJE
    // ==========================================

    private void seleccionarViaje(DocumentSnapshot documento) {

        viajeSeleccionado = documento.getReference();

        destinoPresupuesto = documento.getString("destino");

        if (destinoPresupuesto == null) {
            destinoPresupuesto = "";
        }

        Long presupuesto =
                documento.getLong("presupuestoTotal");

        limitePresupuesto =
                presupuesto == null ? 0L : presupuesto;

        mostrarLimitePresupuesto();

        // Reiniciar el resumen antes de cargar los gastos de este viaje
        totalGastado = 0L;
        actualizarResumenGastos();

        // Recuperar los gastos guardados en Firebase
        cargarGastosDelViaje();

        if (presupuesto == null) {

            Toast.makeText(
                    this,
                    "Este viaje fue creado antes de guardar " +
                            "presupuestos. Podés establecer su límite con Editar.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ==========================================
    // ACTUALIZAR TEXTO
    // ==========================================

    private void mostrarLimitePresupuesto() {

        TextView texto =
                findViewById(R.id.tvLimiteTotal);

        NumberFormat formato =
                NumberFormat.getNumberInstance(
                        new Locale("es", "AR")
                );

        formato.setMaximumFractionDigits(0);

        texto.setText(
                "Límite: $" + formato.format(limitePresupuesto)
        );
    }

    // ==========================================
    // EDITAR PRESUPUESTO DEL VIAJE
    // ==========================================

    private void editarLimiteViaje() {

        if (viajeSeleccionado == null) {
            Toast.makeText(
                    this,
                    "Primero seleccioná un viaje",
                    Toast.LENGTH_LONG
            ).show();

            cargarViajesDisponibles();
            return;
        }

        EditText campo = new EditText(this);

        campo.setHint("Ej.: 500000");

        campo.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        campo.setSingleLine(true);

        if (limitePresupuesto > 0) {
            campo.setText(
                    String.valueOf(limitePresupuesto)
            );
        }

        LinearLayout contenedor = new LinearLayout(this);

        contenedor.setPadding(
                dp(20), dp(8), dp(20), 0
        );

        contenedor.addView(campo);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar presupuesto")
                .setMessage(
                        "Modificá el límite del viaje: " +
                                destinoPresupuesto
                )
                .setView(contenedor)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialogo.setOnShowListener(v -> {

            dialogo.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(b -> {

                long nuevoLimite;

                try {
                    nuevoLimite = Long.parseLong(
                            campo.getText().toString().trim()
                    );

                    if (nuevoLimite <= 0) {
                        campo.setError(
                                "Ingresá un valor mayor a cero"
                        );
                        return;
                    }

                } catch (NumberFormatException e) {
                    campo.setError("Ingresá un monto válido");
                    return;
                }

                dialogo.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setEnabled(false);

                viajeSeleccionado
                        .update("presupuestoTotal", nuevoLimite)
                        .addOnSuccessListener(resultado -> {

                            if (isFinishing() || isDestroyed()) return;

                            limitePresupuesto = nuevoLimite;

                            mostrarLimitePresupuesto();

                            // Recalcular el saldo con el nuevo límite
                            actualizarResumenGastos();

                            dialogo.dismiss();

                            Toast.makeText(
                                    this,
                                    "Presupuesto actualizado en Firebase",
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .addOnFailureListener(error -> {

                            dialogo.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            ).setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "No se pudo actualizar el presupuesto",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            });
        });

        dialogo.show();
    }

    private void mostrarMensajePresupuesto(String mensaje) {

        TextView texto =
                findViewById(R.id.tvLimiteTotal);

        texto.setText(mensaje);
    }

    // ==========================================
    // ACTIVAR REGISTRO DE GASTOS
    // ==========================================

    private void configurarRegistroGastos() {

        Button boton = findViewById(R.id.btnRegistrarGasto);

        boton.setOnClickListener(v -> registrarNuevoGasto());
    }

    // ==========================================
    // REGISTRAR UN GASTO EN FIREBASE
    // ==========================================

    private void registrarNuevoGasto() {

        if (guardandoGasto) return;

        if (viajeSeleccionado == null) {
            Toast.makeText(
                    this,
                    "Primero seleccioná un viaje",
                    Toast.LENGTH_LONG
            ).show();
            cargarViajesDisponibles();
            return;
        }

        EditText campoMonto = findViewById(R.id.etMontoGasto);
        EditText campoConcepto = findViewById(R.id.etConceptoGasto);


        String montoTexto =
                campoMonto.getText().toString().trim();

        String concepto =
                campoConcepto.getText().toString().trim();

        if (montoTexto.isEmpty()) {
            campoMonto.setError("Ingresá el monto");
            campoMonto.requestFocus();
            return;
        }

        long monto;

        try {
            monto = Long.parseLong(montoTexto);

            if (monto <= 0) {
                campoMonto.setError(
                        "El monto debe ser mayor a cero"
                );
                return;
            }

        } catch (NumberFormatException e) {
            campoMonto.setError(
                    "Ingresá un importe válido en pesos enteros"
            );
            return;
        }

        if (concepto.isEmpty()) {
            campoConcepto.setError(
                    "Ingresá el concepto o lugar"
            );
            campoConcepto.requestFocus();
            return;
        }


        String categoria = obtenerCategoriaSeleccionada();

        boolean imprevisto =
                categoria.equals("Gasto no planificado o imprevisto");


        java.util.Map<String, Object> datos =
                new java.util.HashMap<>();

        datos.put("monto", monto);
        datos.put("concepto", concepto);
        datos.put("categoria", categoria);
        datos.put("imprevisto", imprevisto);
        datos.put("fecha", FieldValue.serverTimestamp());

        // Conservar la referencia al viaje del gasto
        DocumentReference referencia = viajeSeleccionado;

        Button boton = findViewById(R.id.btnRegistrarGasto);

        guardandoGasto = true;
        boton.setEnabled(false);

        referencia.collection("gastos")
                .add(datos)
                .addOnSuccessListener(documento -> {

                    guardandoGasto = false;
                    boton.setEnabled(true);

                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(
                            this,
                            "Gasto registrado correctamente",
                            Toast.LENGTH_SHORT
                    ).show();

                    campoMonto.setText("");
                    campoConcepto.setText("");


                    // Volver a seleccionar Comida
                    RadioButton comida =
                            findViewById(R.id.rbComida);
                    comida.performClick();

                    // Actualizar únicamente el viaje correspondiente
                    if (viajeSeleccionado != null
                            && viajeSeleccionado.getPath()
                            .equals(referencia.getPath())) {

                        cargarGastosDelViaje();
                    }

                })
                .addOnFailureListener(error -> {

                    guardandoGasto = false;
                    boton.setEnabled(true);

                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(
                            this,
                            "No se pudo registrar el gasto. "
                                    + "Revisá tu conexión.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ==========================================
    // OBTENER UNA SOLA CATEGORÍA
    // ==========================================

    private String obtenerCategoriaSeleccionada() {

        if (((RadioButton) findViewById(R.id.rbTransporte))
                .isChecked()) {
            return "Transporte";
        }

        if (((RadioButton) findViewById(R.id.rbTours))
                .isChecked()) {
            return "Tours";
        }

        if (((RadioButton) findViewById(R.id.rbAlojamiento))
                .isChecked()) {
            return "Alojamiento";
        }

        if (((RadioButton) findViewById(R.id.rbSouvenirs))
                .isChecked()) {
            return "Souvenirs y recuerdos";
        }

        if (((RadioButton) findViewById(R.id.rbOtros))
                .isChecked()) {
            return "Otros";
        }

        if (((RadioButton) findViewById(R.id.rbImprevisto))
                .isChecked()) {
            return "Gasto no planificado o imprevisto";
        }

        return "Comida";
    }


    // ==========================================
    // CARGAR GASTOS DEL VIAJE
    // ==========================================

    private void cargarGastosDelViaje() {

        if (viajeSeleccionado == null) return;

        DocumentReference referencia = viajeSeleccionado;

        // Limpiar el historial del viaje anterior
        LinearLayout contenedor =
                findViewById(R.id.contenedorHistorialGastos);

        while (contenedor.getChildCount() > 1) {
            contenedor.removeViewAt(1);
        }

        TextView vacio = findViewById(R.id.tvHistorialVacio);

        vacio.setVisibility(View.VISIBLE);
        vacio.setText("Cargando gastos del viaje...");

        referencia.collection("gastos")
                .get()
                .addOnSuccessListener(resultado -> {

                    if (isFinishing() || isDestroyed()) return;

                    // Evitar mostrar gastos de otro viaje
                    if (viajeSeleccionado == null
                            || !viajeSeleccionado.getPath()
                            .equals(referencia.getPath())) {
                        return;
                    }

                    while (contenedor.getChildCount() > 1) {
                        contenedor.removeViewAt(1);
                    }

                    totalGastado = 0L;

                    List<DocumentSnapshot> gastos =
                            new ArrayList<>(resultado.getDocuments());

                    // Gastos más recientes primero
                    gastos.sort((a, b) -> {
                        java.util.Date fa = a.getDate("fecha");
                        java.util.Date fb = b.getDate("fecha");

                        if (fa == null && fb == null) return 0;
                        if (fa == null) return 1;
                        if (fb == null) return -1;

                        return fb.compareTo(fa);
                    });

                    if (gastos.isEmpty()) {

                        vacio.setVisibility(View.VISIBLE);
                        vacio.setText(
                                "Todavía no registraste gastos para este viaje."
                        );

                    } else {

                        vacio.setVisibility(View.GONE);

                        for (DocumentSnapshot gasto : gastos) {

                            Long importe = gasto.getLong("monto");

                            if (importe == null || importe <= 0) {
                                continue;
                            }

                            try {
                                totalGastado = Math.addExact(
                                        totalGastado, importe
                                );
                            } catch (ArithmeticException e) {
                                vacio.setVisibility(View.VISIBLE);
                                vacio.setText(
                                        "El total de gastos supera "
                                                + "el importe admitido."
                                );
                                return;
                            }

                            agregarGastoAlHistorial(gasto);
                        }
                    }

                    actualizarResumenGastos();

                    // Adaptar las tarjetas nuevas al contraste actual
                    aplicarTemaContraste();
                    actualizarTamanoTextos(pantallaPrincipal);

                })
                .addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (viajeSeleccionado == null
                            || !viajeSeleccionado.getPath()
                            .equals(referencia.getPath())) {
                        return;
                    }

                    vacio.setVisibility(View.VISIBLE);
                    vacio.setText(
                            "No se pudieron cargar los gastos. "
                                    + "Revisá tu conexión."
                    );
                });
    }

    // ==========================================
    // CREAR UNA TARJETA EN EL HISTORIAL
    // ==========================================

    private void agregarGastoAlHistorial(
            DocumentSnapshot gasto) {

        LinearLayout contenedor =
                findViewById(R.id.contenedorHistorialGastos);

        String concepto = gasto.getString("concepto");
        String categoria = gasto.getString("categoria");

        if (concepto == null) concepto = "Gasto";
        if (categoria == null) categoria = "Sin categoría";

        Long importe = gasto.getLong("monto");
        Boolean imprevisto = gasto.getBoolean("imprevisto");

        java.util.Date fecha = gasto.getDate("fecha");

        String detalle = categoria;

        if (Boolean.TRUE.equals(imprevisto)) {
            detalle += " · Imprevisto";
        }

        if (fecha != null) {
            java.text.SimpleDateFormat formatoFecha =
                    new java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            new Locale("es", "AR")
                    );

            detalle += " · " + formatoFecha.format(fecha);
        }

        MaterialCardView tarjeta = new MaterialCardView(this);

        tarjeta.setCardBackgroundColor(Color.WHITE);
        tarjeta.setRadius(dp(15));
        tarjeta.setCardElevation(dp(1));
        tarjeta.setStrokeColor(
                Color.parseColor("#93C5FD")
        );
        tarjeta.setStrokeWidth(dp(2));

        LinearLayout.LayoutParams parametros =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        parametros.setMargins(0, 0, 0, dp(10));
        tarjeta.setLayoutParams(parametros);

        LinearLayout contenido = new LinearLayout(this);

        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(
                dp(16), dp(12), dp(16), dp(12)
        );

        TextView titulo = new TextView(this);
        titulo.setText(concepto);
        titulo.setTextSize(15);
        titulo.setTextColor(Color.parseColor("#0C2340"));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView descripcion = new TextView(this);
        descripcion.setText(detalle);
        descripcion.setTextSize(12);
        descripcion.setTextColor(Color.parseColor("#64748B"));

        TextView monto = new TextView(this);
        monto.setText(
                "− " + formatearPesos(importe == null ? 0 : importe)
        );
        monto.setTextSize(17);
        monto.setTextColor(Color.parseColor("#1D4ED8"));
        monto.setTypeface(null, android.graphics.Typeface.BOLD);

        contenido.addView(titulo);
        contenido.addView(descripcion);
        contenido.addView(monto);


        // BOTONES PARA EDITAR Y ELIMINAR CADA GASTO

        LinearLayout acciones = new LinearLayout(this);
        acciones.setOrientation(LinearLayout.HORIZONTAL);
        acciones.setGravity(
                android.view.Gravity.END
                        | android.view.Gravity.CENTER_VERTICAL
        );

        // BOTÓN EDITAR
        TextView editar = new TextView(this);

        editar.setText("Editar");
        editar.setTextSize(14);
        editar.setTextColor(Color.parseColor("#1D4ED8"));
        editar.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        editar.setGravity(android.view.Gravity.CENTER);
        editar.setMinHeight(dp(48));
        editar.setPadding(dp(14), 0, dp(14), 0);
        editar.setClickable(true);
        editar.setFocusable(true);
        editar.setContentDescription(
                "Editar gasto: " + concepto
        );

        editar.setOnClickListener(v -> editarGasto(gasto));

        // BOTÓN ELIMINAR
        TextView eliminar = new TextView(this);

        eliminar.setText("Eliminar");
        eliminar.setTextSize(14);
        eliminar.setTextColor(Color.parseColor("#B91C1C"));
        eliminar.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        eliminar.setGravity(android.view.Gravity.CENTER);
        eliminar.setMinHeight(dp(48));
        eliminar.setPadding(dp(14), 0, dp(14), 0);
        eliminar.setClickable(true);
        eliminar.setFocusable(true);
        eliminar.setContentDescription(
                "Eliminar gasto: " + concepto
        );

        eliminar.setOnClickListener(
                v -> confirmarEliminarGasto(gasto)
        );

        acciones.addView(editar);
        acciones.addView(eliminar);

        contenido.addView(acciones);


        tarjeta.addView(contenido);
        contenedor.addView(tarjeta);

        // Guardar su diseño normal para los cambios de contraste
        guardarOriginales(tarjeta);
    }


    // ==========================================
    // EDITAR UN GASTO EXISTENTE
    // ==========================================

    private void editarGasto(DocumentSnapshot gasto) {

        if (viajeSeleccionado == null || guardandoGasto) return;

        DocumentReference referencia = gasto.getReference();
        DocumentReference viajeDelGasto = viajeSeleccionado;

        // CAMPO CONCEPTO
        EditText conceptoCampo = new EditText(this);
        conceptoCampo.setHint("Concepto o lugar");
        conceptoCampo.setSingleLine(true);
        conceptoCampo.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        conceptoCampo.setText(gasto.getString("concepto"));

        // CAMPO MONTO
        EditText montoCampo = new EditText(this);
        montoCampo.setHint("Monto en pesos");
        montoCampo.setSingleLine(true);
        montoCampo.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        Long montoAnterior = gasto.getLong("monto");

        if (montoAnterior != null) {
            montoCampo.setText(String.valueOf(montoAnterior));
        }

        // SELECCIONAR CATEGORÍA
        String[] categorias = {
                "Comida",
                "Transporte",
                "Tours",
                "Alojamiento",
                "Souvenirs y recuerdos",
                "Otros",
                "Gasto no planificado o imprevisto"
        };


        android.widget.Spinner selector =
                new android.widget.Spinner(this);

        android.widget.ArrayAdapter<String> adaptador =
                new android.widget.ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categorias
                );

        adaptador.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        selector.setAdapter(adaptador);

        String categoriaAnterior =
                gasto.getString("categoria");


        if (Boolean.TRUE.equals(gasto.getBoolean("imprevisto"))) {
            categoriaAnterior = "Gasto no planificado o imprevisto";
        }


        for (int i = 0; i < categorias.length; i++) {

            if (categorias[i].equals(categoriaAnterior)) {
                selector.setSelection(i);
                break;
            }
        }


        // ARMAR FORMULARIO
        LinearLayout formulario = new LinearLayout(this);

        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(
                dp(20), dp(8), dp(20), dp(4)
        );

        formulario.addView(conceptoCampo);
        formulario.addView(montoCampo);
        formulario.addView(selector);


        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar gasto")
                .setView(formulario)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialogo.setOnShowListener(v -> {

            dialogo.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(b -> {

                String nuevoConcepto =
                        conceptoCampo.getText().toString().trim();

                if (nuevoConcepto.isEmpty()) {
                    conceptoCampo.setError("Ingresá el concepto");
                    conceptoCampo.requestFocus();
                    return;
                }

                long nuevoMonto;

                try {
                    nuevoMonto = Long.parseLong(
                            montoCampo.getText().toString().trim()
                    );

                    if (nuevoMonto <= 0) {
                        montoCampo.setError(
                                "El monto debe ser mayor a cero"
                        );
                        return;
                    }

                } catch (NumberFormatException e) {

                    montoCampo.setError(
                            "Ingresá un monto válido en pesos enteros"
                    );
                    return;
                }

                dialogo.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setEnabled(false);

                // ACTUALIZAR EL GASTO EN FIREBASE
                referencia.update(
                        "concepto", nuevoConcepto,
                        "monto", nuevoMonto,
                        "categoria", selector.getSelectedItem().toString(),
                        "imprevisto",
                        categorias[selector.getSelectedItemPosition()]
                                .equals("Gasto no planificado o imprevisto")
                ).addOnSuccessListener(ignorado -> {

                    if (isFinishing() || isDestroyed()) return;

                    dialogo.dismiss();

                    Toast.makeText(
                            this,
                            "Gasto actualizado",
                            Toast.LENGTH_SHORT
                    ).show();

                    // ACTUALIZAR HISTORIAL Y SALDO
                    if (viajeSeleccionado != null
                            && viajeSeleccionado.getPath()
                            .equals(viajeDelGasto.getPath())) {

                        cargarGastosDelViaje();
                    }

                }).addOnFailureListener(error -> {

                    if (isFinishing() || isDestroyed()) return;

                    dialogo.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setEnabled(true);

                    Toast.makeText(
                            this,
                            "No se pudo actualizar el gasto",
                            Toast.LENGTH_LONG
                    ).show();
                });
            });
        });

        dialogo.show();
    }

    // ==========================================
    // ELIMINAR GASTO CON CONFIRMACIÓN
    // ==========================================

    private void confirmarEliminarGasto(DocumentSnapshot gasto) {

        if (viajeSeleccionado == null || guardandoGasto) return;

        DocumentReference referencia = gasto.getReference();
        DocumentReference viajeDelGasto = viajeSeleccionado;

        String concepto = gasto.getString("concepto");

        if (concepto == null || concepto.trim().isEmpty()) {
            concepto = "este gasto";
        }

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Eliminar gasto")
                .setMessage(
                        "¿Querés eliminar " + concepto
                                + "? Esta acción no se puede deshacer."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", null)
                .create();

        dialogo.setOnShowListener(v -> {

            dialogo.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(b -> {

                dialogo.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setEnabled(false);

                // ELIMINAR SOLO ESTE GASTO DE FIREBASE
                referencia.delete()
                        .addOnSuccessListener(ignorado -> {

                            if (isFinishing() || isDestroyed()) return;

                            dialogo.dismiss();

                            Toast.makeText(
                                    this,
                                    "Gasto eliminado",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // RECALCULAR EL SALDO
                            if (viajeSeleccionado != null
                                    && viajeSeleccionado.getPath()
                                    .equals(viajeDelGasto.getPath())) {

                                cargarGastosDelViaje();
                            }

                        })
                        .addOnFailureListener(error -> {

                            if (isFinishing() || isDestroyed()) return;

                            dialogo.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            ).setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "No se pudo eliminar el gasto",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            });
        });

        dialogo.show();
    }


    // ==========================================
    // GENERAR PDF DEL VIAJE SELECCIONADO
    // ==========================================

    private void prepararPdfPresupuesto() {

        // Comprobar que el usuario seleccionó un viaje
        if (viajeSeleccionado == null) {
            Toast.makeText(
                    this,
                    "Primero seleccioná un viaje",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        DocumentReference referencia = viajeSeleccionado;

        Button boton = findViewById(R.id.btnDescargarPdf);

        boton.setEnabled(false);

        Toast.makeText(
                this,
                "Preparando el informe del viaje...",
                Toast.LENGTH_SHORT
        ).show();

        // Consultar los datos actualizados del viaje
        referencia.get()
                .addOnSuccessListener(documento -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (!documento.exists()) {

                        boton.setEnabled(true);

                        Toast.makeText(
                                this,
                                "El viaje ya no está disponible",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    // Recuperar todos los gastos guardados en Firebase
                    referencia.collection("gastos")
                            .get()
                            .addOnSuccessListener(resultado -> {

                                if (isFinishing() || isDestroyed()) return;

                                boton.setEnabled(true);

                                try {

                                    String destino =
                                            documento.getString("destino");

                                    Long presupuesto =
                                            documento.getLong("presupuestoTotal");

                                    // Generar el PDF con el logo y los gastos
                                    pdfPendiente =
                                            PdfPresupuestoGenerator.crear(
                                                    this,
                                                    destino,
                                                    presupuesto == null
                                                            ? 0L
                                                            : presupuesto,
                                                    resultado.getDocuments()
                                            );

                                    // Nombre del archivo
                                    String nombreDestino =
                                            destino == null
                                                    ? "Viaje"
                                                    : destino.replaceAll(
                                                    "[^a-zA-Z0-9_-]",
                                                    "_"
                                            );

                                    String nombreArchivo =
                                            "ViajAPP_" + nombreDestino + ".pdf";

                                    // Abrir la ventana de Android para guardar
                                    Intent guardar = new Intent(
                                            Intent.ACTION_CREATE_DOCUMENT
                                    );

                                    guardar.addCategory(
                                            Intent.CATEGORY_OPENABLE
                                    );

                                    guardar.setType("application/pdf");

                                    guardar.putExtra(
                                            Intent.EXTRA_TITLE,
                                            nombreArchivo
                                    );

                                    guardarPdfLauncher.launch(guardar);

                                } catch (Exception e) {

                                    pdfPendiente = null;

                                    Toast.makeText(
                                            this,
                                            "No se pudo generar el PDF",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            })
                            .addOnFailureListener(error -> {

                                boton.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        "No se pudieron cargar los gastos",
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                })
                .addOnFailureListener(error -> {

                    boton.setEnabled(true);

                    Toast.makeText(
                            this,
                            "No se pudo consultar el presupuesto",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // ==========================================
    // FORMATO DE DINERO ARGENTINO
    // ==========================================

    private String formatearPesos(long importe) {

        NumberFormat formato =
                NumberFormat.getNumberInstance(
                        new Locale("es", "AR")
                );

        formato.setMaximumFractionDigits(0);

        return "$" + formato.format(importe);
    }

    // ==========================================
    // ACTUALIZAR CÁLCULOS Y RECOMENDACIONES
    // ==========================================

    private void actualizarResumenGastos() {

        long saldo = limitePresupuesto - totalGastado;

        int porcentaje = 0;

        if (limitePresupuesto > 0) {
            porcentaje = (int) Math.min(
                    100,
                    Math.round(
                            (double) totalGastado * 100.0
                                    / limitePresupuesto
                    )
            );
        }

        double porcentajeReal = limitePresupuesto > 0
                ? (double) totalGastado * 100.0
                  / limitePresupuesto
                : 0;

        TextView tvGastado =
                findViewById(R.id.tvTotalGastado);

        TextView tvSaldo =
                findViewById(R.id.tvSaldoRestante);

        TextView tvConsumido =
                findViewById(R.id.tvPorcentajeConsumido);

        TextView tvBarra =
                findViewById(R.id.tvPorcentajeBarra);

        TextView tvEstado =
                findViewById(R.id.tvEstadoSaldo);

        TextView tvAviso =
                findViewById(R.id.tvAvisoPresupuesto);

        TextView tvRecomendacion =
                findViewById(R.id.tvRecomendacionGasto);

        ProgressBar barra =
                findViewById(R.id.progresoPresupuesto);

        tvGastado.setText(formatearPesos(totalGastado));

        tvSaldo.setText(formatearPesos(saldo));

        barra.setProgress(porcentaje);

        if (limitePresupuesto <= 0) {

            tvConsumido.setText("Sin límite definido");
            tvBarra.setText("0%");
            tvEstado.setText("Sin presupuesto");

            tvAviso.setText(
                    "Establecé el límite del viaje con Editar."
            );

            tvRecomendacion.setText(
                    "Definí el presupuesto para consultar "
                            + "tu saldo y controlar el ritmo de gastos."
            );

            return;
        }

        String porcentajeTexto = String.format(
                new Locale("es", "AR"),
                "%.1f%%",
                porcentajeReal
        );

        tvConsumido.setText(
                porcentajeTexto + " consumido"
        );

        tvBarra.setText(porcentajeTexto);

        if (totalGastado == 0) {

            tvEstado.setText("Sin gastos");

            tvAviso.setText(
                    "✓ Todavía tenés todo tu presupuesto disponible."
            );

            tvRecomendacion.setText(
                    "Todavía no registraste gastos. "
                            + "Podés comenzar cuando realices tu primer consumo."
            );

        } else if (saldo < 0) {

            tvEstado.setText("Presupuesto superado");

            tvAviso.setText(
                    "⚠ Superaste tu presupuesto en "
                            + formatearPesos(-saldo)
            );

            tvRecomendacion.setText(
                    "Tus gastos superaron el límite planificado. "
                            + "Revisá tus próximos consumos."
            );

        } else if (porcentajeReal >= 80) {

            tvEstado.setText("Saldo bajo");

            tvAviso.setText(
                    "⚠ Ya utilizaste gran parte de tu presupuesto."
            );

            tvRecomendacion.setText(
                    "Te queda " + formatearPesos(saldo)
                            + ". Conviene controlar los próximos gastos."
            );

        } else {

            tvEstado.setText("Dentro del presupuesto");

            tvAviso.setText(
                    "✓ Tus gastos están dentro del presupuesto."
            );

            tvRecomendacion.setText(
                    "Llevás gastado " + porcentajeTexto
                            + " de tu presupuesto. Te quedan "
                            + formatearPesos(saldo) + "."
            );
        }
    }


    private void configurarNavegacion() {

        findViewById(R.id.navAccesibilidad)
                .setOnClickListener(v ->
                        abrirPantalla(AccesibilidadActivity.class));

        findViewById(R.id.navPlanear)
                .setOnClickListener(v ->
                        abrirPantalla(PlanearActivity.class));

        findViewById(R.id.navItinerario)
                .setOnClickListener(v ->
                        abrirPantalla(ItinerarioActivity.class));

        findViewById(R.id.navLugares)
                .setOnClickListener(v ->
                        abrirPantalla(LugaresActivity.class));
    }

    private void abrirPantalla(Class<?> destino) {
        startActivity(new Intent(this, destino));
    }

    // ==========================================
    // PERFIL DEL USUARIO
    // ==========================================

    private void configurarPerfil() {

        View btnAbrirPerfil = findViewById(R.id.btnAbrirPerfil);

        btnAbrirPerfil.setOnClickListener(v -> {

            MenuPerfilUsuario.mostrar(
                    PresupuestoActivity.this,
                    btnAbrirPerfil,
                    modoContraste,
                    escalaTexto
            );
        });
    }

    // ==========================================
    // BARRA DE ACCESIBILIDAD
    // ==========================================

    private void configurarBarraAccesibilidad() {

        btnAccesibilidad = findViewById(R.id.btnAccesibilidad);
        btnDisminuirTexto = findViewById(R.id.btnDisminuirTexto);
        btnAumentarTexto = findViewById(R.id.btnAumentarTexto);
        btnContraste = findViewById(R.id.btnContraste);
        btnAudio = findViewById(R.id.btnAudio);

        // Abrir o cerrar controles
        btnAccesibilidad.setOnClickListener(v -> {

            controlesVisibles = !controlesVisibles;

            int visibilidad = controlesVisibles
                    ? View.VISIBLE
                    : View.GONE;

            btnDisminuirTexto.setVisibility(visibilidad);
            btnAumentarTexto.setVisibility(visibilidad);
            btnContraste.setVisibility(visibilidad);
            btnAudio.setVisibility(visibilidad);

            LinearLayout.LayoutParams parametrosBoton =
                    (LinearLayout.LayoutParams)
                            btnAccesibilidad.getLayoutParams();

            parametrosBoton.width = controlesVisibles
                    ? 0
                    : LinearLayout.LayoutParams.WRAP_CONTENT;

            parametrosBoton.weight =
                    controlesVisibles ? 1f : 0f;

            btnAccesibilidad.setLayoutParams(parametrosBoton);

            int espacio = dp(12);

            btnAccesibilidad.setPadding(
                    controlesVisibles ? 0 : espacio,
                    0,
                    controlesVisibles ? 0 : espacio,
                    0
            );

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    parametrosBarra =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                            barraAccesibilidad.getLayoutParams();

            parametrosBarra.width = controlesVisibles
                    ? 0
                    : androidx.constraintlayout.widget.ConstraintLayout
                      .LayoutParams.WRAP_CONTENT;

            barraAccesibilidad.setLayoutParams(parametrosBarra);

            btnAccesibilidad.setContentDescription(
                    controlesVisibles
                            ? "Cerrar menú de accesibilidad"
                            : "Abrir menú de accesibilidad"
            );
        });

        // A+
        btnAumentarTexto.setOnClickListener(v -> {

            escalaTexto = Math.min(
                    1.3f,
                    Math.round((escalaTexto + 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(pantallaPrincipal);

            PreferenciasAccesibilidad.guardarEscalaTexto(
                    this, escalaTexto
            );
        });

        // A-
        btnDisminuirTexto.setOnClickListener(v -> {

            escalaTexto = Math.max(
                    0.9f,
                    Math.round((escalaTexto - 0.1f) * 10f) / 10f
            );

            actualizarTamanoTextos(pantallaPrincipal);

            PreferenciasAccesibilidad.guardarEscalaTexto(
                    this, escalaTexto
            );
        });

        // NORMAL -> CREMA -> OSCURO -> NORMAL
        btnContraste.setOnClickListener(v -> {

            modoContraste = (modoContraste + 1) % 3;

            aplicarTemaContraste();

            PreferenciasAccesibilidad.guardarContraste(
                    this, modoContraste
            );
        });

        // Audio pendiente
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

    // ==========================================
    // GUARDAR DISEÑO ORIGINAL
    // ==========================================

    private void guardarOriginales(View vista) {

        if (vista == null) return;

        Drawable fondo = vista.getBackground();

        fondosOriginales.put(vista, copiarFondo(fondo));

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            coloresOriginales.put(
                    texto, texto.getTextColors()
            );

            tamanosOriginales.put(
                    texto, texto.getTextSize()
            );
        }

        if (vista instanceof Button) {

            Button boton = (Button) vista;

            tintesOriginales.put(
                    boton, boton.getBackgroundTintList()
            );
        }

        if (vista instanceof MaterialCardView) {

            MaterialCardView tarjeta =
                    (MaterialCardView) vista;

            tarjetasOriginales.put(
                    tarjeta,
                    new int[]{
                            tarjeta.getCardBackgroundColor()
                                    .getDefaultColor(),
                            tarjeta.getStrokeColor(),
                            tarjeta.getStrokeWidth()
                    }
            );
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                guardarOriginales(grupo.getChildAt(i));
            }
        }
    }

    private Drawable copiarFondo(Drawable fondo) {

        if (fondo == null) return null;

        if (fondo.getConstantState() != null) {
            return fondo.getConstantState()
                    .newDrawable()
                    .mutate();
        }

        return fondo.mutate();
    }

    // ==========================================
    // TAMAÑO DE TEXTO
    // ==========================================

    private void actualizarTamanoTextos(View vista) {

        if (vista == null) return;

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

    // ==========================================
    // RESTAURAR DISEÑO ORIGINAL
    // ==========================================

    private void restaurarDisenoOriginal(View vista) {

        if (vista == null) return;

        if (!(vista instanceof MaterialCardView)) {
            vista.setBackground(
                    copiarFondo(fondosOriginales.get(vista))
            );
        }

        if (vista instanceof TextView) {

            TextView texto = (TextView) vista;

            ColorStateList color =
                    coloresOriginales.get(texto);

            if (color != null) {
                texto.setTextColor(color);
            }
        }

        if (vista instanceof Button) {

            Button boton = (Button) vista;

            boton.setBackgroundTintList(
                    tintesOriginales.get(boton)
            );
        }

        if (vista instanceof MaterialCardView) {

            MaterialCardView tarjeta =
                    (MaterialCardView) vista;

            int[] original = tarjetasOriginales.get(tarjeta);

            if (original != null) {
                tarjeta.setCardBackgroundColor(original[0]);
                tarjeta.setStrokeColor(original[1]);
                tarjeta.setStrokeWidth(original[2]);
            }
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {
                restaurarDisenoOriginal(grupo.getChildAt(i));
            }
        }
    }

    // ==========================================
    // CONTRASTE
    // ==========================================


    private void aplicarTemaContraste() {

        // Recuperar primero el diseño original
        restaurarDisenoOriginal(pantallaPrincipal);


// COLORES DEL SELECTOR DE VIAJES
        com.google.android.material.textfield.MaterialAutoCompleteTextView selectorViajes =
                findViewById(R.id.selectorViajes);

        com.google.android.material.textfield.TextInputLayout contenedorSelector =
                findViewById(R.id.contenedorSelectorViaje);

        int colorSelector = Color.parseColor(
                modoContraste == 1 ? "#F3DFC0" :
                        modoContraste == 2 ? "#2A2A2A" : "#EFF6FF"
        );

        int bordeSelector = Color.parseColor(
                modoContraste == 1 ? "#96734D" :
                        modoContraste == 2 ? "#888888" : "#60A5FA"
        );

        int textoSelector = modoContraste == 2
                ? Color.WHITE
                : Color.parseColor("#222222");

        contenedorSelector.setBoxBackgroundColor(colorSelector);
        contenedorSelector.setBoxStrokeColor(bordeSelector);

        selectorViajes.setTextColor(textoSelector);
        selectorViajes.setHintTextColor(textoSelector);

        selectorViajes.setDropDownBackgroundDrawable(
                fondoRedondeado(colorSelector, bordeSelector, 12, 1)
        );


        if (modoContraste == 0) {


            btnContraste.setText("◐  Contraste");

            // Recuperar el color original del selector de viajes
            com.google.android.material.textfield.MaterialAutoCompleteTextView selector =
                    findViewById(R.id.selectorViajes);

            selector.setHintTextColor(
                    Color.parseColor("#64748B")
            );


            // Recuperar los circulitos azules de las categorías
            int[] categorias = {
                    R.id.rbComida,
                    R.id.rbTransporte,
                    R.id.rbTours,
                    R.id.rbAlojamiento,
                    R.id.rbSouvenirs,
                    R.id.rbOtros,
                    R.id.rbImprevisto
            };

            for (int idCategoria : categorias) {
                RadioButton opcion = findViewById(idCategoria);

                opcion.setButtonTintList(
                        ColorStateList.valueOf(
                                Color.parseColor("#2563EB")
                        )
                );
            }

            // Recuperar el color legible de los ejemplos
            EditText monto = findViewById(R.id.etMontoGasto);
            EditText concepto = findViewById(R.id.etConceptoGasto);

            monto.setHintTextColor(
                    Color.parseColor("#64748B")
            );

            concepto.setHintTextColor(
                    Color.parseColor("#64748B")
            );



            // Recuperar el color original del ícono PDF
            android.widget.ImageView iconoPdf =
                    findViewById(R.id.imgDocumentoPdf);

            iconoPdf.setImageTintList(null);

            return;
        }


        boolean crema = modoContraste == 1;

        int fondoPantalla = Color.parseColor(
                crema ? "#FFF1D6" : "#111111"
        );

        int fondoTarjeta = Color.parseColor(
                crema ? "#EBD6B4" : "#222222"
        );

        int borde = Color.parseColor(
                crema ? "#C7AC88" : "#555555"
        );

        int textoPrincipal = Color.parseColor(
                crema ? "#222222" : "#FFFFFF"
        );

        int textoSecundario = Color.parseColor(
                crema ? "#5A5146" : "#D0D0D0"
        );

        // Adaptar los elementos de la pantalla
        aplicarColoresRecursivos(
                pantallaPrincipal,
                fondoTarjeta,
                borde,
                textoPrincipal,
                textoSecundario,
                crema
        );

        findViewById(R.id.main)
                .setBackgroundColor(fondoPantalla);

        findViewById(R.id.scrollContenido)
                .setBackgroundColor(fondoPantalla);

        findViewById(R.id.header)
                .setBackgroundColor(fondoTarjeta);

        findViewById(R.id.bottomNav)
                .setBackgroundColor(fondoTarjeta);

        barraAccesibilidad.setBackground(
                fondoRedondeado(
                        fondoTarjeta,
                        crema
                                ? Color.parseColor("#96734D")
                                : Color.parseColor("#777777"),
                        16,
                        2
                )
        );

        btnAccesibilidad.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnDisminuirTexto.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnAumentarTexto.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnContraste.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnAudio.setBackground(
                fondoRedondeado(fondoTarjeta, borde, 12)
        );

        btnContraste.setText(
                crema ? "◐  Crema" : "◐  Oscuro"
        );
        // Adaptar los elementos que tienen fondos especiales
        adaptarElementosEspeciales(crema);
    }


    private void aplicarColoresRecursivos(
            View vista,
            int fondoTarjeta,
            int borde,
            int textoPrincipal,
            int textoSecundario,
            boolean crema) {

        if (vista == null) return;

        int bordeVisible = Color.parseColor(
                crema ? "#96734D" : "#777777"
        );

        // Tarjetas Material: bordes completos y visibles
        if (vista instanceof MaterialCardView) {

            MaterialCardView tarjeta =
                    (MaterialCardView) vista;

            tarjeta.setCardBackgroundColor(fondoTarjeta);
            tarjeta.setStrokeColor(bordeVisible);
            tarjeta.setStrokeWidth(dp(2));
        }

        // Solo modificar las dos tarjetas grandes.
        // No tocar los LinearLayout interiores.
        if (vista.getId() == R.id.tarjetaResumenPresupuesto
                || vista.getId() == R.id.tarjetaNuevoGasto) {

            vista.setBackground(
                    fondoRedondeado(
                            fondoTarjeta,
                            bordeVisible,
                            16,
                            2
                    )
            );
        }

        if (vista instanceof EditText
                && vista.getId() != R.id.selectorViajes) {

            EditText campo = (EditText) vista;

            campo.setBackground(
                    fondoRedondeado(
                            crema
                                    ? Color.parseColor("#FFF8ED")
                                    : Color.parseColor("#303030"),
                            bordeVisible,
                            12,
                            2
                    )
            );

            campo.setTextColor(textoPrincipal);
            campo.setHintTextColor(textoSecundario);

        } else if (vista instanceof android.widget.RadioButton) {

            android.widget.RadioButton opcion =
                    (android.widget.RadioButton) vista;

            opcion.setBackground(
                    fondoRedondeado(
                            crema
                                    ? Color.parseColor("#FFF8ED")
                                    : Color.parseColor("#303030"),
                            bordeVisible,
                            12,
                            2
                    )
            );

            opcion.setTextColor(textoPrincipal);
            opcion.setButtonTintList(
                    ColorStateList.valueOf(
                            crema
                                    ? Color.parseColor("#825523")
                                    : Color.WHITE
                    )
            );

        } else if (vista instanceof android.widget.CheckBox) {

            android.widget.CheckBox casilla =
                    (android.widget.CheckBox) vista;

            casilla.setBackground(
                    fondoRedondeado(
                            crema
                                    ? Color.parseColor("#FFF8ED")
                                    : Color.parseColor("#303030"),
                            bordeVisible,
                            12,
                            2
                    )
            );

            casilla.setTextColor(textoPrincipal);
            casilla.setButtonTintList(
                    ColorStateList.valueOf(
                            crema
                                    ? Color.parseColor("#825523")
                                    : Color.WHITE
                    )
            );

        } else if (vista instanceof Button) {

            Button boton = (Button) vista;

            boton.setBackgroundTintList(null);

            boton.setBackground(
                    fondoRedondeado(
                            crema
                                    ? Color.parseColor("#825523")
                                    : Color.parseColor("#383838"),
                            bordeVisible,
                            12,
                            2
                    )
            );

            boton.setTextColor(Color.WHITE);

        } else if (vista instanceof TextView) {

            TextView texto = (TextView) vista;
            texto.setTextColor(textoPrincipal);
        }

        if (vista instanceof ViewGroup) {

            ViewGroup grupo = (ViewGroup) vista;

            for (int i = 0; i < grupo.getChildCount(); i++) {

                aplicarColoresRecursivos(
                        grupo.getChildAt(i),
                        fondoTarjeta,
                        borde,
                        textoPrincipal,
                        textoSecundario,
                        crema
                );
            }
        }
    }


    // ==========================================
    // CORREGIR FRANJA DE VOZ Y BOTÓN EDITAR
    // ==========================================


    private void adaptarElementosEspeciales(boolean crema) {

        int fondoTarjeta = Color.parseColor(
                crema ? "#EBD6B4" : "#222222"
        );

        int fondoBoton = Color.parseColor(
                crema ? "#825523" : "#383838"
        );

        int borde = Color.parseColor(
                crema ? "#96734D" : "#888888"
        );

        int colorIconos = Color.parseColor(
                crema ? "#825523" : "#FFFFFF"
        );

        // FRANJA DE REGISTRO POR VOZ
        View franja = findViewById(R.id.tarjetaRegistroVoz);

        franja.setBackground(
                fondoRedondeado(
                        fondoTarjeta, borde, 16, 2
                )
        );

        // BOTÓN EDITAR
        android.widget.Button editar =
                findViewById(R.id.btnEditarPresupuesto);

        editar.setBackgroundTintList(null);

        editar.setBackground(
                fondoRedondeado(
                        fondoBoton, borde, 12, 2
                )
        );

        editar.setTextColor(Color.WHITE);

        // BOTÓN HABLAR
        android.widget.Button hablar =
                findViewById(R.id.btnRegistrarVoz);

        hablar.setBackgroundTintList(null);

        hablar.setBackground(
                fondoRedondeado(
                        fondoBoton, borde, 12, 2
                )
        );


        // ==========================================
        // ÍCONO DEL MICRÓFONO
        // ==========================================

        android.widget.ImageView microfono =
                findViewById(R.id.imgMicrofonoPresupuesto);

        microfono.setBackground(
                fondoRedondeado(
                        fondoBoton,
                        borde,
                        12,
                        2
                )
        );

        microfono.setImageTintList(
                ColorStateList.valueOf(Color.WHITE)
        );


        hablar.setTextColor(Color.WHITE);

        // ÍCONO DEL DOCUMENTO PDF
        android.widget.ImageView iconoPdf =
                findViewById(R.id.imgDocumentoPdf);

        iconoPdf.setImageTintList(
                ColorStateList.valueOf(colorIconos)
        );

        iconoPdf.setBackground(
                fondoRedondeado(
                        crema
                                ? Color.parseColor("#FFF8ED")
                                : Color.parseColor("#303030"),
                        borde,
                        12,
                        2
                )
        );

    }

    private GradientDrawable fondoRedondeado(

            int fondo, int borde, int radio) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(fondo);
        drawable.setCornerRadius(dp(radio));
        drawable.setStroke(dp(1), borde);

        return drawable;
    }


    private GradientDrawable fondoRedondeado(
            int fondo,
            int borde,
            int radio,
            int grosor) {

        GradientDrawable drawable = new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fondo);
        drawable.setCornerRadius(dp(radio));
        drawable.setStroke(dp(grosor), borde);

        return drawable;
    }


    private int dp(int valor) {

        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                valor,
                getResources().getDisplayMetrics()
        );
    }
}
