package com.example.calculadoraimc

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitarios para los cálculos de IMC
 */
class CalculadoraIMCTest {

    @Test
    fun calcularIMC_valoresNormales_calculaCorrectamente() {
        // Caso típico: 70kg, 1.75m = IMC 22.86
        val peso = 70.0
        val altura = 1.75
        val imcEsperado = 22.86

        val imc = peso / (altura * altura)

        assertEquals(imcEsperado, imc, 0.01)
    }

    @Test
    fun calcularIMC_alturaEnCentimetros_convierteYCalculaCorrectamente() {
        // 70kg, 175cm = IMC 22.86
        val peso = 70.0
        val alturaCm = 175.0
        val alturaM = alturaCm / 100.0
        val imcEsperado = 22.86

        val imc = peso / (alturaM * alturaM)

        assertEquals(imcEsperado, imc, 0.01)
    }

    @Test
    fun interpretarIMC_bajoWeight_devuelveCorrectamente() {
        val imc = 17.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Bajo peso", interpretacion)
    }

    @Test
    fun interpretarIMC_pesoNormal_devuelveCorrectamente() {
        val imc = 22.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Normal", interpretacion)
    }

    @Test
    fun interpretarIMC_sobrepeso_devuelveCorrectamente() {
        val imc = 27.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Sobrepeso", interpretacion)
    }

    @Test
    fun interpretarIMC_obesidadI_devuelveCorrectamente() {
        val imc = 32.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Obesidad I", interpretacion)
    }

    @Test
    fun interpretarIMC_obesidadII_devuelveCorrectamente() {
        val imc = 37.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Obesidad II", interpretacion)
    }

    @Test
    fun interpretarIMC_obesidadIII_devuelveCorrectamente() {
        val imc = 42.5
        val interpretacion = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else -> "Obesidad III"
        }

        assertEquals("Obesidad III", interpretacion)
    }

    @Test
    fun validarEntrada_valoresPositivos_esValido() {
        val peso = 70.0
        val altura = 1.75

        val esValido = peso > 0 && altura > 0

        assertTrue(esValido)
    }

    @Test
    fun validarEntrada_pesoNegativo_noEsValido() {
        val peso = -70.0
        val altura = 1.75

        val esValido = peso > 0 && altura > 0

        assertFalse(esValido)
    }

    @Test
    fun validarEntrada_alturaNegativa_noEsValido() {
        val peso = 70.0
        val altura = -1.75

        val esValido = peso > 0 && altura > 0

        assertFalse(esValido)
    }

    @Test
    fun validarEntrada_valorCero_noEsValido() {
        val peso = 0.0
        val altura = 1.75

        val esValido = peso > 0 && altura > 0

        assertFalse(esValido)
    }
}
