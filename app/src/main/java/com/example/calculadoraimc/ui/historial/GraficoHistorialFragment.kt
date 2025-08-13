package com.example.calculadoraimc.ui.historial

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.chaquo.python.Python
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentGraficoHistorialBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

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
                // Limpiar highlight para evitar crash del marker previo
                binding.lineChart.highlightValues(null)
                modoActual = Modo.ADULTOS
                animarPill(aIzquierda = true)
                actualizarColoresToggle(true)
                cargarDatos()
            }
        }
        binding.btnChartMenores.setOnClickListener {
            if (modoActual != Modo.MENORES) {
                binding.lineChart.highlightValues(null)
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
        chart.axisLeft.axisMinimum = 0f // Evitar valores negativos
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textColor = Color.DKGRAY
        chart.setNoDataText(getString(R.string.sin_datos_grafico))
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
        val lista = when (modoActual) {
            Modo.ADULTOS -> funciones.callAttr("obtener_historial_adultos").asList()
            Modo.MENORES -> funciones.callAttr("obtener_historial_menores").asList()
        }
        if (lista.isEmpty()) {
            binding.lineChart.clear()
            binding.lineChart.invalidate()
            return
        }

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
            } catch (_: ParseException) { null }
        }.sortedBy { it.fecha }

        if (registros.isEmpty()) {
            binding.lineChart.clear(); binding.lineChart.invalidate(); return
        }

        // Downsampling simple
        val MAX_POINTS = 150
        val registrosUsados: List<Registro> = when {
            registros.size <= MAX_POINTS -> registros
            registros.size <= 500 -> {
                val step = ceil(registros.size / MAX_POINTS.toDouble()).toInt().coerceAtLeast(1)
                registros.filterIndexed { index, _ -> index % step == 0 }
            }
            else -> registros.takeLast(MAX_POINTS) // Últimos más recientes
        }

        fechasActuales = registrosUsados.map { it.fecha }

        val lineDataSets = mutableListOf<ILineDataSet>()
        var index = 0

        fun shouldAdd(entries: List<Entry>) = entries.any { it.y != 0f }
        fun nuevaDataSet(label: String, color: Int, entries: List<Entry>, fill: Boolean): LineDataSet = LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2.5f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(color)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(fill)
            if (fill) { fillColor = color; fillAlpha = 60 }
            highLightColor = Color.BLACK
        }

        when (modoActual) {
            Modo.ADULTOS -> {
                val entradasIMC = mutableListOf<Entry>()
                val entradasPeso = mutableListOf<Entry>()
                registrosUsados.forEach { r ->
                    entradasIMC.add(Entry(index.toFloat(), (r.imc ?: 0.0).toFloat()))
                    entradasPeso.add(Entry(index.toFloat(), (r.peso ?: 0.0).toFloat()))
                    index++
                }
                if (shouldAdd(entradasIMC)) lineDataSets.add(nuevaDataSet(getString(R.string.label_imc), requireContext().getColor(R.color.line_imc), entradasIMC, true))
                if (shouldAdd(entradasPeso)) lineDataSets.add(nuevaDataSet(getString(R.string.label_peso), requireContext().getColor(R.color.line_peso), entradasPeso, false))
            }
            Modo.MENORES -> {
                val entradasPercentil = mutableListOf<Entry>()
                val entradasPeso = mutableListOf<Entry>()
                val entradasAltura = mutableListOf<Entry>()
                registrosUsados.forEach { r ->
                    entradasPercentil.add(Entry(index.toFloat(), (r.percentil ?: 0.0).toFloat()))
                    entradasPeso.add(Entry(index.toFloat(), (r.peso ?: 0.0).toFloat()))
                    entradasAltura.add(Entry(index.toFloat(), (r.altura ?: 0.0).toFloat()))
                    index++
                }
                if (shouldAdd(entradasPercentil)) lineDataSets.add(nuevaDataSet(getString(R.string.label_percentil), requireContext().getColor(R.color.line_percentil), entradasPercentil, true))
                if (shouldAdd(entradasPeso)) lineDataSets.add(nuevaDataSet(getString(R.string.label_peso), requireContext().getColor(R.color.line_peso), entradasPeso, false))
                if (shouldAdd(entradasAltura)) lineDataSets.add(nuevaDataSet(getString(R.string.label_altura), requireContext().getColor(R.color.line_altura), entradasAltura, false))
            }
        }

        val lineData = LineData(lineDataSets)
        binding.lineChart.data = lineData
        binding.lineChart.highlightValues(null) // Reset highlight tras recarga

        // Marker táctil personalizado (re-creado tras cada carga de datos)
        binding.lineChart.marker = HistorialMarker(requireContext()) { fechasActuales }

        // Formateador de fechas adaptable a la configuración regional
        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
        binding.lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val i = value.toInt()
                return if (i in fechasActuales.indices) dateFormat.format(fechasActuales[i]) else ""
            }
        }
        binding.lineChart.xAxis.setLabelCount(minOf(fechasActuales.size, 6), true)
        binding.lineChart.xAxis.labelRotationAngle = -35f
        binding.lineChart.animateX(600)
        binding.lineChart.animateY(600)
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Clase interna para el MarkerView personalizado
    private class HistorialMarker(
        context: Context,
        private val fechasProvider: () -> List<Date>
    ) : MarkerView(context, R.layout.marker_chart) {
        private val tvFecha: TextView = findViewById(R.id.tvMarkerFecha)
        private val tvValores: TextView = findViewById(R.id.tvMarkerValores)
        private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        private val oneDecimal = DecimalFormat("#.0")

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                if (e == null) return
                val fechas = fechasProvider()
                val idxRedondeado = e.x.roundToInt()
                val fechaTxt = fechas.getOrNull(idxRedondeado)?.let { dateTimeFormat.format(it) } ?: ""
                val data = chartView?.data
                val builder = StringBuilder()
                if (data != null) {
                    for (i in 0 until data.dataSetCount) {
                        val ds = data.getDataSetByIndex(i)
                        // Intentar obtener por X exacto primero
                        var entry: Entry? = ds.getEntryForXValue(e.x, Float.NaN)
                        if (entry == null && idxRedondeado in 0 until ds.entryCount) {
                            // Fallback por índice
                            entry = ds.getEntryForIndex(idxRedondeado)
                        }
                        if (entry == null) {
                            // Fallback adicional: si e.x está fuera de rango por interpolación, usar último
                            if (ds.entryCount > 0) {
                                entry = ds.getEntryForIndex(min(ds.entryCount - 1, idxRedondeado.coerceAtLeast(0)))
                            }
                        }
                        if (entry != null) {
                            if (builder.isNotEmpty()) builder.append('\n')
                            val etiqueta = ds.label
                            val valor = oneDecimal.format(entry.y)
                            val valorConUnidad = when (etiqueta.lowercase(Locale.getDefault())) {
                                "peso" -> valor + " kg"
                                "altura" -> valor + " m"
                                "percentil" -> valor + "%"
                                else -> valor // IMC u otros
                            }
                            builder.append(etiqueta).append(": ").append(valorConUnidad)
                        }
                    }
                }
                tvFecha.text = fechaTxt
                tvValores.text = builder.toString()
            } catch (_: Exception) { }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF = MPPointF(-width / 2f, -height - 20f)
    }
}
