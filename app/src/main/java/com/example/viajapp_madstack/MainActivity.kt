
package com.example.viajapp_madstack

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abrir la pantalla de Registro de ViajAPP
        startActivity(Intent(this, RegistroActivity::class.java))

        // Cerrar MainActivity para dejar Registro como pantalla activa
        finish()
    }

    /*Probando rama p2. Hola Soy Abi*/
    /*Probando Rama p3.*/
    /*Probando Rama p4.*/
}
