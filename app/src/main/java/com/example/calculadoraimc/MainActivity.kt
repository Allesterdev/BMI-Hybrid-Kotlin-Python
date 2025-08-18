package com.example.calculadoraimc

import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.calculadoraimc.databinding.ActivityMainBinding
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AlertDialog
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa Chaquopy si no está iniciado
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        // Verificar si el disclaimer ya fue aceptado
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val disclaimerAccepted = sharedPreferences.getBoolean("disclaimer_accepted", false)

        if (!disclaimerAccepted) {
            mostrarDisclaimer()
        }
    }

    private fun mostrarDisclaimer() {
        val disclaimerTexto = getString(R.string.disclaimer_texto)
            .replace("[b]", "")
            .replace("[/b]", "")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(disclaimerTexto)
            .setPositiveButton("Aceptar") { _, _ ->
                // Guardar que el disclaimer fue aceptado
                sharedPreferences.edit().putBoolean("disclaimer_accepted", true).apply()
            }
            .setNegativeButton(getString(R.string.btn_salir)) { _, _ ->
                // Mostrar mensaje explicativo antes de cerrar
                Toast.makeText(
                    this,
                    getString(R.string.mensaje_debe_aceptar),
                    Toast.LENGTH_LONG
                ).show()

                // Esperar un momento para que el usuario vea el mensaje antes de cerrar
                binding.root.postDelayed({
                    finish()
                }, 2000)
            }
            .setCancelable(false)
            .show()
    }
}