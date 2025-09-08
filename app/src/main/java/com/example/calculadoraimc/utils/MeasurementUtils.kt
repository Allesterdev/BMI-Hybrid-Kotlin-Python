package com.example.calculadoraimc.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

/**
 * Utilidad para manejar sistemas de medición (métrico e imperial) según localización.
 */
object MeasurementUtils {

    // Constantes para las preferencias
    private const val PREFS_NAME = "measurement_prefs"
    private const val KEY_SYSTEM_TYPE = "measurement_system_type"

    // Tipos de sistemas de medición
    enum class MeasurementSystem {
        METRIC,     // Sistema métrico (cm, kg)
        IMPERIAL    // Sistema imperial (pies/pulgadas, libras)
    }

    /**
     * Determina el sistema de medición adecuado según el locale del dispositivo.
     *
     * Oficialmente solo Estados Unidos, Liberia y Myanmar usan el sistema imperial como estándar.
     * Sin embargo, varios países con influencia británica usan una mezcla de ambos sistemas
     * o tienen uso cotidiano del sistema imperial para altura y peso de personas.
     */
    fun getSystemByLocale(locale: Locale): MeasurementSystem {
        val country = locale.country
        return when (country) {
            // Países que oficialmente usan el sistema imperial como estándar
            "US", // Estados Unidos
            "LR", // Liberia
            "MM", // Myanmar (Birmania)

            // Países con fuerte uso del sistema imperial para peso y altura en la vida cotidiana
            "GB", // Reino Unido (mezcla ambos sistemas, pero usa piedras/libras para peso y pies/pulgadas para altura)
            "CA", // Canadá (oficialmente métrico pero uso común de imperial)
            "IN", // India (mezcla de sistemas)
            "PK", // Pakistán (mezcla de sistemas)
            "BD", // Bangladesh (mezcla de sistemas)
            "JM", // Jamaica
            "AE", // Emiratos Árabes Unidos (usan ambos sistemas)
            "ZA", // Sudáfrica (mezcla de sistemas)
            "KN", // San Cristóbal y Nieves
            "LC", // Santa Lucía
            "TT", // Trinidad y Tobago
            "BS", // Bahamas
            "BB", // Barbados
            "AG", // Antigua y Barbuda
            "GD", // Granada
            "GY", // Guyana
            "BZ"  // Belice
            -> MeasurementSystem.IMPERIAL

            // El resto del mundo usa sistema métrico
            else -> MeasurementSystem.METRIC
        }
    }

    /**
     * Obtiene el sistema de medición almacenado en preferencias o, si no existe,
     * lo determina automáticamente según la localización y lo guarda.
     */
    fun getPreferredSystem(context: Context): MeasurementSystem {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSystem = prefs.getString(KEY_SYSTEM_TYPE, null)

        // Si ya hay un sistema guardado en preferencias, usarlo
        savedSystem?.let {
            return MeasurementSystem.valueOf(it)
        }

        // Si no hay sistema guardado, determinarlo por locale y guardarlo
        val locale = Locale.getDefault()
        val system = getSystemByLocale(locale)
        savePreferredSystem(prefs, system)
        return system
    }

    /**
     * Guarda el sistema de medición preferido en las preferencias.
     */
    private fun savePreferredSystem(prefs: SharedPreferences, system: MeasurementSystem) {
        prefs.edit {
            putString(KEY_SYSTEM_TYPE, system.name)
            commit()
        }
    }

    /**
     * Cambia manualmente el sistema de medición preferido y lo guarda.
     */
    fun setPreferredSystem(context: Context, system: MeasurementSystem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        savePreferredSystem(prefs, system)
    }

    /**
     * Convierte altura de centímetros a pies y pulgadas
     * @return Par donde first = pies, second = pulgadas
     */
    fun cmToFeetInches(cm: Float): Pair<Int, Float> {
        val totalInches = cm / 2.54f
        val feet = (totalInches / 12).toInt()
        val inches = totalInches % 12
        return Pair(feet, inches)
    }

    /**
     * Convierte de pies y pulgadas a centímetros
     */
    fun feetInchesToCm(feet: Int, inches: Float): Float {
        val totalInches = feet * 12 + inches
        return totalInches * 2.54f
    }

    /**
     * Convierte directamente de pulgadas a centímetros
     */
    fun inchesToCm(inches: Float): Float {
        return inches * 2.54f
    }

    /**
     * Convierte peso de kilogramos a libras
     */
    fun kgToLbs(kg: Float): Float {
        return kg * 2.20462f
    }

    /**
     * Convierte peso de libras a kilogramos
     */
    fun lbsToKg(lbs: Float): Float {
        return lbs / 2.20462f
    }

    /**
     * Calcula el IMC independientemente del sistema de unidades
     * (IMC = peso(kg) / altura(m)²)
     */
    fun calculateBMI(height: Float, weight: Float, system: MeasurementSystem): Float {
        return when (system) {
            MeasurementSystem.METRIC -> {
                // Altura en cm, peso en kg
                val heightInMeters = height / 100f
                weight / (heightInMeters * heightInMeters)
            }
            MeasurementSystem.IMPERIAL -> {
                // Altura en pulgadas, peso en libras
                // Fórmula imperial: (peso en libras * 703) / (altura en pulgadas)²
                (weight * 703) / (height * height)
            }
        }
    }
}
