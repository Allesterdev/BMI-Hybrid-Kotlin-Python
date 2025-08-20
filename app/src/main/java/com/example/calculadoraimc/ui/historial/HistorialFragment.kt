package com.example.calculadoraimc.ui.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentHistorialBinding
import com.chaquo.python.Python
import android.graphics.Color


class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistorialAdapter

    // Variable para rastrear el modo actual
    private var modoActual: String = "adultos"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.textHistorial.visibility = View.GONE

        // Obtener el tipo de historial del argumento (si viene del gráfico)
        val tipoHistorial = arguments?.getString("tipo_historial") ?: "adultos"
        modoActual = tipoHistorial

        setupRecyclerView()
        setupToggleButtons()

        // Navegación al gráfico
        binding.btnVerGrafico.setOnClickListener {
            // Pasar el modo actual como argumento al gráfico
            val bundle = Bundle().apply {
                putString("tipo_historial", modoActual)
            }
            findNavController().navigate(R.id.navigation_grafico_historial, bundle)
        }

        // Cargar el historial según el modo recibido
        if (modoActual == "menores") {
            selectMenores()
        } else {
            consultarHistorialAdultos()
        }

        return root
    }

    private fun setupRecyclerView() {
        adapter = HistorialAdapter(emptyList())
        binding.rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorial.adapter = adapter
    }

    private fun setupToggleButtons() {
        // Configurar estado inicial basado en el modo actual
        val esAdultos = (modoActual == "adultos")

        updateTextColors(isAdultosSelected = esAdultos)

        // Posicionar la píldora inmediatamente según el modo actual usando ViewTreeObserver
        binding.slidingPill.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.slidingPill.viewTreeObserver.removeOnGlobalLayoutListener(this)

                if (!esAdultos) {
                    // Si es menores, posicionar en la mitad derecha
                    val destinationX = binding.btnAdultos.width.toFloat()
                    binding.slidingPill.translationX = destinationX
                } else {
                    // Si es adultos, posicionar en 0
                    binding.slidingPill.translationX = 0f
                }
            }
        })

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

        modoActual = "adultos" // Actualizar modo actual
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

        modoActual = "menores" // Actualizar modo actual
        consultarHistorialMenores()
    }

    private fun updateTextColors(isAdultosSelected: Boolean) {
        if (isAdultosSelected) {
            // Adultos seleccionado: texto verde (sobre fondo blanco de la píldora)
            // Menores no seleccionado: texto blanco (sobre fondo verde del contenedor)
            binding.btnAdultos.setTextColor(resources.getColor(R.color.green_primary_dark, null))
            binding.btnMenores.setTextColor(resources.getColor(R.color.white, null))
        } else {
            // Adultos no seleccionado: texto blanco (sobre fondo verde del contenedor)
            // Menores seleccionado: texto verde (sobre fondo blanco de la píldora)
            binding.btnAdultos.setTextColor(resources.getColor(R.color.white, null))
            binding.btnMenores.setTextColor(resources.getColor(R.color.green_primary_dark, null))
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
            android.util.Log.e("HistorialFragment", "Error al consultar historial de adultos: ${e.message}", e)
            android.widget.Toast.makeText(context, getString(R.string.error_generico, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
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
            android.util.Log.e("HistorialFragment", "Error al consultar historial de menores: ${e.message}", e)
            android.widget.Toast.makeText(context, getString(R.string.error_generico, e.message ?: ""), android.widget.Toast.LENGTH_LONG).show()
            adapter.clearData()
            binding.tvHistorialVacio.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}