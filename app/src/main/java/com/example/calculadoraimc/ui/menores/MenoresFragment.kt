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
        binding.etFechaNacimiento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""

                // Ocultar el teclado cuando se detecte una fecha potencialmente completa
                if (shouldHideKeyboard(text)) {
                    hideKeyboard(binding.etFechaNacimiento)
                }
            }
        })
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun shouldHideKeyboard(dateText: String): Boolean {
        // Remover espacios y caracteres especiales para el análisis
        val cleanText = dateText.replace(Regex("[^0-9]"), "")

        // Si tiene 8 dígitos (formato DDMMAAAA), es una fecha completa
        if (cleanText.length == 8) {
            return true
        }

        // Si el texto contiene formato de fecha con separadores (dd/mm/yyyy, dd-mm-yyyy, etc.)
        val datePatterns = listOf(
            Regex("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}"), // dd/mm/yyyy o dd-mm-yyyy
            Regex("\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2}"), // yyyy/mm/dd o yyyy-mm-dd
            Regex("\\d{1,2}/\\d{1,2}/\\d{4}"),              // dd/mm/yyyy específicamente
            Regex("\\d{1,2}-\\d{1,2}-\\d{4}")               // dd-mm-yyyy específicamente
        )

        for (pattern in datePatterns) {
            if (pattern.matches(dateText)) {
                return true
            }
        }

        // Si se han escrito al menos 4 dígitos consecutivos al final (año completo)
        val yearMatch = Regex(".*\\d{4}$").matches(dateText)
        if (yearMatch) {
            return true
        }

        return false
    }

    private fun calcularPercentil() {
        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_campos_vacios_menores), Toast.LENGTH_SHORT).show()
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
            val interpretacion = resultado.callAttr("get", "interpretacion")?.toString() ?: getString(R.string.sin_interpretacion)
            val edadAnios = resultado.callAttr("get", "edad_años")?.toDouble() ?: 0.0

            // Debug: Mostrar valores extraídos
            android.util.Log.d("MenoresFragment", "Valores extraídos:")
            android.util.Log.d("MenoresFragment", "  IMC: $imc")
            android.util.Log.d("MenoresFragment", "  Percentil: $percentil")
            android.util.Log.d("MenoresFragment", "  Interpretación: '$interpretacion'")
            android.util.Log.d("MenoresFragment", "  Edad años: $edadAnios")

            // Mostrar resultados (incluyendo edad calculada)
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvPercentil.text = getString(R.string.formato_percentil, percentil)
            binding.tvInterpretacion.text = getString(R.string.interpretacion_con_edad, interpretacion, edadAnios)

            // Mostrar y actualizar la barra de percentiles
            binding.barraPercentilMenores.setPercentil(percentil)
            binding.barraPercentilMenores.visibility = View.VISIBLE

            binding.cardResultado.visibility = View.VISIBLE

            // Auto-scroll para mostrar los resultados y el botón de guardar
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.cardResultado.bottom)
            }

        } catch (e: Exception) {
            android.util.Log.e("MenoresFragment", "Excepción en calcularPercentil: ${e.message}", e)
            Toast.makeText(context, getString(R.string.error_calculo, e.message ?: ""), Toast.LENGTH_SHORT).show()
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

        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.error_guardar, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
