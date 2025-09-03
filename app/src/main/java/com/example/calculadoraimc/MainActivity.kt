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
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.edit
import android.graphics.Rect
import android.view.ViewTreeObserver

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
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar SplashScreen API antes de super
        val splashScreen = installSplashScreen()
        splashStartTime = System.currentTimeMillis()
        val isColdStart = (savedInstanceState == null)
        // Si es arranque en frío mantener el splash un mínimo; en recreaciones no mantenerlo.
        if (isColdStart) {
            splashScreen.setKeepOnScreenCondition {
                // Garantiza un mínimo para evitar parpadeo
                System.currentTimeMillis() - splashStartTime < SPLASH_DURATION_MS
            }
        } else {
            splashScreen.setKeepOnScreenCondition { false }
        }

        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "onCreate: isColdStart=$isColdStart, savedInstanceState=${savedInstanceState != null}")

        binding = ActivityMainBinding.inflate(layoutInflater)
        // Si la actividad se está recreando (por ejemplo una rotación), no ocultar el contenido.
        // Solo iniciar invisible en el arranque frío para poder crossfadear con el splash.
        binding.root.alpha = if (isColdStart) 0f else 1f
        setContentView(binding.root)

        // Configurar el listener para manejar el teclado
        setupKeyboardListener()

        // Configurar animación de salida (fade) del SplashScreen y mostrar disclaimer si procede
        if (savedInstanceState == null) {
            // Arranque en frío: hacer la animación del splash hacia el contenido
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                android.util.Log.d("MainActivity", "splash onExitAnimationListener (cold start) invoked")
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
                        android.util.Log.d("MainActivity", "splash fade animation end; removing splashView")
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
        } else {
            // Recreación (rotación): eliminar cualquier overlay del splash inmediatamente
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                android.util.Log.d("MainActivity", "splash onExitAnimationListener (recreation) invoked: removing immediately")
                splashScreenView.remove()
            }
        }

        // Fallback: asegurar que la raíz sea visible tras la recreación
        if (!isColdStart) {
            binding.root.post { binding.root.alpha = 1f }
        }

        android.util.Log.d("MainActivity", "after setContentView: root.visibility=${binding.root.visibility}, root.alpha=${binding.root.alpha}")

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

    private fun setupKeyboardListener() {
        // Listener para ajustar el layout cuando el teclado es mostrado/ocultado
        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val heightDiff = binding.root.height - (rect.bottom - rect.top)

            // Si la diferencia de altura es significativa, asumimos que el teclado está visible
            if (heightDiff > 200) {
                // El teclado está visible - no necesitamos hacer nada especial
                // adjustPan en el manifest se encarga del desplazamiento automático
                android.util.Log.d("MainActivity", "Teclado visible, heightDiff=$heightDiff")
            } else {
                // El teclado no está visible
                android.util.Log.d("MainActivity", "Teclado oculto, heightDiff=$heightDiff")
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remover el listener al destruir la actividad
        keyboardLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }
}