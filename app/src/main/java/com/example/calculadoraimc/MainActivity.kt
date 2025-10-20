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
import android.graphics.Rect
import android.view.ViewTreeObserver
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.navigation.NavController
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.*
import com.example.calculadoraimc.ui.adultos.AdultosFragment
import com.example.calculadoraimc.ui.menores.MenoresFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.core.content.edit
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MS = 1400L
        private const val OVERLAY_FADE_MS = 600L
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var disclaimerPending = false
    private var keyboardLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var navController: NavController
    private lateinit var consentInformation: ConsentInformation

    private var isUiReady = false
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Habilitar edge-to-edge para cumplir con Android 15+
        enableEdgeToEdge()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isUiReady }

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Configurar manejo de insets del sistema para edge-to-edge
        setupEdgeToEdgeInsets()

        isUiReady = true

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        crashlytics = FirebaseCrashlytics.getInstance()
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        disclaimerPending = !sharedPreferences.getBoolean("disclaimer_accepted", false)

        try {
            val userId = sharedPreferences.getString("user_id", null) ?: "usuario_anónimo"
            crashlytics.setUserId(userId)
            crashlytics.log("MainActivity onCreate - Usuario: $userId")
        } catch (e: Exception) {
            android.util.Log.e("Crashlytics", "Error configurando Crashlytics", e)
        }

        configurarParametrosAnalytics()
        logScreenView("MainActivity")
        registrarIdiomaDispositivo()
        setupKeyboardListener()

        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ -> onConsentFlowComplete() } },
            { onConsentFlowComplete() }
        )
    }

    private fun onConsentFlowComplete() {
        if (disclaimerPending) {
            mostrarDisclaimer()
        } else {
            inicializarAdMobYContinuar()
        }
    }

    private fun inicializarAdMobYContinuar() {
        MobileAds.initialize(this) {
            setAppReady()
        }
    }

    private fun setAppReady() {
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        val navView: BottomNavigationView = binding.navView
        navView.setupWithNavController(navController)
        configurarRastreoNavegacion()
        com.example.calculadoraimc.utils.AdManager.getInstance(this).startAdLoading()

        isAppReady = true
    }

    private fun mostrarDisclaimer() {
        firebaseAnalytics.logEvent("disclaimer_shown", null)

        val dialog = AlertDialog.Builder(this, R.style.AppAlertDialogTheme)
            .setTitle(getString(R.string.disclaimer_titulo))
            .setMessage(getString(R.string.disclaimer_texto))
            .setPositiveButton(getString(R.string.btn_aceptar)) { _, _ ->
                sharedPreferences.edit { putBoolean("disclaimer_accepted", true) }
                firebaseAnalytics.logEvent("disclaimer_accepted", null)
                inicializarAdMobYContinuar()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun logScreenView(screenName: String, screenClass: String? = null) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

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
            firebaseAnalytics.setDefaultEventParameters(defaultParams)
            firebaseAnalytics.setUserProperty("app_version", versionName)
        } catch (e: Exception) {
            android.util.Log.e("Analytics", "Error al obtener información de versión", e)
        }
    }

    private fun configurarRastreoNavegacion() {
        var lastMainTabId = -1
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val screenName = when (destination.id) {
                R.id.navigation_adultos -> "Adultos"
                R.id.navigation_menores -> "Menores"
                R.id.navigation_historial -> "Historial"
                R.id.navigation_opciones -> "Opciones"
                else -> null
            }
            screenName?.let { logScreenView(it) }

            val mainTabIds = listOf(R.id.navigation_adultos, R.id.navigation_menores, R.id.navigation_historial, R.id.navigation_opciones)
            if (mainTabIds.contains(destination.id)) {
                if (lastMainTabId != -1 && lastMainTabId != destination.id) {
                    if (consentInformation.canRequestAds()) {
                        com.example.calculadoraimc.utils.AdManager.getInstance(this).showAdOnTabChange(this)
                    }
                }
                lastMainTabId = destination.id
            }

            binding.root.post {
                if (consentInformation.canRequestAds()) {
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
                    val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                    if (currentFragment is AdultosFragment) {
                        currentFragment.cargarAnuncios()
                    } else if (currentFragment is MenoresFragment) {
                        currentFragment.cargarAnuncios()
                    }
                }
            }
        }
    }

    private fun registrarIdiomaDispositivo() {
        val currentLocale = resources.configuration.locales.get(0)
        val idiomaCompleto = "${currentLocale.language}-${currentLocale.country}"
        firebaseAnalytics.setUserProperty("user_language", idiomaCompleto)
    }

    private fun setupKeyboardListener() {
        keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val heightDiff = binding.root.height - (rect.bottom - rect.top)
            if (heightDiff > 200) {
                // Teclado visible
            } else {
                // Teclado oculto
            }
        }
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Aplicar padding al contenedor principal para la barra de estado
            view.setPadding(0, systemBars.top, 0, 0)

            // El BottomNavigationView maneja su propio padding para la barra de navegación
            binding.navView.setPadding(
                binding.navView.paddingLeft,
                binding.navView.paddingTop,
                binding.navView.paddingRight,
                systemBars.bottom
            )

            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
    }
}
