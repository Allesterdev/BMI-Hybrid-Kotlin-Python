package com.example.calculadoraimc.utils

import android.util.Log

/**
 * Clase de utilidad para logs condicionales
 * Los logs solo aparecen en builds de DEBUG
 */
object Logger {

    private const val DEFAULT_TAG = "CalculadoraIMC"

    // Detectar automáticamente si es build de debug
    private val isDebugBuild = try {
        Class.forName("com.example.calculadoraimc.BuildConfig").getDeclaredField("DEBUG").getBoolean(null)
    } catch (e: Exception) {
        // Si no se puede acceder a BuildConfig, asumir que es debug para desarrollo
        true
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugBuild) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugBuild) {
            Log.i(tag, message)
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugBuild) {
            Log.w(tag, message)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String) {
        // Los logs de ERROR siempre se muestran para poder monitorear problemas
        Log.e(tag, message)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        // Los logs de ERROR siempre se muestran para poder monitorear problemas
        Log.e(tag, message, throwable)
    }
}
