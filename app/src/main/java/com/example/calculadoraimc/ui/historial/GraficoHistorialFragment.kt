package com.example.calculadoraimc.ui.historial

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.chaquo.python.Python
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentGraficoHistorialBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class GraficoHistorialFragment : Fragment() {

    private var _binding: FragmentGraficoHistorialBinding? = null
    private val binding get() = _binding!!

    private enum class Modo { ADULTOS, MENORES }
    private var modoActual = Modo.ADULTOS

    // Lista de fechas asociadas al eje X (usaremos índice como X para simplicidad)
    private var fechasActuales: List<Date> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraficoHistorialBinding.inflate(inflater, container, false)
        setupChart()
        setupToggle()
        setupBotonVolver()
        cargarDatos()
        return binding.root
    }

    private fun setupBotonVolver() {
        binding.btnVolverHistorial.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupToggle() {
        // Estado inicial adultos
        binding.slidingPillChart.translationX = 0f
        actualizarColoresToggle(adultosSeleccionado = true)

        binding.btnChartAdultos.setOnClickListener {
            if (modoActual != Modo.ADULTOS) {
                modoActual = Modo.ADULTOS
                animarPill(aIzquierda = true)
                actualizarColoresToggle(true)
                cargarDatos()
            }
        }
        binding.btnChartMenores.setOnClickListener {
            if (modoActual != Modo.MENORES) {
                modoActual = Modo.MENORES
                animarPill(aIzquierda = false)
                actualizarColoresToggle(false)
                cargarDatos()
            }
        }
    }

    private fun animarPill(aIzquierda: Boolean) {
        val destino = if (aIzquierda) 0f else binding.btnChartAdultos.width.toFloat()
        binding.slidingPillChart.animate()
            .translationX(destino)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun actualizarColoresToggle(adultosSeleccionado: Boolean) {
        if (adultosSeleccionado) {
            binding.btnChartAdultos.setTextColor(Color.parseColor("#2E7D32"))
            binding.btnChartMenores.setTextColor(Color.WHITE)
        } else {
            binding.btnChartAdultos.setTextColor(Color.WHITE)
            binding.btnChartMenores.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    private fun setupChart() {
        val chart = binding.lineChart
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = Color.DKGRAY
        chart.axisLeft.setDrawGridLines(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textColor = Color.DKGRAY
        chart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            textColor = Color.DKGRAY
        }
    }

    private fun cargarDatos() {
        val python = Python.getInstance()
        val funciones = python.getModule("funciones_imc_android")

        // Obtener historial completo para construir múltiples datasets
        val lista = when (modoActual) {
            Modo.ADULTOS -> funciones.callAttr("obtener_historial_adultos").asList()
            Modo.MENORES -> funciones.callAttr("obtener_historial_menores").asList()
        }

        if (lista.isEmpty()) {
            binding.tvSinDatos.visibility = View.VISIBLE
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        } else {
            binding.tvSinDatos.visibility = View.GONE
        }

        // Parsear y ordenar por fecha ascendente
        val formatoPython = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        data class Registro(val fecha: Date, val peso: Double?, val altura: Double?, val imc: Double?, val percentil: Double?)

        val registros = lista.mapNotNull { item ->
            val map = item.asMap()
            val fechaStr = map[com.chaquo.python.PyObject.fromJava("fecha")]?.toString()
            try {
                val fecha = fechaStr?.let { formatoPython.parse(it) } ?: return@mapNotNull null
                val peso = map[com.chaquo.python.PyObject.fromJava("peso")]?.toString()?.toDoubleOrNull()
                val altura = map[com.chaquo.python.PyObject.fromJava("altura")]?.toString()?.toDoubleOrNull()
                val imc = map[com.chaquo.python.PyObject.fromJava("imc")]?.toString()?.toDoubleOrNull()
                val percentil = map[com.chaquo.python.PyObject.fromJava("percentil")]?.toString()?.toDoubleOrNull()
                Registro(fecha, peso, altura, imc, percentil)
            } catch (_: ParseException) {
                null
            }
        }.sortedBy { it.fecha }

        if (registros.isEmpty()) {
            binding.tvSinDatos.visibility = View.VISIBLE
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

        fechasActuales = registros.map { it.fecha }

        val lineDataSets = mutableListOf<LineDataSet>()
        var index = 0

        fun nuevaDataSet(label: String, color: Int, entries: List<Entry>): LineDataSet {
            return LineDataSet(entries, label).apply {
                this.color = color
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 4f
                setCircleColor(color)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = color
                fillAlpha = 60
                highLightColor = Color.BLACK
            }
        }

        when (modoActual) {
            Modo.ADULTOS -> {
                val entradasIMC = mutableListOf<Entry>()
                val entradasPeso = mutableListOf<Entry>()
                registros.forEach { r ->
                    entradasIMC.add(Entry(index.toFloat(), (r.imc ?: 0.0).toFloat()))
                    entradasPeso.add(Entry(index.toFloat(), (r.peso ?: 0.0).toFloat()))
                    index++
                }
                lineDataSets.add(nuevaDataSet(getString(R.string.historial_imc).removePrefix("IMC: "), requireContext().getColor(R.color.line_imc), entradasIMC))
                lineDataSets.add(nuevaDataSet(getString(R.string.historial_peso).substringBefore("%"), requireContext().getColor(R.color.line_peso), entradasPeso))
            }
            Modo.MENORES -> {
                val entradasPercentil = mutableListOf<Entry>()
                val entradasPeso = mutableListOf<Entry>()
                val entradasAltura = mutableListOf<Entry>()
                registros.forEach { r ->
                    entradasPercentil.add(Entry(index.toFloat(), (r.percentil ?: 0.0).toFloat()))
                    entradasPeso.add(Entry(index.toFloat(), (r.peso ?: 0.0).toFloat()))
                    entradasAltura.add(Entry(index.toFloat(), (r.altura ?: 0.0).toFloat()))
                    index++
                }
                lineDataSets.add(nuevaDataSet("Percentil", requireContext().getColor(R.color.line_percentil), entradasPercentil))
                lineDataSets.add(nuevaDataSet("Peso", requireContext().getColor(R.color.line_peso), entradasPeso))
                lineDataSets.add(nuevaDataSet("Altura", requireContext().getColor(R.color.line_altura), entradasAltura))
            }
        }

        val lineData = LineData(lineDataSets as List<LineDataSet>)
        binding.lineChart.data = lineData

        // Formateador de fechas adaptable a la configuración regional
        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        binding.lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val i = value.toInt()
                return if (i in fechasActuales.indices) dateFormat.format(fechasActuales[i]) else ""
            }
        }
        binding.lineChart.xAxis.labelRotationAngle = -35f
        binding.lineChart.animateX(600)
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

