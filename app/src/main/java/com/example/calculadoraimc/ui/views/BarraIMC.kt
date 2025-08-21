package com.example.calculadoraimc.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import com.chaquo.python.Python
import java.util.*

class BarraIMC @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var currentIMC: Float = 0f
    private var showIndicator = false
    private var ranges: List<IMCRange> = emptyList()

    init {
        textPaint.apply {
            textSize = 28f
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        indicatorPaint.apply {
            color = Color.BLACK
            strokeWidth = 6f
            style = Paint.Style.FILL
        }

        // Cargar rangos desde Python
        loadRangesFromPython()
    }

    private fun loadRangesFromPython() {
        try {
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")
            val pyRanges = funcionesModule.callAttr("obtener_rangos_imc")

            ranges = pyRanges.asList().map { pyRange ->
                IMCRange(
                    name = pyRange.callAttr("get", "nombre")?.toString() ?: "",
                    displayRange = pyRange.callAttr("get", "rango_texto")?.toString() ?: "",
                    minValue = pyRange.callAttr("get", "min_valor")?.toFloat() ?: 0f,
                    maxValue = pyRange.callAttr("get", "max_valor")?.toFloat() ?: 0f,
                    color = pyRange.callAttr("get", "color")?.toString()?.toColorInt() ?: Color.BLACK
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BarraIMC", "Error al cargar rangos: ${e.message}")
            ranges = emptyList()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBarraIMC(canvas)
    }

    private fun drawBarraIMC(canvas: Canvas) {
        val barHeight = height * 0.3f
        val barTop = height * 0.35f
        val barBottom = barTop + barHeight

        val totalWidth = width.toFloat()
        val segmentWidth = totalWidth / ranges.size

        // Dibujar segmentos de la barra
        ranges.forEachIndexed { index, range ->
            val left = index * segmentWidth
            val right = left + segmentWidth

            paint.color = range.color
            canvas.drawRect(left, barTop, right, barBottom, paint)

            // Dibujar texto del rango
            val centerX = left + segmentWidth / 2

            // Nombre del rango
            canvas.drawText(range.name, centerX, barTop - 20f, textPaint)

            // Valores del rango
            textPaint.textSize = 24f
            canvas.drawText(range.displayRange, centerX, barBottom + 40f, textPaint)
            textPaint.textSize = 28f
        }

        // Dibujar indicador si hay un IMC establecido
        if (showIndicator && currentIMC > 0) {
            drawIndicator(canvas, barTop, barBottom)
        }
    }

    private fun drawIndicator(canvas: Canvas, barTop: Float, barBottom: Float) {
        val position = calculatePositionFromPython(currentIMC)
        val x = position * width

        // Dibujar línea indicadora
        indicatorPaint.style = Paint.Style.STROKE
        indicatorPaint.strokeWidth = 6f
        canvas.drawLine(x, barTop - 40f, x, barBottom + 60f, indicatorPaint)

        // Dibujar triángulo en la parte superior
        val trianglePath = Path().apply {
            moveTo(x, barTop - 10f)
            lineTo(x - 15f, barTop - 40f)
            lineTo(x + 15f, barTop - 40f)
            close()
        }
        indicatorPaint.style = Paint.Style.FILL
        canvas.drawPath(trianglePath, indicatorPaint)

        // Dibujar valor del IMC
        val imcText = String.format(Locale.getDefault(), "%.1f", currentIMC)
        textPaint.color = Color.BLACK
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(imcText, x, barTop - 50f, textPaint)
        textPaint.typeface = Typeface.DEFAULT
        textPaint.textSize = 28f
    }

    private fun calculatePositionFromPython(imc: Float): Float {
        return try {
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")
            funcionesModule.callAttr("calcular_posicion_en_barra", imc).toFloat()
        } catch (_: Exception) {
            // Si falla Python, mostrar en posición por defecto (centro de la barra)
            // En lugar de duplicar lógica de cálculo
            0.5f // Posición central como fallback seguro
        }
    }

    fun setIMC(imc: Float) {
        currentIMC = imc
        showIndicator = true
        invalidate()
    }

    fun clearIndicator() {
        showIndicator = false
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 200
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    data class IMCRange(
        val name: String,
        val displayRange: String,
        val minValue: Float,
        val maxValue: Float,
        val color: Int
    )
}
