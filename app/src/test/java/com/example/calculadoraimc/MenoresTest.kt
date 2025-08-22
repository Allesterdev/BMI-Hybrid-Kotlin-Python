package com.example.calculadoraimc

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Tests unitarios para los cálculos en menores (5-19 años)
 */
class MenoresTest {

    @Test
    fun calcularEdadEnMeses_fechasValidas_calculaCorrectamente() {
        // Caso: nacido el 11/01/2016, fecha actual 21/08/2025
        // Aproximadamente 9 años y 7 meses = 115 meses
        val fechaNacimiento = LocalDate.of(2016, 1, 11)
        val fechaActual = LocalDate.of(2025, 8, 21)

        val mesesEntre = ChronoUnit.MONTHS.between(fechaNacimiento, fechaActual)

        // Debe ser aproximadamente 115 meses
        assertTrue("La edad debe estar entre 114 y 116 meses", mesesEntre in 114..116)
    }

    @Test
    fun calcularEdadEnAnios_fechasValidas_calculaCorrectamente() {
        val fechaNacimiento = LocalDate.of(2016, 1, 11)
        val fechaActual = LocalDate.of(2025, 8, 21)

        val aniosEntre = ChronoUnit.YEARS.between(fechaNacimiento, fechaActual)

        assertEquals(9, aniosEntre)
    }

    @Test
    fun validarRangoEdad_edadValida_esValido() {
        val edadMeses = 120 // 10 años

        val esValido = edadMeses >= 60 && edadMeses <= 228 // 5 a 19 años

        assertTrue(esValido)
    }

    @Test
    fun validarRangoEdad_menorDe5_noEsValido() {
        val edadMeses = 48 // 4 años

        val esValido = edadMeses >= 60 && edadMeses <= 228 // 5 a 19 años

        assertFalse(esValido)
    }

    @Test
    fun validarRangoEdad_mayorDe19_noEsValido() {
        val edadMeses = 240 // 20 años

        val esValido = edadMeses >= 60 && edadMeses <= 228 // 5 a 19 años

        assertFalse(esValido)
    }

    @Test
    fun validarDatosMenores_todosValidos_esValido() {
        val peso = 40.0
        val altura = 140.0
        val edadMeses = 115
        val sexo = "M"

        val esValido = peso > 0 &&
                      altura > 0 &&
                      edadMeses >= 60 &&
                      edadMeses <= 228 &&
                      (sexo == "M" || sexo == "F")

        assertTrue(esValido)
    }

    @Test
    fun validarDatosMenores_sexoInvalido_noEsValido() {
        val peso = 40.0
        val altura = 140.0
        val edadMeses = 115
        val sexo = "X"

        val esValido = peso > 0 &&
                      altura > 0 &&
                      edadMeses >= 60 &&
                      edadMeses <= 228 &&
                      (sexo == "M" || sexo == "F")

        assertFalse(esValido)
    }

    @Test
    fun calcularIMC_menores_calculaCorrectamente() {
        // Caso: 40kg, 140cm = IMC 20.41
        val peso = 40.0
        val alturaCm = 140.0
        val alturaM = alturaCm / 100.0
        val imcEsperado = 20.41

        val imc = peso / (alturaM * alturaM)

        assertEquals(imcEsperado, imc, 0.01)
    }

    @Test
    fun interpretarPercentil_bajoWeight_correcto() {
        val percentil = 2.0
        val interpretacion = when {
            percentil < 3 -> "Bajo peso"
            percentil < 85 -> "Peso saludable"
            percentil < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }

        assertEquals("Bajo peso", interpretacion)
    }

    @Test
    fun interpretarPercentil_pesoSaludable_correcto() {
        val percentil = 50.0
        val interpretacion = when {
            percentil < 3 -> "Bajo peso"
            percentil < 85 -> "Peso saludable"
            percentil < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }

        assertEquals("Peso saludable", interpretacion)
    }

    @Test
    fun interpretarPercentil_sobrepeso_correcto() {
        val percentil = 90.0
        val interpretacion = when {
            percentil < 3 -> "Bajo peso"
            percentil < 85 -> "Peso saludable"
            percentil < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }

        assertEquals("Sobrepeso", interpretacion)
    }

    @Test
    fun interpretarPercentil_obesidad_correcto() {
        val percentil = 98.0
        val interpretacion = when {
            percentil < 3 -> "Bajo peso"
            percentil < 85 -> "Peso saludable"
            percentil < 97 -> "Sobrepeso"
            else -> "Obesidad"
        }

        assertEquals("Obesidad", interpretacion)
    }
}
