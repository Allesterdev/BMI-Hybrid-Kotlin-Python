package com.example.calculadoraimc.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculadoraimc.databinding.FragmentNotificationsBinding
import com.chaquo.python.Python
import android.graphics.Color
import android.graphics.Typeface

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistorialAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.textNotifications.visibility = View.GONE

        setupRecyclerView()
        setupToggleButtons()

        consultarHistorialAdultos()

        return root
    }

    private fun setupRecyclerView() {
        adapter = HistorialAdapter(emptyList())
        binding.rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorial.adapter = adapter
    }

    private fun setupToggleButtons() {
        // Configurar estado inicial
        updateTextColors(isAdultosSelected = true)

        binding.slidingPill.translationX = 0f

        binding.btnAdultos.setOnClickListener {
            selectAdultos()
        }

        binding.btnMenores.setOnClickListener {
            selectMenores()
        }
    }

    private fun selectAdultos() {
        // Animar la píldora hacia la izquierda
        binding.slidingPill.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Actualizar colores del texto
        updateTextColors(isAdultosSelected = true)

        consultarHistorialAdultos()
    }

    private fun selectMenores() {
        // Calcular posición de destino y animar
        binding.btnAdultos.post {
            val destinationX = binding.btnAdultos.width.toFloat()
            binding.slidingPill.animate()
                .translationX(destinationX)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        // Actualizar colores del texto
        updateTextColors(isAdultosSelected = false)

        consultarHistorialMenores()
    }

    private fun updateTextColors(isAdultosSelected: Boolean) {
        if (isAdultosSelected) {
            // Adultos seleccionado: texto verde (sobre fondo blanco de la píldora)
            // Menores no seleccionado: texto blanco (sobre fondo verde del contenedor)
            binding.btnAdultos.setTextColor(Color.parseColor("#2E7D32"))
            binding.btnMenores.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            // Adultos no seleccionado: texto blanco (sobre fondo verde del contenedor)
            // Menores seleccionado: texto verde (sobre fondo blanco de la píldora)
            binding.btnAdultos.setTextColor(Color.parseColor("#FFFFFF"))
            binding.btnMenores.setTextColor(Color.parseColor("#2E7D32"))
        }
    }

    private fun consultarHistorialAdultos() {
        try {
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")
            val resultado = funcionesModule.callAttr("obtener_historial_adultos")
            val lista = resultado.asList()

            val historial = lista.map { item ->
                val map = item.asMap()
                HistorialAdulto(
                    peso = map[com.chaquo.python.PyObject.fromJava("peso")]?.toString()?.toDouble() ?: 0.0,
                    altura = map[com.chaquo.python.PyObject.fromJava("altura")]?.toString()?.toDouble() ?: 0.0,
                    imc = map[com.chaquo.python.PyObject.fromJava("imc")]?.toString()?.toDouble() ?: 0.0,
                    fecha = map[com.chaquo.python.PyObject.fromJava("fecha")]?.toString() ?: ""
                )
            }

            adapter.updateDataAdultos(historial)
            binding.tvHistorialVacio.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error al consultar historial de adultos: ${e.message}", e)
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            adapter.clearData()
            binding.tvHistorialVacio.visibility = View.VISIBLE
        }
    }

    private fun consultarHistorialMenores() {
        try {
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")
            val resultado = funcionesModule.callAttr("obtener_historial_menores")
            val lista = resultado.asList()

            val historial = lista.mapIndexed { _, item ->
                val map = item.asMap()

                fun getDoubleValue(key: String): Double {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    return when {
                        value == null -> 0.0
                        value.toString() == "None" -> 0.0
                        else -> value.toString().toDoubleOrNull() ?: 0.0
                    }
                }

                fun getIntValue(key: String): Int {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    return when {
                        value == null -> 0
                        value.toString() == "None" -> 0
                        else -> value.toString().toIntOrNull() ?: 0
                    }
                }

                fun getStringValue(key: String): String {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    return when {
                        value == null -> ""
                        value.toString() == "None" -> ""
                        else -> value.toString()
                    }
                }

                HistorialMenor(
                    peso = getDoubleValue("peso"),
                    altura = getDoubleValue("altura"),
                    imc = getDoubleValue("imc"),
                    fecha = getStringValue("fecha"),
                    sexo = getStringValue("sexo"),
                    edadMeses = getIntValue("edad_meses"),
                    percentil = getDoubleValue("percentil")
                )
            }

            adapter.updateDataMenores(historial)
            binding.tvHistorialVacio.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            android.util.Log.e("NotificationsFragment", "Error al consultar historial de menores: ${e.message}", e)
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            adapter.clearData()
            binding.tvHistorialVacio.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}