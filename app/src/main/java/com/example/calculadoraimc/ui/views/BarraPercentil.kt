package com.example.calculadoraimc.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import com.chaquo.python.Python

data class RangoPercentil(
    val nombre: String,
    val rangoTexto: String,
    val minValor: Double,
    val maxValor: Double,
    val color: Int
)

class BarraPercentil @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ranges = emptyList<RangoPercentil>()
    private var indicatorPosition = 0f
    private var percentilValue = 0.0

    init {
        loadRangosPercentiles()
    }

    fun setPercentil(percentil: Double) {
        percentilValue = percentil
        updateIndicatorPosition()
        invalidate()
    }

    private fun loadRangosPercentiles() {
        val python = Python.getInstance()
        val module = python.getModule("funciones_imc_android")

        val rangosData = module.callAttr("obtener_rangos_percentiles").asList()

        ranges = rangosData.map { rangoMap ->
            val mapa = rangoMap.asMap()
            val key = mapa[com.chaquo.python.PyObject.fromJava("key")]?.toString() ?: ""
            val rawName = mapa[com.chaquo.python.PyObject.fromJava("nombre")]?.toString() ?: ""
            val localizedName = resolveRangeNamePercentil(key, rawName)

            RangoPercentil(
                nombre = localizedName,
                rangoTexto = mapa[com.chaquo.python.PyObject.fromJava("rango_texto")]?.toString() ?: "",
                minValor = mapa[com.chaquo.python.PyObject.fromJava("min_valor")]?.toString()?.toDouble() ?: 0.0,
                maxValor = mapa[com.chaquo.python.PyObject.fromJava("max_valor")]?.toString()?.toDouble() ?: 0.0,
                color = Color.parseColor(mapa[com.chaquo.python.PyObject.fromJava("color")]?.toString() ?: "#2196F3")
            )
        }
    }

    // Resolver claves de percentiles a recursos localizados
    private fun resolveRangeNamePercentil(key: String, fallback: String): String {
        return when (key) {
            "bajo_peso" -> context.getString(com.example.calculadoraimc.R.string.bajo_peso)
            "peso_saludable" -> context.getString(com.example.calculadoraimc.R.string.peso_saludable)
            "sobrepeso" -> context.getString(com.example.calculadoraimc.R.string.sobrepeso)
            "obesidad" -> context.getString(com.example.calculadoraimc.R.string.obesidad)
            "" -> fallback
            else -> {
                android.util.Log.w("BarraPercentil", "Clave de percentil desconocida recibida de Python: $key")
                fallback
            }
        }
    }

    private fun updateIndicatorPosition() {
        val python = Python.getInstance()
        val module = python.getModule("funciones_imc_android")

        val position = module.callAttr("calcular_posicion_en_barra_percentil", percentilValue).toDouble()
        indicatorPosition = position.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBarraPercentil(canvas)
    }

    private fun drawBarraPercentil(canvas: Canvas) {
        if (ranges.isEmpty()) return

        val barHeight = height * 0.3f
        val barTop = height * 0.35f
        val barBottom = barTop + barHeight

        val totalWidth = width.toFloat()

        // Dibujar segmentos de la barra con anchos proporcionales
        var currentX = 0f
        ranges.forEachIndexed { index, range ->
            val segmentWidth = when (index) {
                0 -> totalWidth * 0.15f  // Bajo peso: 15%
                1 -> totalWidth * 0.55f  // Peso saludable: 55%
                2 -> totalWidth * 0.20f  // Sobrepeso: 20%
                3 -> totalWidth * 0.10f  // Obesidad: 10%
                else -> 0f
            }

            paint.color = range.color
            canvas.drawRect(currentX, barTop, currentX + segmentWidth, barBottom, paint)

            // Dibujar texto del rango
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.textAlign = Paint.Align.CENTER

            val textX = currentX + segmentWidth / 2
            val textY = barTop - 10f
            canvas.drawText(range.rangoTexto, textX, textY, paint)

            // Dibujar nombre del rango debajo de la barra
            paint.textSize = 20f
            val nameY = barBottom + 30f
            canvas.drawText(range.nombre, textX, nameY, paint)

            currentX += segmentWidth
        }

        // Dibujar indicador del percentil actual
        if (percentilValue > 0) {
            val indicatorX = totalWidth * indicatorPosition
            val indicatorWidth = 8f

            // Línea del indicador
            paint.color = Color.BLACK
            paint.strokeWidth = indicatorWidth
            canvas.drawLine(
                indicatorX,
                barTop - 20f,
                indicatorX,
                barBottom + 20f,
                paint
            )

            // Círculo en la parte superior del indicador
            paint.style = Paint.Style.FILL
            canvas.drawCircle(indicatorX, barTop - 20f, 12f, paint)

            // Texto con el valor del percentil
            paint.color = Color.BLACK
            paint.textSize = 28f
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            canvas.drawText(
                "P${percentilValue.toInt()}",
                indicatorX,
                barTop - 40f,
                paint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultHeight = (context.resources.displayMetrics.density * 120).toInt()
        val height = resolveSize(defaultHeight, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, height)
    }
}
