package com.example.calculadoraimc.ui.menores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentMenoresBinding
import com.example.calculadoraimc.ui.views.BarraPercentil
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

        return root
    }

    private fun setupClickListeners() {
        binding.btnCalcular.setOnClickListener {
            calcularPercentil()
        }

        binding.btnGuardar.setOnClickListener {
            guardarMedicion()
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
            val edadAños = resultado.callAttr("get", "edad_años")?.toDouble() ?: 0.0

            // Debug: Mostrar valores extraídos
            android.util.Log.d("MenoresFragment", "Valores extraídos:")
            android.util.Log.d("MenoresFragment", "  IMC: $imc")
            android.util.Log.d("MenoresFragment", "  Percentil: $percentil")
            android.util.Log.d("MenoresFragment", "  Interpretación: '$interpretacion'")
            android.util.Log.d("MenoresFragment", "  Edad años: $edadAños")

            // Mostrar resultados (incluyendo edad calculada)
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvPercentil.text = getString(R.string.formato_percentil, percentil)
            binding.tvInterpretacion.text = "$interpretacion (${edadAños} años)"

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
