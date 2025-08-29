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
import android.animation.ObjectAnimator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    companion object {
        // Aumentado el mínimo para que el splash sea visible más tiempo
        private const val SPLASH_DURATION_MS = 1400L
        // Aumentado para un fade más suave
        private const val OVERLAY_FADE_MS = 600L
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
            // Garantiza un mínimo para evitar parpadeo
            System.currentTimeMillis() - splashStartTime < SPLASH_DURATION_MS
        }


        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        // Empezamos con el contenido invisible para poder crossfadear con el splash
        binding.root.alpha = 0f
        setContentView(binding.root)

        // Configurar animación de salida (fade) del SplashScreen y mostrar disclaimer si procede
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val splashView = splashScreenView.view

            // Preparar animaciones
            val fade = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f)
            fade.duration = OVERLAY_FADE_MS
            fade.interpolator = AccelerateDecelerateInterpolator()

            val contentFade = ObjectAnimator.ofFloat(binding.root, View.ALPHA, 0f, 1f)
            contentFade.duration = OVERLAY_FADE_MS
            // Pequeño retardo para que el contenido entre ligeramente después y el cambio sea menos brusco
            contentFade.startDelay = 120L
            contentFade.interpolator = AccelerateDecelerateInterpolator()

            fade.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Remover la vista del splash y continuar
                    splashScreenView.remove()
                    if (disclaimerPending) {
                        mostrarDisclaimer()
                    }
                }
            })

            // Asegurar que el splash se muestre al menos SPLASH_DURATION_MS: si el sistema pide salir antes,
            // retrasamos el inicio de la animación la cantidad restante.
            val elapsed = System.currentTimeMillis() - splashStartTime
            val remaining = SPLASH_DURATION_MS - elapsed
            if (remaining > 0) {
                binding.root.postDelayed({
                    contentFade.start()
                    fade.start()
                }, remaining)
            } else {
                contentFade.start()
                fade.start()
            }
        }

        // Python ya está inicializado en CalculadoraIMCApplication

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val disclaimerAccepted = sharedPreferences.getBoolean("disclaimer_accepted", false)
        disclaimerPending = !disclaimerAccepted

    }

    private fun mostrarDisclaimer() {
        AlertDialog.Builder(this, R.style.AppAlertDialogTheme)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(getString(R.string.disclaimer_texto))
            .setPositiveButton(getString(R.string.btn_aceptar)) { _, _ ->
                sharedPreferences.edit { putBoolean("disclaimer_accepted", true) }
            }
            .setNegativeButton(getString(R.string.btn_salir)) { _, _ ->
                Toast.makeText(this, getString(R.string.msg_debe_aceptar_disclaimer), Toast.LENGTH_LONG).show()
                binding.root.postDelayed({ finish() }, DISCLAIMER_EXIT_DELAY_MS)
            }
            .setCancelable(false)
            .show()
    }
}