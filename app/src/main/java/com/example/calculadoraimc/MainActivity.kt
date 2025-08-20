package com.example.calculadoraimc

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.calculadoraimc.databinding.ActivityMainBinding
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var splashStartTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar SplashScreen API antes de super
        val splashScreen = installSplashScreen()
        splashStartTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            // Garantiza un mínimo de 120 ms para evitar parpadeo (ajustable)
            System.currentTimeMillis() - splashStartTime < 120
        }

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Overlay visual propio (rellena pantalla con la imagen) también sirve en APIs < 31
        addSplashOverlay()

        // Inicializa Chaquopy si no está iniciado (potencialmente costoso, después del overlay)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val disclaimerAccepted = sharedPreferences.getBoolean("disclaimer_accepted", false)
        if (!disclaimerAccepted) {
            // Retrasa un poco el diálogo para que no aparezca sobre el splash en fade
            binding.root.postDelayed({ mostrarDisclaimer() }, 400)
        }
    }

    private fun addSplashOverlay() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        val overlay = layoutInflater.inflate(R.layout.view_splash_overlay, root, false)
        root.addView(overlay)
        // Fade out tras breve retardo (ajusta tiempos a gusto)
        val MIN_VISIBLE = 1550L // ms que el usuario ve la imagen full-screen
        val fadeDuration = 350L
        overlay.postDelayed({
            overlay.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .withEndAction { root.removeView(overlay) }
                .start()
        }, MIN_VISIBLE)
    }

    private fun mostrarDisclaimer() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(getString(R.string.disclaimer_texto_limpio))
            .setPositiveButton("Aceptar") { _, _ ->
                sharedPreferences.edit().putBoolean("disclaimer_accepted", true).apply()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(this, "Debe aceptar el disclaimer para continuar", Toast.LENGTH_LONG).show()
                binding.root.postDelayed({ finish() }, 1500)
            }
            .setCancelable(false)
            .show()
    }
}