package com.example.calculadoraimc.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.example.calculadoraimc.utils.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Clase para gestionar los anuncios intersticiales en la aplicación
 * Optimizada para cumplir con las políticas de Google AdMob
 */
class AdManager private constructor(context: Context) {

    // ID del anuncio intersticial de prueba de AdMob
    private val interstitialAdId = "ca-app-pub-3940256099942544/1033173712"

    // Referencia débil al contexto para evitar fugas de memoria
    private val contextRef = WeakReference(context.applicationContext)

    // Anuncio intersticial
    private var interstitialAd: InterstitialAd? = null

    // Para manejar solicitudes pendientes cuando el anuncio no está listo
    private var pendingAdRequest: PendingAdRequest? = null

    // Control de frecuencia de anuncios - SharedPreferences
    private val prefs: SharedPreferences = context.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)

    // Variables para controlar carga agresiva y rate limiting
    private var lastLoadTime = 0L
    private var loadAttempts = 0
    private var isLoading = false

    // Claves para SharedPreferences - SOLO para intersticiales
    private val KEY_LAST_INTERSTITIAL_TIME = "last_interstitial_shown_time"
    private val KEY_INTERSTITIAL_SESSION_COUNT = "intersticials_shown_in_session"
    private val KEY_FIRST_SESSION_FLAG = "first_session_flag"
    private val KEY_NAVIGATION_COUNT = "navigation_count"
    private val KEY_LAST_LOAD_TIME = "last_load_time"
    private val KEY_LOAD_ATTEMPTS = "load_attempts_today"

    // Límites de frecuencia para diferentes tipos de anuncios intersticiales
    private val MIN_TIME_BETWEEN_SAVE_ADS = TimeUnit.MINUTES.toMillis(2) // 2 minutos entre anuncios de guardar
    private val MIN_TIME_BETWEEN_NAV_ADS = TimeUnit.MINUTES.toMillis(5) // 5 minutos entre anuncios de navegación
    private val MAX_INTERSTITIALS_PER_SESSION = 3 // Máximo 3 intersticiales por sesión

    // Rate limiting para cumplir políticas de Google
    private val MIN_TIME_BETWEEN_LOADS = TimeUnit.SECONDS.toMillis(30) // 30 segundos entre cargas
    private val MAX_LOAD_ATTEMPTS_PER_DAY = 50 // Máximo 50 intentos de carga por día
    private val PENDING_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(30) // 30 segundos timeout para solicitudes pendientes

    // Probabilidad para mostrar anuncios de navegación (25%)
    private val NAV_AD_PROBABILITY = 0.25

    companion object {
        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Reiniciar contador de sesión si es la primera ejecución
        if (!prefs.contains(KEY_FIRST_SESSION_FLAG)) {
            resetSessionCounter()
            prefs.edit().putBoolean(KEY_FIRST_SESSION_FLAG, false).apply()
        }

        // Restaurar variables de rate limiting
        lastLoadTime = prefs.getLong(KEY_LAST_LOAD_TIME, 0)
        loadAttempts = prefs.getInt(KEY_LOAD_ATTEMPTS, 0)

        // Resetear intentos de carga si es un nuevo día
        resetLoadAttemptsIfNewDay()

        // Eliminado: Cargar el primer anuncio intersticial al inicializar (con rate limiting)
        // Esto ahora se hará explícitamente desde MainActivity después de la inicialización de MobileAds
    }

    /**
     * Inicia la carga de anuncios intersticiales.
     * Debe llamarse después de que MobileAds.initialize() haya completado.
     */
    fun startAdLoading() {
        Logger.d("AdManager", "Iniciando carga de anuncios a través de startAdLoading()")
        loadInterstitialAdWithRateLimit()
    }

    /**
     * Resetea los intentos de carga si es un nuevo día
     */
    private fun resetLoadAttemptsIfNewDay() {
        val currentTime = System.currentTimeMillis()
        val lastLoadDate = java.util.Date(lastLoadTime)
        val currentDate = java.util.Date(currentTime)

        val daysDiff = TimeUnit.DAYS.convert(
            currentTime - lastLoadTime,
            TimeUnit.MILLISECONDS
        )

        if (daysDiff >= 1) {
            loadAttempts = 0
            prefs.edit().putInt(KEY_LOAD_ATTEMPTS, 0).apply()
            Logger.d("AdManager", "Intentos de carga reseteados para nuevo día")
        }
    }

    /**
     * Carga un anuncio intersticial con rate limiting para cumplir políticas
     */
    private fun loadInterstitialAdWithRateLimit() {
        val currentTime = System.currentTimeMillis()

        // Verificar si ya se está cargando
        if (isLoading) {
            Logger.d("AdManager", "Ya hay una carga en progreso, omitiendo")
            return
        }

        // Verificar rate limiting por tiempo
        if (currentTime - lastLoadTime < MIN_TIME_BETWEEN_LOADS) {
            val remainingTime = (MIN_TIME_BETWEEN_LOADS - (currentTime - lastLoadTime)) / 1000
            Logger.d("AdManager", "Rate limiting: faltan ${remainingTime}s para poder cargar otro anuncio")
            return
        }

        // Verificar límite de intentos por día
        if (loadAttempts >= MAX_LOAD_ATTEMPTS_PER_DAY) {
            Logger.d("AdManager", "Límite diario de cargas alcanzado ($MAX_LOAD_ATTEMPTS_PER_DAY)")
            return
        }

        // Actualizar variables de control
        lastLoadTime = currentTime
        loadAttempts++
        isLoading = true

        // Guardar en SharedPreferences
        prefs.edit()
            .putLong(KEY_LAST_LOAD_TIME, lastLoadTime)
            .putInt(KEY_LOAD_ATTEMPTS, loadAttempts)
            .apply()

        Logger.d("AdManager", "Iniciando carga de anuncio intersticial (intento $loadAttempts/$MAX_LOAD_ATTEMPTS_PER_DAY)")

        loadInterstitialAd()
    }

    /**
     * Carga un anuncio intersticial (método interno)
     */
    private fun loadInterstitialAd() {
        val context = contextRef.get()
        if (context == null) {
            Logger.e("AdManager", "Contexto no disponible para cargar anuncio")
            isLoading = false
            return
        }

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, interstitialAdId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Logger.d("AdManager", "Anuncio intersticial cargado correctamente")
                interstitialAd = ad
                isLoading = false

                // Configurar callback para eventos del anuncio
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Logger.d("AdManager", "Anuncio intersticial cerrado por el usuario")
                        interstitialAd = null

                        // Cargar el siguiente anuncio con delay para evitar carga agresiva
                        scheduleNextAdLoad()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Logger.e("AdManager", "Error al mostrar anuncio intersticial: ${error.message}")
                        interstitialAd = null
                        // NO incrementar contador aquí - el anuncio no se mostró

                        // Intentar cargar otro anuncio con rate limiting
                        scheduleNextAdLoad()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Logger.d("AdManager", "Anuncio intersticial mostrado exitosamente al usuario")
                        interstitialAd = null

                        // AQUÍ es donde realmente sabemos que el usuario vio el anuncio
                        // Actualizar contadores y timestamp en SharedPreferences
                        updateInterstitialShownStats()
                    }
                }

                // Verificar si hay una solicitud pendiente y mostrar el anuncio
                checkPendingAdRequest()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Logger.e("AdManager", "Error al cargar anuncio intersticial: ${error.message}")
                interstitialAd = null
                isLoading = false

                // Limpiar solicitud pendiente si falla la carga
                pendingAdRequest = null
            }
        })
    }

    /**
     * Programa la carga del siguiente anuncio con delay para evitar carga agresiva
     */
    private fun scheduleNextAdLoad() {
        // Usar un Handler para retrasar la carga del siguiente anuncio
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            loadInterstitialAdWithRateLimit()
        }, MIN_TIME_BETWEEN_LOADS)
    }

    /**
     * Verifica si hay una solicitud pendiente y la ejecuta
     */
    private fun checkPendingAdRequest() {
        pendingAdRequest?.let { request ->
            Logger.d("AdManager", "Ejecutando solicitud pendiente de anuncio intersticial")

            // Verificar si la Activity aún es válida
            val activity = request.activityRef.get()
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                Logger.d("AdManager", "Activity no válida, cancelando solicitud pendiente")
                pendingAdRequest = null
                return
            }

            // Verificar si aún es válida la solicitud (no ha pasado mucho tiempo)
            val currentTime = System.currentTimeMillis()
            if (currentTime - request.timestamp < PENDING_REQUEST_TIMEOUT) {
                val canShow = showInterstitialAd(activity, request.adType, request.forceShow)
                Logger.d("AdManager", "Solicitud pendiente ejecutada: $canShow")
            } else {
                Logger.d("AdManager", "Solicitud pendiente expirada, no se muestra")
            }

            pendingAdRequest = null
        }
    }

    /**
     * Actualiza estadísticas de visualización de anuncios INTERSTICIALES en SharedPreferences
     * SOLO se llama cuando el anuncio se muestra exitosamente al usuario
     */
    private fun updateInterstitialShownStats() {
        val currentTime = System.currentTimeMillis()
        val intersticialtsShownInSession = prefs.getInt(KEY_INTERSTITIAL_SESSION_COUNT, 0) + 1

        prefs.edit()
            .putLong(KEY_LAST_INTERSTITIAL_TIME, currentTime)
            .putInt(KEY_INTERSTITIAL_SESSION_COUNT, intersticialtsShownInSession)
            .apply()

        Logger.d("AdManager", "Estadísticas intersticiales actualizadas: último=$currentTime, contador=$intersticialtsShownInSession")
    }

    /**
     * Comprueba si se puede mostrar un anuncio intersticial según el tipo y límites
     */
    private fun canShowInterstitialAd(adType: AdType): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastAdTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME, 0)
        val intersticialtsShownInSession = prefs.getInt(KEY_INTERSTITIAL_SESSION_COUNT, 0)
        val timeElapsed = currentTime - lastAdTime

        // Verificar límite de intersticiales por sesión
        if (intersticialtsShownInSession >= MAX_INTERSTITIALS_PER_SESSION) {
            Logger.d("AdManager", "Límite de intersticiales por sesión alcanzado ($MAX_INTERSTITIALS_PER_SESSION)")
            return false
        }

        // Verificar límite de tiempo según tipo de anuncio
        val minTimeRequired = when (adType) {
            AdType.SAVE -> MIN_TIME_BETWEEN_SAVE_ADS
            AdType.NAVIGATION -> MIN_TIME_BETWEEN_NAV_ADS
        }

        if (timeElapsed < minTimeRequired) {
            Logger.d("AdManager", "No ha pasado suficiente tiempo desde el último intersticial: ${timeElapsed/1000}s < ${minTimeRequired/1000}s")
            return false
        }

        return true
    }

    /**
     * Intenta mostrar un anuncio intersticial si se cumplen las condiciones
     * @param activity La actividad donde se mostrará el anuncio
     * @param adType El tipo de anuncio (guardar o navegación)
     * @param forceShow Forzar la muestra del anuncio ignorando restricciones
     * @return true si se muestra el anuncio, false en caso contrario
     */
    private fun showInterstitialAd(activity: Activity, adType: AdType, forceShow: Boolean = false): Boolean {
        // Verificar si se puede mostrar el anuncio
        if (!forceShow && !canShowInterstitialAd(adType)) {
            return false
        }

        // Para anuncios de navegación, aplicar probabilidad
        if (adType == AdType.NAVIGATION && !forceShow) {
            val shouldShow = Math.random() < NAV_AD_PROBABILITY
            if (!shouldShow) {
                Logger.d("AdManager", "Anuncio intersticial de navegación omitido por probabilidad")
                return false
            }
        }

        // Verificar si el anuncio está listo
        if (interstitialAd != null) {
            Logger.d("AdManager", "Intentando mostrar anuncio intersticial (tipo: $adType)")
            interstitialAd?.show(activity)
            return true
        } else {
            Logger.d("AdManager", "Anuncio intersticial no disponible")

            // Solo crear solicitud pendiente si no hay límites violados
            if (loadAttempts < MAX_LOAD_ATTEMPTS_PER_DAY) {
                Logger.d("AdManager", "Creando solicitud pendiente")
                // Guardar solicitud pendiente con WeakReference para evitar memory leaks
                pendingAdRequest = PendingAdRequest(
                    WeakReference(activity),
                    adType,
                    forceShow,
                    System.currentTimeMillis()
                )
                loadInterstitialAdWithRateLimit()
            } else {
                Logger.d("AdManager", "Límite diario alcanzado, no se carga anuncio")
            }

            return false
        }
    }

    /**
     * Intenta mostrar un anuncio intersticial después de guardar un cálculo
     */
    fun showAdAfterSaving(activity: Activity) {
        Logger.d("AdManager", "=== LLAMADA A showAdAfterSaving ===")
        Logger.d("AdManager", "Activity: ${activity.javaClass.simpleName}")

        val currentTime = System.currentTimeMillis()
        val lastAdTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME, 0)
        val intersticialtsShownInSession = prefs.getInt(KEY_INTERSTITIAL_SESSION_COUNT, 0)
        val timeElapsed = currentTime - lastAdTime

        Logger.d("AdManager", "Estado actual:")
        Logger.d("AdManager", "- Intersticiales mostrados en sesión: $intersticialtsShownInSession/$MAX_INTERSTITIALS_PER_SESSION")
        Logger.d("AdManager", "- Tiempo desde último intersticial: ${timeElapsed/1000}s (mínimo requerido: ${MIN_TIME_BETWEEN_SAVE_ADS/1000}s)")
        Logger.d("AdManager", "- Anuncio cargado: ${interstitialAd != null}")

        val result = showInterstitialAd(activity, AdType.SAVE)
        Logger.d("AdManager", "Resultado showInterstitialAd: $result")
    }

    /**
     * Intenta mostrar un anuncio intersticial al cambiar entre pestañas
     * con una probabilidad reducida para no ser intrusivo
     */
    fun showAdOnTabChange(activity: Activity) {
        val navigationCount = prefs.getInt(KEY_NAVIGATION_COUNT, 0)

        // Incrementar contador de navegaciones
        prefs.edit()
            .putInt(KEY_NAVIGATION_COUNT, navigationCount + 1)
            .apply()

        // No mostrar anuncio en la primera navegación de la sesión
        if (navigationCount == 0) {
            Logger.d("AdManager", "Primera navegación de la sesión, no se muestra anuncio intersticial")
            return
        }

        showInterstitialAd(activity, AdType.NAVIGATION)
    }

    /**
     * Reinicia el contador de intersticiales por sesión
     * Llamar este método al iniciar una nueva sesión de usuario
     */
    fun resetSessionCounter() {
        prefs.edit()
            .putInt(KEY_INTERSTITIAL_SESSION_COUNT, 0)
            .putInt(KEY_NAVIGATION_COUNT, 0)
            .apply()

        Logger.d("AdManager", "Contadores de intersticiales y navegación reiniciados")
    }

    /**
     * Tipos de anuncios intersticiales para aplicar diferentes reglas
     */
    enum class AdType {
        SAVE,       // Anuncio después de guardar un cálculo
        NAVIGATION  // Anuncio al cambiar entre pestañas
    }

    /**
     * Clase interna para representar una solicitud pendiente de anuncio intersticial
     * Usando WeakReference para evitar memory leaks
     */
    private class PendingAdRequest(
        val activityRef: WeakReference<Activity>,  // ✅ WeakReference para evitar memory leaks
        val adType: AdType,
        val forceShow: Boolean,
        val timestamp: Long
    )
}
