package com.example.calculadoraimc

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.calculadoraimc.databinding.ActivityMainBinding
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_API_MIN_MS = 120L
        private const val OVERLAY_VISIBLE_MS = 1550L
        private const val OVERLAY_FADE_MS = 350L
        private const val DISCLAIMER_EXIT_DELAY_MS = 1500L
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var splashStartTime: Long = 0
    private var disclaimerPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar SplashScreen API antes de super
        val splashScreen = installSplashScreen()
        splashStartTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            // Garantiza un mínimo de 120 ms para evitar parpadeo
            System.currentTimeMillis() - splashStartTime < SPLASH_API_MIN_MS
        }

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Overlay visual propio (rellena pantalla con la imagen) también sirve en APIs < 31
        addSplashOverlay()

        // Python ya está inicializado en CalculadoraIMCApplication

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val disclaimerAccepted = sharedPreferences.getBoolean("disclaimer_accepted", false)
        disclaimerPending = !disclaimerAccepted
        // No delay: si ya aceptado no hacemos nada; si no, se mostrará tras el fade.
    }

    private fun addSplashOverlay() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        if (root.findViewById<ViewGroup?>(R.id.splash_overlay_root) != null) return // ya agregado
        val overlay = layoutInflater.inflate(R.layout.view_splash_overlay, root, false)
        root.addView(overlay)
        // Fade out tras breve retardo (ajusta tiempos a gusto)
        val fadeDuration = OVERLAY_FADE_MS
        overlay.postDelayed({
            overlay.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .withEndAction {
                    root.removeView(overlay)
                    if (disclaimerPending) {
                        mostrarDisclaimer()
                        disclaimerPending = false
                    }
                }
                .start()
        }, OVERLAY_VISIBLE_MS)
    }

    private fun mostrarDisclaimer() {
        AlertDialog.Builder(this, R.style.AppAlertDialogTheme)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(getString(R.string.disclaimer_texto_limpio))
            .setPositiveButton(getString(R.string.btn_aceptar)) { _, _ ->
                sharedPreferences.edit().putBoolean("disclaimer_accepted", true).apply()
            }
            .setNegativeButton(getString(R.string.btn_salir)) { _, _ ->
                Toast.makeText(this, getString(R.string.msg_debe_aceptar_disclaimer), Toast.LENGTH_LONG).show()
                binding.root.postDelayed({ finish() }, DISCLAIMER_EXIT_DELAY_MS)
            }
            .setCancelable(false)
            .show()
    }
}