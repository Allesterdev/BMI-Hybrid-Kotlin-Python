package com.example.calculadoraimc

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class CalculadoraIMCApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar Python tan pronto como sea posible
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
