package com.example.calculadoraimc

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class ChaquopyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Forzar el modo claro en toda la aplicación
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Inicializar Python tan pronto como sea posible
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}