package com.example.calculadoraimc.ui.menores

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentMenoresBinding
import com.chaquo.python.Python

class MenoresFragment : Fragment() {

    private var _binding: FragmentMenoresBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenoresBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupClickListeners()
        setupTextWatchers()

        return root
    }

    fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupClickListeners() {
        binding.btnCalcular.setOnClickListener {
            calcularPercentil()
        }

        binding.btnGuardar.setOnClickListener {
            guardarMedicion()
        }

        // Botón para mostrar/ocultar información sobre percentiles
        binding.btnToggleInfo.setOnClickListener {
            toggleInfoPercentiles()
        }
    }

    private fun toggleInfoPercentiles() {
        val infoTextView = binding.tvInfoPercentiles
        val toggleButton = binding.btnToggleInfo

        if (infoTextView.visibility == View.GONE) {
            // Mostrar información
            infoTextView.visibility = View.VISIBLE
            toggleButton.text = getString(R.string.btn_ocultar_info)
            toggleButton.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            // Ocultar información
            infoTextView.visibility = View.GONE
            toggleButton.text = getString(R.string.btn_mostrar_info)
            toggleButton.setIconResource(android.R.drawable.ic_dialog_info)
        }
    }

    private fun setupTextWatchers() {
        // Implementación recomendada: campo único, solo dígitos, máscara según locale
        val dateOrder = detectDateOrder()
        val watcher = object : TextWatcher {
            var selfChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (selfChange) return
                val original = s?.toString() ?: ""
                val digits = original.replace(Regex("[^0-9]"), "")

                // Limitar a 8 dígitos (DDMMYYYY / MMDDYYYY / YYYYMMDD)
                val maxDigits = 8
                val limited = if (digits.length > maxDigits) digits.substring(0, maxDigits) else digits

                val formatted = formatDigitsAsDate(limited, dateOrder)

                try {
                    selfChange = true
                    binding.etFechaNacimiento.setText(formatted)
                    binding.etFechaNacimiento.setSelection(formatted.length)
                } finally {
                    selfChange = false
                }

                // Validar cuando tengamos la longitud esperada
                val expectedDigits = 8
                if (limited.length == expectedDigits) {
                    val pattern = patternWithSeparators(dateOrder)
                    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                    sdf.isLenient = false
                    try {
                        sdf.parse(formatted)
                        binding.tilFechaNacimiento.error = null
                        hideKeyboard(binding.etFechaNacimiento)
                        binding.etFechaNacimiento.clearFocus()
                    } catch (e: Exception) {
                        binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
                    }
                } else {
                    binding.tilFechaNacimiento.error = null
                }
            }
        }

        binding.etFechaNacimiento.addTextChangedListener(watcher)
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Detecta orden preferido: "DMY", "MDY" o "YMD"
    private fun detectDateOrder(): String {
        return try {
            val best = android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "yMd")
            val lower = best.lowercase(java.util.Locale.getDefault())
            val dIndex = lower.indexOf('d')
            val mIndex = lower.indexOf('M')
            val yIndex = lower.indexOf('y')
            if (dIndex >= 0 && mIndex >= 0 && dIndex < mIndex) {
                "DMY"
            } else if (mIndex >= 0 && dIndex >= 0 && mIndex < dIndex) {
                "MDY"
            } else if (yIndex >= 0 && yIndex < Math.min(dIndex.takeIf { it>=0 } ?: Int.MAX_VALUE, mIndex.takeIf { it>=0 } ?: Int.MAX_VALUE)) {
                "YMD"
            } else {
                "DMY"
            }
        } catch (_: Exception) {
            "DMY"
        }
    }

    private fun formatDigitsAsDate(digits: String, order: String): String {
        when (order) {
            "DMY" -> {
                val sb = StringBuilder()
                if (digits.length >= 2) sb.append(digits.substring(0, 2)) else sb.append(digits)
                if (digits.length > 2) sb.append('/').append(if (digits.length >= 4) digits.substring(2, 4) else digits.substring(2))
                if (digits.length > 4) sb.append('/').append(digits.substring(4))
                return sb.toString()
            }
            "MDY" -> {
                val sb = StringBuilder()
                if (digits.length >= 2) sb.append(digits.substring(0, 2)) else sb.append(digits)
                if (digits.length > 2) sb.append('/').append(if (digits.length >= 4) digits.substring(2, 4) else digits.substring(2))
                if (digits.length > 4) sb.append('/').append(digits.substring(4))
                return sb.toString()
            }
            "YMD" -> {
                val sb = StringBuilder()
                if (digits.length >= 4) sb.append(digits.substring(0, 4)) else sb.append(digits)
                if (digits.length > 4) sb.append('/').append(if (digits.length >= 6) digits.substring(4, 6) else digits.substring(4))
                if (digits.length > 6) sb.append('/').append(digits.substring(6))
                return sb.toString()
            }
        }
        return digits
    }

    private fun patternWithSeparators(order: String): String {
        return when (order) {
            "DMY" -> "dd/MM/yyyy"
            "MDY" -> "MM/dd/yyyy"
            "YMD" -> "yyyy/MM/dd"
            else -> "dd/MM/yyyy"
        }
    }

    private fun calcularPercentil() {
        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_campos_vacios_menores), Toast.LENGTH_SHORT).show()
            return
        }

        // Validar fecha localmente: formato, año de 4 dígitos, no futura y rango 5-19 años
        val order = detectDateOrder()
        val pattern = patternWithSeparators(order)
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        sdf.isLenient = false
        val dobDate = try {
            // Aceptamos la fecha formateada con separadores (dd/MM/yyyy etc.)
            val parsed = sdf.parse(fechaNacimiento)
            parsed
        } catch (e: Exception) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        // Comprobar fecha futura
        val today = java.util.Calendar.getInstance()
        val dobCal = java.util.Calendar.getInstance().apply { time = dobDate }
        if (dobCal.after(today)) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        // Calcular edad en años
        var ageYears = today.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
        val todayMonth = today.get(java.util.Calendar.MONTH)
        val dobMonth = dobCal.get(java.util.Calendar.MONTH)
        val todayDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        val dobDay = dobCal.get(java.util.Calendar.DAY_OF_MONTH)
        if (todayMonth < dobMonth || (todayMonth == dobMonth && todayDay < dobDay)) {
            ageYears -= 1
        }

        if (ageYears < 5 || ageYears > 19) {
            binding.tilFechaNacimiento.error = getString(R.string.error_edad_rango)
            Toast.makeText(context, getString(R.string.error_edad_rango), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sexo = if (binding.rbMasculino.isChecked) "Masculino" else "Femenino"

            // Debug: Mostrar qué datos estamos enviando
            android.util.Log.d("MenoresFragment", "Enviando a Python:")
            android.util.Log.d("MenoresFragment", "  Sexo: $sexo")
            android.util.Log.d("MenoresFragment", "  Fecha: '$fechaNacimiento'")
            android.util.Log.d("MenoresFragment", "  Peso: '$pesoText'")
            android.util.Log.d("MenoresFragment", "  Altura: '$alturaText'")

            // Llamar a la función de Python que usa fecha de nacimiento
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Python maneja la validación y conversión de fecha automáticamente
            val resultado = funcionesModule.callAttr("calcular_imc_menor_por_fecha", sexo, fechaNacimiento, pesoText, alturaText)

            // Debug: Mostrar qué devuelve Python
            android.util.Log.d("MenoresFragment", "Resultado Python completo: $resultado")

            // Verificar si hay error en el resultado
            if (resultado.containsKey("error")) {
                val error = resultado["error"]?.toString() ?: "Error desconocido"
                android.util.Log.e("MenoresFragment", "Error de Python: $error")
                Toast.makeText(context, getString(R.string.error_percentiles, error), Toast.LENGTH_LONG).show()
                return
            }

            // Extraer valores usando el método correcto para objetos Python en Chaquopy
            val imc = resultado.callAttr("get", "imc")?.toDouble() ?: 0.0
            val percentil = resultado.callAttr("get", "percentil")?.toDouble() ?: 0.0
            // La función Python interpretar_percentil ahora devuelve una clave de recurso.
            val interpretacionKey = resultado.callAttr("get", "interpretacion")?.toString() ?: ""
            val edadAnios = resultado.callAttr("get", "edad_años")?.toDouble() ?: 0.0

            // Debug: Mostrar valores extraídos
            android.util.Log.d("MenoresFragment", "Valores extraídos:")
            android.util.Log.d("MenoresFragment", "  IMC: $imc")
            android.util.Log.d("MenoresFragment", "  Percentil: $percentil")
            android.util.Log.d("MenoresFragment", "  Interpretación clave: '$interpretacionKey'")
            android.util.Log.d("MenoresFragment", "  Edad años: $edadAnios")

            // Mostrar resultados (incluyendo edad calculada)
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvPercentil.text = getString(R.string.formato_percentil, percentil)
            val interpretacionTexto = when (interpretacionKey) {
                "interpretacion_bajo_peso" -> getString(R.string.interpretacion_bajo_peso)
                "interpretacion_peso_saludable" -> getString(R.string.interpretacion_peso_saludable)
                "interpretacion_sobrepeso" -> getString(R.string.interpretacion_sobrepeso)
                "interpretacion_obesidad" -> getString(R.string.interpretacion_obesidad)
                "" -> getString(R.string.sin_interpretacion)
                else -> {
                    // Python devolvió una clave inesperada: registrar y usar el texto por defecto
                    android.util.Log.w("MenoresFragment", "Clave de interpretación desconocida recibida de Python: $interpretacionKey")
                    getString(R.string.sin_interpretacion)
                }
            }

            binding.tvInterpretacion.text = getString(R.string.interpretacion_con_edad, interpretacionTexto, edadAnios)

            // Mostrar y actualizar la barra de percentiles
            binding.barraPercentilMenores.setPercentil(percentil)
            binding.barraPercentilMenores.visibility = View.VISIBLE

            binding.cardResultado.visibility = View.VISIBLE

            // Auto-scroll para mostrar los resultados y el botón de guardar
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.cardResultado.bottom)
            }
        } catch (_: Exception) {
            android.util.Log.e("MenoresFragment", "Excepción en calcularPercentil")
            Toast.makeText(context, getString(R.string.error_calculo, ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarMedicion() {
        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_calcular_primero_menores), Toast.LENGTH_SHORT).show()
            return
        }

        // Validar fecha de nuevo antes de guardar
        val orderSave = detectDateOrder()
        val patternSave = patternWithSeparators(orderSave)
        val sdfSave = java.text.SimpleDateFormat(patternSave, java.util.Locale.getDefault())
        sdfSave.isLenient = false
        val dobDateSave = try {
            sdfSave.parse(fechaNacimiento)
        } catch (e: Exception) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        val todaySave = java.util.Calendar.getInstance()
        val dobCalSave = java.util.Calendar.getInstance().apply { time = dobDateSave }
        if (dobCalSave.after(todaySave)) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        var ageYearsSave = todaySave.get(java.util.Calendar.YEAR) - dobCalSave.get(java.util.Calendar.YEAR)
        if (todaySave.get(java.util.Calendar.MONTH) < dobCalSave.get(java.util.Calendar.MONTH) ||
            (todaySave.get(java.util.Calendar.MONTH) == dobCalSave.get(java.util.Calendar.MONTH) &&
                    todaySave.get(java.util.Calendar.DAY_OF_MONTH) < dobCalSave.get(java.util.Calendar.DAY_OF_MONTH))) {
            ageYearsSave -= 1
        }

        if (ageYearsSave < 5 || ageYearsSave > 19) {
            binding.tilFechaNacimiento.error = getString(R.string.error_edad_rango)
            Toast.makeText(context, getString(R.string.error_edad_rango), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sexo = if (binding.rbMasculino.isChecked) "Masculino" else "Femenino"

            // Python maneja todas las conversiones automáticamente
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Calculamos nuevamente usando la nueva función por fecha
            val resultado = funcionesModule.callAttr("calcular_imc_menor_por_fecha", sexo, fechaNacimiento, pesoText, alturaText)

            if (resultado.containsKey("error") != true) {
                // Usar la sintaxis correcta para extraer valores de objetos Python
                val imc = resultado.callAttr("get", "imc")?.toDouble() ?: 0.0
                val percentil = resultado.callAttr("get", "percentil")?.toDouble() ?: 0.0
                val edadMeses = resultado.callAttr("get", "edad_meses")?.toInt() ?: 0

                // Convertir peso y altura a valores numéricos antes de guardar
                val pesoNumerico = pesoText.toDouble()
                val alturaNumerico = alturaText.toDouble()

                // Log para verificar que los valores son correctos antes de guardar
                android.util.Log.d("MenoresFragment", "Guardando medición:")
                android.util.Log.d("MenoresFragment", "  Peso: $pesoNumerico")
                android.util.Log.d("MenoresFragment", "  Altura: $alturaNumerico")
                android.util.Log.d("MenoresFragment", "  IMC: $imc")
                android.util.Log.d("MenoresFragment", "  Sexo: $sexo")
                android.util.Log.d("MenoresFragment", "  Edad meses: $edadMeses")
                android.util.Log.d("MenoresFragment", "  Percentil: $percentil")

                funcionesModule.callAttr("guardar_medicion", pesoNumerico, alturaNumerico, imc, sexo, edadMeses, percentil)

                Toast.makeText(context, getString(R.string.exito_guardado), Toast.LENGTH_SHORT).show()

                // Limpiar campos
                binding.etPeso.text?.clear()
                binding.etAltura.text?.clear()
                binding.etFechaNacimiento.text?.clear()
                binding.rbMasculino.isChecked = true
                binding.cardResultado.visibility = View.GONE
            }

        } catch (_: Exception) {
            Toast.makeText(context, getString(R.string.error_guardar, ""), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
