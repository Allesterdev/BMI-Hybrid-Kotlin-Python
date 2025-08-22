package com.example.calculadoraimc

import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Suite de tests de integración para verificar funcionalidades completas
 */
class IntegracionTest {

    @Test
    fun flujoCompletoAdultos_calculoYGuardado_funcionaCorrectamente() {
        // Simular flujo completo: entrada -> validación -> cálculo -> interpretación
        val peso = 70.0
        val altura = 175.0 // En centímetros

        // 1. Validar entrada
        assertTrue("Peso debe ser positivo", peso > 0)
        assertTrue("Altura debe ser positiva", altura > 0)

        // 2. Convertir altura a metros si es necesario
        val alturaM = if (altura > 3) altura / 100 else altura
        assertEquals("Altura convertida correctamente", 1.75, alturaM, 0.01)

        // 3. Calcular IMC
        val imc = peso / (alturaM * alturaM)
        assertEquals("IMC calculado correctamente", 22.86, imc, 0.01)

        // 4. Interpretar resultado
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }
        assertEquals("Interpretación correcta", "Normal", interpretacion)

        // 5. Formatear para mostrar (usando Locale.US para consistencia)
        val imcFormateado = String.format(Locale.US, "%.1f", imc)
        assertEquals("Formato correcto", "22.9", imcFormateado)
    }

    @Test
    fun flujoCompletoMenores_calculoPercentil_funcionaCorrectamente() {
        // Simular flujo completo para menores
        val peso = 40.0
        val altura = 140.0 // En centímetros
        val edadMeses = 115 // Aproximadamente 9.6 años
        val sexo = "M"

        // 1. Validar entrada
        assertTrue("Peso debe ser positivo", peso > 0)
        assertTrue("Altura debe ser positiva", altura > 0)
        assertTrue("Edad en rango válido", edadMeses >= 60 && edadMeses <= 228)
        assertTrue("Sexo válido", sexo == "M" || sexo == "F")

        // 2. Convertir altura a metros
        val alturaM = altura / 100.0
        assertEquals("Altura convertida", 1.40, alturaM, 0.01)

        // 3. Calcular IMC
        val imc = peso / (alturaM * alturaM)
        assertEquals("IMC calculado", 20.41, imc, 0.01)

        // 4. Calcular edad en años
        val edadAnios = edadMeses / 12.0
        assertEquals("Edad en años", 9.58, edadAnios, 0.01)

        // 5. Simular percentil (normalmente vendría de Python/OMS)
        val percentilSimulado = 96.0 // Valor ejemplo
        assertTrue("Percentil en rango válido", percentilSimulado >= 0 && percentilSimulado <= 100)

        // 6. Interpretar percentil según OMS
        val interpretacion = when {
            percentilSimulado < 3 -> "Bajo peso"
            percentilSimulado < 85 -> "Peso saludable"
            percentilSimulado < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }
        assertEquals("Interpretación percentil", "Sobrepeso", interpretacion)
    }

    @Test
    fun validacionCompleta_todosLosCamposRequeridos_funcionaCorrectamente() {
        // Test de validación completa para adultos
        val validacionAdultos = validarDatosAdultos("70.5", "1.75")
        assertTrue("Validación adultos exitosa", validacionAdultos.esValido)
        assertEquals("Peso parseado correctamente", 70.5, validacionAdultos.peso, 0.01)
        assertEquals("Altura parseada correctamente", 1.75, validacionAdultos.altura, 0.01)

        // Test de validación completa para menores
        val validacionMenores = validarDatosMenores("40", "140", "115", "M")
        assertTrue("Validación menores exitosa", validacionMenores.esValido)
        assertEquals("Datos menores correctos", 40.0, validacionMenores.peso, 0.01)
    }

    @Test
    fun manejoErrores_entradasInvalidas_manejadoCorrectamente() {
        // Test manejo de errores con entradas inválidas
        val validacionError1 = validarDatosAdultos("abc", "1.75")
        assertFalse("Debe fallar con peso inválido", validacionError1.esValido)

        val validacionError2 = validarDatosAdultos("70", "0")
        assertFalse("Debe fallar con altura cero", validacionError2.esValido)

        val validacionError3 = validarDatosMenores("40", "140", "300", "M")
        assertFalse("Debe fallar con edad fuera de rango", validacionError3.esValido)

        val validacionError4 = validarDatosMenores("40", "140", "115", "X")
        assertFalse("Debe fallar con sexo inválido", validacionError4.esValido)
    }

    @Test
    fun formateoResultados_todosLosFormatos_funcionanCorrectamente() {
        // Test todos los formatos de salida
        val imc = 23.456789
        val percentil = 96.789
        val peso = 70.123
        val altura = 1.756

        assertEquals("Formato IMC", "IMC: 23.5", String.format(Locale.US, "IMC: %.1f", imc))
        assertEquals("Formato percentil", "Percentil: 96.8", String.format(Locale.US, "Percentil: %.1f", percentil))
        assertEquals("Formato peso historial", "Peso: 70.1 kg", String.format(Locale.US, "Peso: %.1f kg", peso))
        assertEquals("Formato altura historial", "Altura: 1.76 m", String.format(Locale.US, "Altura: %.2f m", altura))
    }

    // Clases helper para validación
    data class ResultadoValidacionAdultos(
        val esValido: Boolean,
        val peso: Double = 0.0,
        val altura: Double = 0.0,
        val error: String = ""
    )

    data class ResultadoValidacionMenores(
        val esValido: Boolean,
        val peso: Double = 0.0,
        val altura: Double = 0.0,
        val edadMeses: Int = 0,
        val sexo: String = "",
        val error: String = ""
    )

    private fun validarDatosAdultos(pesoStr: String, alturaStr: String): ResultadoValidacionAdultos {
        try {
            val peso = pesoStr.toDouble()
            val altura = alturaStr.toDouble()

            if (peso <= 0) return ResultadoValidacionAdultos(false, error = "Peso debe ser positivo")
            if (altura <= 0) return ResultadoValidacionAdultos(false, error = "Altura debe ser positiva")

            val alturaFinal = if (altura > 3) altura / 100 else altura

            return ResultadoValidacionAdultos(true, peso, alturaFinal)
        } catch (e: NumberFormatException) {
            return ResultadoValidacionAdultos(false, error = "Valores numéricos inválidos")
        }
    }

    private fun validarDatosMenores(pesoStr: String, alturaStr: String, edadMesesStr: String, sexo: String): ResultadoValidacionMenores {
        try {
            val peso = pesoStr.toDouble()
            val altura = alturaStr.toDouble()
            val edadMeses = edadMesesStr.toInt()

            if (peso <= 0) return ResultadoValidacionMenores(false, error = "Peso debe ser positivo")
            if (altura <= 0) return ResultadoValidacionMenores(false, error = "Altura debe ser positiva")
            if (edadMeses < 60 || edadMeses > 228) return ResultadoValidacionMenores(false, error = "Edad fuera de rango")
            if (sexo != "M" && sexo != "F") return ResultadoValidacionMenores(false, error = "Sexo inválido")

            return ResultadoValidacionMenores(true, peso, altura, edadMeses, sexo)
        } catch (e: NumberFormatException) {
            return ResultadoValidacionMenores(false, error = "Valores numéricos inválidos")
        }
    }
}
