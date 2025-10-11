package com.example.calculadoraimc

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.firebase.crashlytics.FirebaseCrashlytics

class ChaquopyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Crashlytics primero para capturar errores tempranos
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            crashlytics.log("ChaquopyApplication onCreate - Inicialización temprana")
        } catch (e: Exception) {
            android.util.Log.e("Crashlytics", "Error inicializando Crashlytics temprano", e)
        }

        // Forzar el modo claro en toda la aplicación
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar Python tan pronto como sea posible
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}