package com.example.calculadoraimc

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.navigation.NavController
import com.google.firebase.crashlytics.FirebaseCrashlytics


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
    // Instancia de Firebase Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    // Instancia de Firebase Crashlytics
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Forzar modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

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

        // Inicializar Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Inicializar Crashlytics
        crashlytics = FirebaseCrashlytics.getInstance()

        // Habilitar la recopilación de Crashlytics en entorno de producción
        // En un entorno de desarrollo puedes deshabilitar con crashlytics.setCrashlyticsCollectionEnabled(false)
        crashlytics.setCrashlyticsCollectionEnabled(true)

        // Configurar información de usuario (opcional)
        try {
            val userId = sharedPreferences?.getString("user_id", null) ?: "usuario_anónimo"
            crashlytics.setUserId(userId)

            // Añadir logs para depuración de crashlytics
            crashlytics.log("MainActivity onCreate - Usuario: $userId")
        } catch (e: Exception) {
            android.util.Log.e("Crashlytics", "Error configurando Crashlytics", e)
        }

        // Configurar la versión de la app como parámetro por defecto en todos los eventos
        configurarParametrosAnalytics()

        // Registrar pantalla principal usando logEvent (método recomendado para reemplazar setCurrentScreen)
        logScreenView("MainActivity")

        // Registrar el idioma del dispositivo
        registrarIdiomaDispositivo()

        // Registrar evento de inicio de app
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
        bundle.putString("start_type", if (isColdStart) "cold_start" else "warm_start")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)

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
        navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Configurar listener para rastrear navegación entre pantallas
        // (Ahora tras inicializar navController, pero con postDelayed para asegurar que se aplique)
        binding.root.post {
            configurarRastreoNavegacion()
        }

        navView.setupWithNavController(navController)

        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val disclaimerAccepted = sharedPreferences.getBoolean("disclaimer_accepted", false)
        disclaimerPending = !disclaimerAccepted

        // Registrar si el disclaimer está pendiente
        if (disclaimerPending) {
            firebaseAnalytics.logEvent("disclaimer_pending", null)
        }
    }

    /**
     * Método de utilidad para registrar vistas de pantalla en Analytics
     * Reemplaza el obsoleto setCurrentScreen
     */
    private fun logScreenView(screenName: String, screenClass: String? = null) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        android.util.Log.d("Analytics", "Pantalla registrada: $screenName")
    }

    /**
     * Configura parámetros predeterminados para todos los eventos de Analytics,
     * incluyendo la versión de la app
     */
    private fun configurarParametrosAnalytics() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = pInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }

            val defaultParams = Bundle().apply {
                putString("app_version_name", versionName)
                putLong("app_version_code", versionCode)
            }

            // Establecer estos parámetros para todos los eventos futuros
            firebaseAnalytics.setDefaultEventParameters(defaultParams)

            // También establecer como propiedades de usuario para segmentación
            firebaseAnalytics.setUserProperty("app_version", versionName)

            android.util.Log.d("Analytics", "Versión registrada: $versionName ($versionCode)")
        } catch (e: Exception) {
            android.util.Log.e("Analytics", "Error al obtener información de versión", e)
        }
    }

    /**
     * Configura un listener de navegación para rastrear entre qué pantallas navega el usuario
     * Usando logEvent con SCREEN_VIEW (enfoque recomendado)
     */
    private fun configurarRastreoNavegacion() {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            // Obtener nombre de pantalla basado en el destino
            val screenName = when (destination.id) {
                R.id.navigation_adultos -> "Adultos"
                R.id.navigation_menores -> "Menores"
                R.id.navigation_historial -> "Historial"
                R.id.navigation_grafico_historial -> {
                    val tipoHistorial = arguments?.getString("tipo_historial") ?: "adultos"
                    "Grafico_$tipoHistorial"
                }
                R.id.navigation_opciones -> "Opciones"
                R.id.navigation_aviso_legal -> "AvisoLegal"
                R.id.navigation_acerca_de -> "AcercaDe"
                else -> "Desconocido"
            }

            // Usar el método recomendado para registrar pantallas: logEvent(SCREEN_VIEW)
            val screenClass = destination.label?.toString() ?: ""
            logScreenView(screenName, screenClass)

            // Evento personalizado adicional para asegurar que aparezca
            val screenBundle = Bundle().apply {
                putString("screen_name", screenName)
            }
            firebaseAnalytics.logEvent("pantalla_visitada", screenBundle)

            android.util.Log.d("Analytics", "Pantalla visitada: $screenName")
        }
    }

    /**
     * Registra el idioma del dispositivo en Firebase Analytics
     */
    private fun registrarIdiomaDispositivo() {
        val currentLocale = resources.configuration.locales.get(0)
        val idioma = currentLocale.language
        val pais = currentLocale.country
        val idiomaCompleto = "$idioma-$pais"

        val bundle = Bundle().apply {
            putString("idioma", idioma)
            putString("idioma_pais", idiomaCompleto)
        }

        // Registrar idioma como evento para poder analizarlo
        firebaseAnalytics.logEvent("app_idioma", bundle)

        // También establecer como propiedad de usuario para segmentación
        firebaseAnalytics.setUserProperty("user_language", idioma)

        android.util.Log.d("Analytics", "Idioma registrado: $idiomaCompleto")
    }

    private fun mostrarDisclaimer() {
        // Registrar evento de mostrar disclaimer
        firebaseAnalytics.logEvent("disclaimer_shown", null)

        AlertDialog.Builder(this, R.style.AppAlertDialogTheme)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(getString(R.string.disclaimer_texto))
            .setPositiveButton(getString(R.string.btn_aceptar)) { _, _ ->
                sharedPreferences.edit { putBoolean("disclaimer_accepted", true) }
                // Registrar evento de aceptar disclaimer
                firebaseAnalytics.logEvent("disclaimer_accepted", null)
            }
            // Eliminamos el botón de salir para que el usuario solo pueda aceptar
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
