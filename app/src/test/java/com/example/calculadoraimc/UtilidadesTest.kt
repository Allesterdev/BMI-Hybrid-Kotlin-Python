package com.example.calculadoraimc

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tests unitarios para funciones de utilidad y validación
 */
class UtilidadesTest {

    @Test
    fun validarFormatoFecha_fechaValida_esValida() {
        val fechaTexto = "11/01/2016"
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            val fecha = formato.parse(fechaTexto)
            assertNotNull(fecha)
        } catch (e: Exception) {
            fail("La fecha debería ser válida")
        }
    }

    @Test
    fun validarFormatoFecha_fechaInvalida_noEsValida() {
        val fechaTexto = "32/13/2016" // Día y mes inválidos
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        formato.isLenient = false

        try {
            formato.parse(fechaTexto)
            fail("La fecha debería ser inválida")
        } catch (e: Exception) {
            // Esperado que lance excepción
            assertTrue(true)
        }
    }

    @Test
    fun formatearResultadoIMC_valorDecimal_formateaCorrectamente() {
        val imc = 23.456789
        val resultado = String.format(Locale.US, "%.1f", imc)

        assertEquals("23.5", resultado)
    }

    @Test
    fun formatearResultadoPercentil_valorDecimal_formateaCorrectamente() {
        val percentil = 96.789
        val resultado = String.format(Locale.US, "%.1f", percentil)

        assertEquals("96.8", resultado)
    }

    @Test
    fun convertirAlturaMetros_entradaEnMetros_mantieneMismoValor() {
        val altura = 1.75
        val alturaConvertida = if (altura > 3) altura / 100 else altura

        assertEquals(1.75, alturaConvertida, 0.001)
    }

    @Test
    fun convertirAlturaMetros_entradaEnCentimetros_convierteAMetros() {
        val altura = 175.0
        val alturaConvertida = if (altura > 3) altura / 100 else altura

        assertEquals(1.75, alturaConvertida, 0.001)
    }

    @Test
    fun validarEntradaNumerica_numeroValido_esValido() {
        val texto = "70.5"

        try {
            val numero = texto.toDouble()
            assertTrue(numero > 0)
        } catch (e: NumberFormatException) {
            fail("Debería ser un número válido")
        }
    }

    @Test
    fun validarEntradaNumerica_textoInvalido_noEsValido() {
        val texto = "abc"

        try {
            texto.toDouble()
            fail("Debería lanzar excepción")
        } catch (e: NumberFormatException) {
            // Esperado
            assertTrue(true)
        }
    }

    @Test
    fun validarCamposVacios_campoVacio_esInvalido() {
        val texto = ""

        assertFalse(texto.trim().isNotEmpty())
    }

    @Test
    fun validarCamposVacios_campoConEspacios_esInvalido() {
        val texto = "   "

        assertFalse(texto.trim().isNotEmpty())
    }

    @Test
    fun validarCamposVacios_campoConTexto_esValido() {
        val texto = "70.5"

        assertTrue(texto.trim().isNotEmpty())
    }

    @Test
    fun rangoIMCAdultos_valorLimite_clasificaCorrectamente() {
        // Test valores límite
        assertEquals("Bajo peso", clasificarIMC(18.4))
        assertEquals("Normal", clasificarIMC(18.5))
        assertEquals("Normal", clasificarIMC(24.9))
        assertEquals("Sobrepeso", clasificarIMC(25.0))
        assertEquals("Sobrepeso", clasificarIMC(29.9))
        assertEquals("Obesidad I", clasificarIMC(30.0))
        assertEquals("Obesidad I", clasificarIMC(34.9))
        assertEquals("Obesidad II", clasificarIMC(35.0))
        assertEquals("Obesidad II", clasificarIMC(39.9))
        assertEquals("Obesidad III", clasificarIMC(40.0))
    }

    private fun clasificarIMC(imc: Double): String {
        return when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }
    }

    @Test
    fun rangoPercentiles_valorLimite_clasificaCorrectamente() {
        // Test valores límite según OMS
        assertEquals("Bajo peso", clasificarPercentil(2.9))
        assertEquals("Peso saludable", clasificarPercentil(3.0))
        assertEquals("Peso saludable", clasificarPercentil(84.9))
        assertEquals("Sobrepeso", clasificarPercentil(85.0))
        assertEquals("Sobrepeso", clasificarPercentil(96.9))
        assertEquals("Obesidad", clasificarPercentil(97.0))
    }

    private fun clasificarPercentil(percentil: Double): String {
        return when {
            percentil < 3 -> "Bajo peso"
            percentil < 85 -> "Peso saludable"
            percentil < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }
    }
}
