package com.example.calculadoraimc.ui.adultos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentAdultosBinding
import com.chaquo.python.Python
import android.view.inputmethod.InputMethodManager
import android.content.Context
import com.example.calculadoraimc.utils.MeasurementUtils
import com.example.calculadoraimc.utils.MeasurementUtils.MeasurementSystem

class AdultosFragment : Fragment() {

    private var _binding: FragmentAdultosBinding? = null
    private val binding get() = _binding!!
    private lateinit var measurementSystem: MeasurementSystem

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdultosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Determinar sistema de medición según localización
        measurementSystem = MeasurementUtils.getPreferredSystem(requireContext())

        // Configurar etiquetas según sistema de medición
        setupMeasurementLabels()

        setupClickListeners()

        return root
    }

    /**
     * Configura los hints según el sistema de medición detectado
     */
    private fun setupMeasurementLabels() {
        when (measurementSystem) {
            MeasurementSystem.METRIC -> {
                // Sistema métrico (cm, kg)
                binding.tilPeso.hint = getString(R.string.hint_peso)
                binding.tilAltura.hint = getString(R.string.hint_altura)
            }
            MeasurementSystem.IMPERIAL -> {
                // Sistema imperial (lb, in)
                binding.tilPeso.hint = getString(R.string.hint_peso_imperial)
                binding.tilAltura.hint = getString(R.string.hint_altura_imperial)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCalcular.setOnClickListener {
            calcularIMC()
        }

        binding.btnGuardar.setOnClickListener {
            guardarMedicion()
        }
    }

    private fun calcularIMC() {
        // Esconder el teclado al presionar calcular
        hideKeyboard()

        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_campos_vacios_adultos), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Convertir valores al sistema métrico si es necesario
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toFloat(), alturaText.toFloat())
                }
                MeasurementSystem.IMPERIAL -> {
                    // Convertir de libras a kg y de pulgadas a cm
                    val pesoLbs = pesoText.toFloat()
                    val alturaInches = alturaText.toFloat()

                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)
                    val alturaCm = MeasurementUtils.inchesToCm(alturaInches)

                    Pair(pesoKg, alturaCm)
                }
            }

            // Llamar a Python con los valores convertidos a métrico
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Usar try-catch interno para manejar errores específicos de Python
            val imc = try {
                funcionesModule.callAttr("calcular_imc", pesoKg.toString(), alturaCm.toString()).toDouble()
            } catch (e: Exception) {
                // Si hay error en el cálculo, mostrar mensaje específico
                Toast.makeText(context, getString(R.string.error_datos_invalidos, e.message ?: ""), Toast.LENGTH_LONG).show()
                return
            }

            // Obtener interpretación
            val interpretacionKey = try {
                funcionesModule.callAttr("interpretar_imc", imc).toString()
            } catch (e: Exception) {
                // Si hay error en la interpretación, mostrar mensaje específico
                Toast.makeText(context, getString(R.string.error_calculo, e.message ?: ""), Toast.LENGTH_LONG).show()
                return
            }

            val interpretacionTexto = when (interpretacionKey) {
                "interpretacion_bajo_peso_adulto" -> getString(R.string.interpretacion_bajo_peso_adulto)
                "interpretacion_normal_adulto" -> getString(R.string.interpretacion_normal_adulto)
                "interpretacion_sobrepeso_adulto" -> getString(R.string.interpretacion_sobrepeso_adulto)
                "interpretacion_obesidad_1_adulto" -> getString(R.string.interpretacion_obesidad_1_adulto)
                "interpretacion_obesidad_2_adulto" -> getString(R.string.interpretacion_obesidad_2_adulto)
                "interpretacion_obesidad_3_adulto" -> getString(R.string.interpretacion_obesidad_3_adulto)
                "" -> getString(R.string.sin_interpretacion)
                else -> {
                    android.util.Log.w("AdultosFragment", "Clave de interpretación desconocida recibida de Python: $interpretacionKey")
                    getString(R.string.sin_interpretacion)
                }
            }

            // Mostrar resultados
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvInterpretacion.text = interpretacionTexto

            // Actualizar la barra de IMC con el valor calculado
            binding.barraImc.setIMC(imc.toFloat())

            binding.cardResultado.visibility = View.VISIBLE

            // Auto-scroll para mostrar los resultados y el botón de guardar
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.cardResultado.bottom)
            }

        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.error_calculo, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarMedicion() {
        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_calcular_primero_adultos), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Convertir valores al sistema métrico para guardar
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toFloat(), alturaText.toFloat())
                }
                MeasurementSystem.IMPERIAL -> {
                    // Convertir de libras a kg y de pulgadas a cm
                    val pesoLbs = pesoText.toFloat()
                    val alturaInches = alturaText.toFloat()

                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)
                    val alturaCm = MeasurementUtils.inchesToCm(alturaInches)

                    Pair(pesoKg, alturaCm)
                }
            }

            // Python espera los valores en sistema métrico
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Usar try-catch interno para manejar errores específicos de Python
            val imc = try {
                funcionesModule.callAttr("calcular_imc", pesoKg.toString(), alturaCm.toString()).toDouble()
            } catch (e: Exception) {
                // Si hay error en el cálculo, mostrar mensaje específico
                Toast.makeText(context, getString(R.string.error_calculo_guardar, e.message ?: ""), Toast.LENGTH_LONG).show()
                return
            }

            // Guardar datos en formato métrico estándar
            funcionesModule.callAttr("guardar_medicion", pesoKg.toString(), alturaCm.toString(), imc)

            Toast.makeText(context, getString(R.string.exito_guardado), Toast.LENGTH_SHORT).show()

            // Limpiar campos
            binding.etPeso.text?.clear()
            binding.etAltura.text?.clear()
            binding.barraImc.clearIndicator()
            binding.cardResultado.visibility = View.GONE

        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.error_guardar, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        currentFocusedView?.let { view ->
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
