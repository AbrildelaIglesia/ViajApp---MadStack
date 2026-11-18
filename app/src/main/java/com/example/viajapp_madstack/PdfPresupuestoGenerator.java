
package com.example.viajapp_madstack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;

import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PdfPresupuestoGenerator {

    private static final int ANCHO = 595;
    private static final int ALTO = 842;
    private static final int MARGEN = 46;

    private static final int AZUL = Color.rgb(29, 78, 216);
    private static final int OSCURO = Color.rgb(12, 35, 64);
    private static final int GRIS = Color.rgb(71, 85, 105);
    private static final int FONDO = Color.rgb(239, 246, 255);

    private static final Locale LOCALE = new Locale("es", "AR");

    private final Context contexto;
    private final PdfDocument pdf = new PdfDocument();
    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PdfDocument.Page pagina;
    private Canvas canvas;

    private float y;
    private int numeroPagina;

    private PdfPresupuestoGenerator(Context contexto) {
        this.contexto = contexto;
    }

    public static byte[] crear(
            Context contexto,
            String destino,
            long presupuesto,
            List<DocumentSnapshot> gastos
    ) throws IOException {

        PdfPresupuestoGenerator generador =
                new PdfPresupuestoGenerator(contexto);

        try {
            return generador.generar(destino, presupuesto, gastos);
        } finally {
            generador.pdf.close();
        }
    }

    private byte[] generar(
            String destino,
            long presupuesto,
            List<DocumentSnapshot> gastosOriginales
    ) throws IOException {

        List<DocumentSnapshot> gastos =
                new ArrayList<>(gastosOriginales);

        // Ordenar los gastos por fecha, del más reciente al más antiguo.
        gastos.sort((a, b) -> {

            Date fa = a.getDate("fecha");
            Date fb = b.getDate("fecha");

            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;

            return fb.compareTo(fa);
        });

        long total = 0;
        int cantidad = 0;

        // Calcular el total de gastos.
        for (DocumentSnapshot gasto : gastos) {

            Long monto = gasto.getLong("monto");

            if (monto != null && monto > 0) {

                total = Math.addExact(total, monto);
                cantidad++;
            }
        }

        long saldo = Math.subtractExact(presupuesto, total);

        // PRIMERA PÁGINA
        nuevaPagina();

        titulo("Informe de presupuesto y gastos", 20);

        y += 7;

        etiqueta("DESTINO DEL VIAJE");

        textoEnLineas(
                destino == null || destino.trim().isEmpty()
                        ? "Viaje sin destino"
                        : destino,
                16,
                OSCURO,
                true,
                ANCHO - 2 * MARGEN,
                21
        );

        y += 10;

        // RESUMEN ECONÓMICO
        panelImporte(
                "PRESUPUESTO TOTAL",
                pesos(presupuesto),
                AZUL
        );

        panelImporte(
                "TOTAL GASTADO",
                pesos(total),
                OSCURO
        );

        panelImporte(
                saldo >= 0
                        ? "SALDO DISPONIBLE"
                        : "PRESUPUESTO EXCEDIDO",
                pesos(saldo >= 0 ? saldo : -saldo),
                saldo >= 0
                        ? AZUL
                        : Color.rgb(153, 27, 27)
        );

        y += 13;

        titulo("Detalle de gastos registrados", 16);

        y += 6;

        texto(
                "Cantidad de gastos: " + cantidad,
                MARGEN,
                y,
                11,
                GRIS,
                false
        );

        y += 20;

        // HISTORIAL COMPLETO
        if (cantidad == 0) {

            textoEnLineas(
                    "Todavía no hay gastos registrados para este viaje.",
                    12,
                    GRIS,
                    false,
                    ANCHO - 2 * MARGEN,
                    18
            );

        } else {

            int numero = 0;

            for (DocumentSnapshot gasto : gastos) {

                Long monto = gasto.getLong("monto");

                if (monto == null || monto <= 0) {
                    continue;
                }

                numero++;

                gasto(numero, gasto, monto);
            }
        }

        cerrarPagina();

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        pdf.writeTo(salida);

        return salida.toByteArray();
    }

    // ==========================================
    // CREAR PÁGINA CON LOGO DE VIAJAPP
    // ==========================================

    private void nuevaPagina() {

        if (pagina != null) {
            cerrarPagina();
        }

        numeroPagina++;

        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(
                        ANCHO,
                        ALTO,
                        numeroPagina
                ).create();

        pagina = pdf.startPage(info);

        canvas = pagina.getCanvas();

        canvas.drawColor(Color.WHITE);

        // Logo real de la aplicación.
        Drawable logo = ContextCompat.getDrawable(
                contexto,
                R.drawable.logo_viajapp
        );

        if (logo != null) {

            logo.setBounds(
                    MARGEN,
                    37,
                    MARGEN + 45,
                    82
            );

            logo.draw(canvas);
        }

        texto(
                "ViajAPP",
                MARGEN + 56,
                62,
                18,
                OSCURO,
                true
        );

        texto(
                "PRESUPUESTO Y GASTOS",
                MARGEN + 56,
                78,
                9,
                GRIS,
                false
        );

        linea(97);

        y = 123;
    }

    // ==========================================
    // PIE DE PÁGINA
    // ==========================================

    private void cerrarPagina() {

        if (pagina == null) return;

        linea(ALTO - 52);

        texto(
                "ViajAPP  |  Generado: " + fecha(new Date()),
                MARGEN,
                ALTO - 35,
                9,
                GRIS,
                false
        );

        texto(
                "Página " + numeroPagina,
                ANCHO - MARGEN - 48,
                ALTO - 35,
                9,
                GRIS,
                false
        );

        pdf.finishPage(pagina);

        pagina = null;
        canvas = null;
    }

    private void asegurarEspacio(float altoNecesario) {

        if (y + altoNecesario > ALTO - 75) {
            nuevaPagina();
        }
    }

    // ==========================================
    // IMPORTES GRANDES Y SIN CORTES
    // ==========================================

    private void panelImporte(
            String etiqueta,
            String importe,
            int color
    ) {

        asegurarEspacio(78);

        float arriba = y;

        pintura.setColor(FONDO);
        pintura.setStyle(Paint.Style.FILL);

        canvas.drawRoundRect(
                MARGEN,
                arriba,
                ANCHO - MARGEN,
                arriba + 69,
                10,
                10,
                pintura
        );

        texto(
                etiqueta,
                MARGEN + 16,
                arriba + 21,
                10,
                GRIS,
                true
        );

        // Mantener el importe en una sola línea.
        float tamano = 20;

        while (tamano > 12
                && anchoTexto(importe, tamano, true)
                > ANCHO - 2 * MARGEN - 32) {

            tamano -= 1;
        }

        texto(
                importe,
                MARGEN + 16,
                arriba + 51,
                tamano,
                color,
                true
        );

        y = arriba + 77;
    }

    // ==========================================
    // TARJETA DE CADA GASTO
    // ==========================================

    private void gasto(
            int numero,
            DocumentSnapshot gasto,
            long monto
    ) {

        String concepto = gasto.getString("concepto");

        if (concepto == null || concepto.trim().isEmpty()) {
            concepto = "Gasto sin concepto";
        }

        String categoria = gasto.getString("categoria");

        if (categoria == null || categoria.trim().isEmpty()) {
            categoria = "Sin categoría";
        }

        Date fechaGasto = gasto.getDate("fecha");

        String informacion =
                "Categoría: " + categoria
                        + "  |  Fecha: "
                        + (fechaGasto == null
                        ? "Sin fecha"
                        : fecha(fechaGasto))
                        + "  |  Imprevisto: "
                        + (Boolean.TRUE.equals(
                        gasto.getBoolean("imprevisto"))
                        ? "Sí"
                        : "No");

        int lineasConcepto = contarLineas(
                numero + ". " + concepto,
                13,
                ANCHO - 2 * MARGEN - 28,
                true
        );

        int lineasInfo = contarLineas(
                informacion,
                10,
                ANCHO - 2 * MARGEN - 28,
                false
        );

        float alto =
                18
                        + lineasConcepto * 18
                        + 5
                        + lineasInfo * 15
                        + 8
                        + 18
                        + 15;

        // Evitar cortar un gasto entre páginas.
        asegurarEspacio(alto + 9);

        float arriba = y;

        pintura.setColor(
                numero % 2 == 0
                        ? Color.rgb(248, 250, 252)
                        : FONDO
        );

        pintura.setStyle(Paint.Style.FILL);

        canvas.drawRoundRect(
                MARGEN,
                arriba,
                ANCHO - MARGEN,
                arriba + alto,
                8,
                8,
                pintura
        );

        y += 23;

        textoEnLineas(
                numero + ". " + concepto,
                13,
                OSCURO,
                true,
                ANCHO - 2 * MARGEN - 28,
                18,
                MARGEN + 14
        );

        y += 4;

        textoEnLineas(
                informacion,
                10,
                GRIS,
                false,
                ANCHO - 2 * MARGEN - 28,
                15,
                MARGEN + 14
        );

        y += 6;

        texto(
                pesos(monto),
                MARGEN + 14,
                y,
                15,
                AZUL,
                true
        );

        y = arriba + alto + 9;
    }

    // ==========================================
    // TÍTULOS
    // ==========================================

    private void titulo(String valor, float tamano) {

        asegurarEspacio(tamano + 20);

        texto(
                valor,
                MARGEN,
                y,
                tamano,
                OSCURO,
                true
        );

        y += tamano + 14;
    }

    private void etiqueta(String valor) {

        texto(
                valor,
                MARGEN,
                y,
                10,
                GRIS,
                true
        );

        y += 20;
    }

    private void linea(float posY) {

        pintura.setStrokeWidth(1);
        pintura.setColor(Color.rgb(203, 213, 225));

        canvas.drawLine(
                MARGEN,
                posY,
                ANCHO - MARGEN,
                posY,
                pintura
        );
    }

    // ==========================================
    // ESCRIBIR TEXTO
    // ==========================================

    private void texto(
            String valor,
            float x,
            float base,
            float tamano,
            int color,
            boolean negrita
    ) {

        configurarPintura(tamano, color, negrita);

        canvas.drawText(valor, x, base, pintura);
    }

    private void configurarPintura(
            float tamano,
            int color,
            boolean negrita
    ) {

        pintura.setStyle(Paint.Style.FILL);
        pintura.setColor(color);
        pintura.setTextSize(tamano);

        pintura.setTypeface(
                negrita
                        ? Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
                        : Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                )
        );
    }

    private float anchoTexto(
            String texto,
            float tamano,
            boolean negrita
    ) {

        configurarPintura(tamano, OSCURO, negrita);

        return pintura.measureText(texto);
    }

    private int contarLineas(
            String valor,
            float tamano,
            float ancho,
            boolean negrita
    ) {

        configurarPintura(tamano, OSCURO, negrita);

        return dividirLineas(valor, ancho).size();
    }

    // ==========================================
    // DIVIDIR TEXTOS LARGOS EN LÍNEAS
    // ==========================================

    private void textoEnLineas(
            String valor,
            float tamano,
            int color,
            boolean negrita,
            float ancho,
            float interlineado
    ) {

        textoEnLineas(
                valor,
                tamano,
                color,
                negrita,
                ancho,
                interlineado,
                MARGEN
        );
    }

    private void textoEnLineas(
            String valor,
            float tamano,
            int color,
            boolean negrita,
            float ancho,
            float interlineado,
            float x
    ) {

        configurarPintura(tamano, color, negrita);

        for (String linea : dividirLineas(valor, ancho)) {

            texto(
                    linea,
                    x,
                    y,
                    tamano,
                    color,
                    negrita
            );

            y += interlineado;
        }
    }

    private List<String> dividirLineas(
            String valor,
            float ancho
    ) {

        List<String> lineas = new ArrayList<>();

        StringBuilder actual = new StringBuilder();

        String[] palabras = valor
                .replace('\n', ' ')
                .trim()
                .split("\\s+");

        for (String palabra : palabras) {

            String candidato =
                    actual.length() == 0
                            ? palabra
                            : actual + " " + palabra;

            if (pintura.measureText(candidato) <= ancho) {

                actual.setLength(0);
                actual.append(candidato);

            } else {

                if (actual.length() > 0) {

                    lineas.add(actual.toString());
                    actual.setLength(0);
                }

                String restante = palabra;

                while (!restante.isEmpty()) {

                    int cantidad = pintura.breakText(
                            restante,
                            true,
                            ancho,
                            null
                    );

                    if (cantidad >= restante.length()) {
                        break;
                    }

                    if (cantidad <= 0) {
                        cantidad = 1;
                    }

                    lineas.add(
                            restante.substring(0, cantidad)
                    );

                    restante = restante.substring(cantidad);
                }

                actual.append(restante);
            }
        }

        if (actual.length() > 0) {
            lineas.add(actual.toString());
        }

        if (lineas.isEmpty()) {
            lineas.add("");
        }

        return lineas;
    }

    // ==========================================
    // FECHAS Y FORMATO DE PESOS ARGENTINOS
    // ==========================================

    private static String fecha(Date fecha) {

        return new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                LOCALE
        ).format(fecha);
    }

    private static String pesos(long monto) {

        NumberFormat formato =
                NumberFormat.getIntegerInstance(LOCALE);

        return "$ " + formato.format(monto);
    }
}
