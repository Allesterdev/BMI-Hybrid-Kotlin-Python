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

    // === FUNCIONES DE CONVERSIÓN ===

    /**
     * Convierte kilogramos a libras
     */
    fun kgToLbs(kg: Double): Double = kg * 2.20462

    /**
     * Convierte libras a kilogramos
     */
    fun lbsToKg(lbs: Double): Double = lbs / 2.20462

    /**
     * Convierte centímetros a pies y pulgadas
     * @return Pair donde first = pies, second = pulgadas
     */
    fun cmToFeetInches(cm: Double): Pair<Int, Double> {
        val totalInches = cm / 2.54
        val feet = (totalInches / 12).toInt()
        val inches = totalInches % 12
        return Pair(feet, inches)
    }

    /**
     * Convierte pies y pulgadas a centímetros
     */
    fun feetInchesToCm(feet: Int, inches: Double): Double {
        val totalInches = feet * 12 + inches
        return totalInches * 2.54
    }

    /**
     * Convierte pulgadas a centímetros
     */
    fun inchesToCm(inches: Double): Double = inches * 2.54

    /**
     * Convierte centímetros a pulgadas
     */
    fun cmToInches(cm: Double): Double = cm / 2.54

    /**
     * Analiza una cadena con formato de pies y pulgadas (ej: "5'10", "5'10.5", "5ft10", "5ft 10.5in", etc.)
     * y la convierte a centímetros
     *
     * @param heightStr String con formato de pies y pulgadas
     * @return altura en centímetros
     * @throws NumberFormatException si el formato no es reconocido
     */
    fun parseFeetInchesToCm(heightStr: String): Double {
        // Eliminar espacios y convertir a minúsculas
        val cleanStr = heightStr.lowercase().trim()

        // Intentar diferentes patrones comunes
        val patterns = listOf(
            """(\d+)['´`](\d+\.?\d*)""",    // 5'10 o 5'10.5
            """(\d+)\s*['´`]\s*(\d+\.?\d*)""",    // 5' 10 o 5' 10.5
            """(\d+)\s*ft\s*(\d+\.?\d*)""",  // 5ft10 o 5ft 10.5
            """(\d+)\s*feet\s*(\d+\.?\d*)""",  // 5feet10 o 5feet 10.5
            """(\d+)[,.](\d+)""",           // 5,10 o 5.10 (como decimal)
            """(\d+)""",                     // Solo pies (ej: "5")
            """(\d+)\s*ft""",                // Solo pies con unidad (ej: "5ft")
            """(\d+)\s*feet"""               // Solo pies con unidad (ej: "5feet")
        )

        // Probar cada patrón
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val matchResult = regex.find(cleanStr)

            if (matchResult != null) {
                val groups = matchResult.groupValues
                when (groups.size) {
                    3 -> {
                        // Formato con pies y pulgadas
                        val feet = groups[1].toDouble()
                        val inches = groups[2].toDouble()
                        return feetInchesToCm(feet.toInt(), inches)
                    }
                    2 -> {
                        if (pattern.contains("ft") || pattern.contains("feet")) {
                            // Solo pies con unidad
                            return feetInchesToCm(groups[1].toInt(), 0.0)
                        } else if (pattern.contains("[,.]")) {
                            // Formato decimal (ej: 5.10 = 5 pies 10 pulgadas)
                            val feet = groups[1].toDouble().toInt()
                            val inchesStr = "0." + groups[2]
                            val inches = (inchesStr.toDouble() * 100.0) / 8.333 // Convertir décimas a pulgadas
                            return feetInchesToCm(feet, inches)
                        } else {
                            // Solo pies
                            return feetInchesToCm(groups[1].toInt(), 0.0)
                        }
                    }
                }
            }
        }

        // Si no se pudo parsear, intentar como simple número de pulgadas
        return try {
            val inches = cleanStr.toDouble()
            inchesToCm(inches)
        } catch (e: NumberFormatException) {
            throw NumberFormatException("No se pudo reconocer el formato de altura: $heightStr")
        }
    }

    /**
     * Formatea el peso según el sistema de medición preferido
     */
    fun formatWeight(weightKg: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format(Locale.US, "%.1f kg", weightKg)
            MeasurementSystem.IMPERIAL -> String.format(Locale.US, "%.1f lbs", kgToLbs(weightKg))
        }
    }

    /**
     * Formatea la altura según el sistema de medición preferido
     */
    fun formatHeight(heightCm: Double, system: MeasurementSystem): String {
        return when (system) {
            MeasurementSystem.METRIC -> String.format(Locale.US, "%.0f cm", heightCm)
            MeasurementSystem.IMPERIAL -> {
                val (feet, inches) = cmToFeetInches(heightCm)
                String.format(Locale.US, "%d'%.1f\"", feet, inches)
            }
        }
    }
}
