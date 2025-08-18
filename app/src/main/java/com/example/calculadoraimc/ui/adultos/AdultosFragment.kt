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

class AdultosFragment : Fragment() {

    private var _binding: FragmentAdultosBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdultosBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupClickListeners()

        return root
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
        val pesoText = binding.etPeso.text.toString()
        val alturaText = binding.etAltura.text.toString()

        if (pesoText.isEmpty() || alturaText.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_campos_vacios_adultos), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Llamar directamente a Python - Python maneja todas las conversiones
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Usar try-catch interno para manejar errores específicos de Python
            val imc = try {
                funcionesModule.callAttr("calcular_imc", pesoText, alturaText).toDouble()
            } catch (e: Exception) {
                // Si hay error en el cálculo, mostrar mensaje específico
                Toast.makeText(context, "Error en los datos: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }

            val interpretacion = funcionesModule.callAttr("interpretar_imc", imc).toString()

            // Mostrar resultados
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvInterpretacion.text = interpretacion

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
            // Python maneja las conversiones automáticamente
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Usar try-catch interno para manejar errores específicos de Python
            val imc = try {
                funcionesModule.callAttr("calcular_imc", pesoText, alturaText).toDouble()
            } catch (e: Exception) {
                // Si hay error en el cálculo, mostrar mensaje específico
                Toast.makeText(context, "Error al calcular IMC para guardar: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }

            funcionesModule.callAttr("guardar_medicion", pesoText, alturaText, imc)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
