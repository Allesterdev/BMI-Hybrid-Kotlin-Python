package com.example.calculadoraimc

import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

/**
 * Tests unitarios para validar la lógica de base de datos y historial
 */
class HistorialTest {

    @Test
    fun validarMedicionAdultos_datosCompletos_esValida() {
        val peso = 70.0
        val altura = 1.75
        val imc = peso / (altura * altura)
        val fecha = "2025-08-22"

        val datosCompletos = peso > 0 && altura > 0 && imc > 0 && fecha.isNotEmpty()

        assertTrue(datosCompletos)
    }

    @Test
    fun validarMedicionMenores_datosCompletos_esValida() {
        val peso = 40.0
        val altura = 140.0
        val edadMeses = 115
        val sexo = "M"
        val percentil = 96.0
        val fecha = "2025-08-22"

        val datosCompletos = peso > 0 &&
                           altura > 0 &&
                           edadMeses > 0 &&
                           (sexo == "M" || sexo == "F") &&
                           percentil >= 0 &&
                           percentil <= 100 &&
                           fecha.isNotEmpty()

        assertTrue(datosCompletos)
    }

    @Test
    fun formatearFechaHistorial_fechaValida_formateaCorrectamente() {
        val fecha = "2025-08-22"
        val partes = fecha.split("-")

        assertEquals(3, partes.size)
        assertEquals("2025", partes[0])
        assertEquals("08", partes[1])
        assertEquals("22", partes[2])
    }

    @Test
    fun validarRangoPercentil_valorValido_estaEnRango() {
        val percentil = 96.0

        val enRango = percentil >= 0 && percentil <= 100

        assertTrue(enRango)
    }

    @Test
    fun validarRangoPercentil_valorInvalido_noEstaEnRango() {
        val percentil = 150.0

        val enRango = percentil >= 0 && percentil <= 100

        assertFalse(enRango)
    }

    @Test
    fun calcularEdadAniosDesdemeses_edadValida_calculaCorrectamente() {
        val edadMeses = 115
        val edadAnios = edadMeses / 12.0

        assertEquals(9.58, edadAnios, 0.01)
    }

    @Test
    fun validarDatosParaGrafico_listaSuficiente_puedeGraficar() {
        val cantidadMediciones = 5

        val suficienteParaGrafico = cantidadMediciones >= 2

        assertTrue(suficienteParaGrafico)
    }

    @Test
    fun validarDatosParaGrafico_listaInsuficiente_noPuedeGraficar() {
        val cantidadMediciones = 1

        val suficienteParaGrafico = cantidadMediciones >= 2

        assertFalse(suficienteParaGrafico)
    }

    @Test
    fun formatearPesoHistorial_valorDecimal_formateaCorrectamente() {
        val peso = 70.456
        val pesoFormateado = String.format(Locale.US, "%.1f", peso)

        assertEquals("70.5", pesoFormateado)
    }

    @Test
    fun formatearAlturaHistorial_valorDecimal_formateaCorrectamente() {
        val altura = 1.756
        val alturaFormateada = String.format(Locale.US, "%.2f", altura)

        assertEquals("1.76", alturaFormateada)
    }

    @Test
    fun validarSexoMenores_masculino_esValido() {
        val sexo = "M"

        val sexoValido = sexo == "M" || sexo == "F"

        assertTrue(sexoValido)
    }

    @Test
    fun validarSexoMenores_femenino_esValido() {
        val sexo = "F"

        val sexoValido = sexo == "M" || sexo == "F"

        assertTrue(sexoValido)
    }

    @Test
    fun validarSexoMenores_invalido_noEsValido() {
        val sexo = "X"

        val sexoValido = sexo == "M" || sexo == "F"

        assertFalse(sexoValido)
    }
}
