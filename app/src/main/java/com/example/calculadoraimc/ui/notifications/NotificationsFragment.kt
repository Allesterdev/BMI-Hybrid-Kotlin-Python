package com.example.calculadoraimc.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calculadoraimc.databinding.FragmentNotificationsBinding
import com.chaquo.python.Python

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

        // Ocultar el TextView original para evitar conflictos
        binding.textNotifications.visibility = View.GONE

        setupRecyclerView()
        setupToggleButtons()
        
        // Cargar historial de adultos por defecto
        consultarHistorialAdultos()

        return root
    }

    private fun setupRecyclerView() {
        adapter = HistorialAdapter(emptyList())
        binding.rvHistorial.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorial.adapter = adapter
    }

    private fun setupToggleButtons() {
        // Seleccionar adultos por defecto
        binding.btnAdultos.isChecked = true
        
        binding.toggleHistorial.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.btnAdultos.id -> consultarHistorialAdultos()
                    binding.btnMenores.id -> consultarHistorialMenores()
                }
            }
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
            // Log del error específico para debugging
            android.util.Log.e("NotificationsFragment", "Error al consultar historial de adultos: ${e.message}", e)

            // En desarrollo: mostrar el error al usuario para identificar problemas
            // TODO: Remover este Toast en producción
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()

            // Mostrar lista vacía como fallback temporal
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

            // Log para debugging
            android.util.Log.d("HistorialMenores", "Cantidad de registros encontrados: ${lista.size}")

            val historial = lista.mapIndexed { index, item ->
                val map = item.asMap()

                // Log cada registro para debugging
                android.util.Log.d("HistorialMenores", "Registro $index: $map")

                // Función auxiliar para obtener valores de forma segura
                fun getDoubleValue(key: String): Double {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    val result = when {
                        value == null -> 0.0
                        value.toString() == "None" -> 0.0
                        else -> try {
                            value.toString().toDouble()
                        } catch (e: NumberFormatException) {
                            android.util.Log.w("HistorialMenores", "Error convirtiendo $key: ${value.toString()}")
                            0.0
                        }
                    }
                    android.util.Log.d("HistorialMenores", "$key: ${value?.toString()} -> $result")
                    return result
                }

                fun getIntValue(key: String): Int {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    val result = when {
                        value == null -> 0
                        value.toString() == "None" -> 0
                        else -> try {
                            value.toString().toInt()
                        } catch (e: NumberFormatException) {
                            android.util.Log.w("HistorialMenores", "Error convirtiendo $key: ${value.toString()}")
                            0
                        }
                    }
                    android.util.Log.d("HistorialMenores", "$key: ${value?.toString()} -> $result")
                    return result
                }

                fun getStringValue(key: String): String {
                    val pyKey = com.chaquo.python.PyObject.fromJava(key)
                    val value = map[pyKey]
                    val result = when {
                        value == null -> ""
                        value.toString() == "None" -> ""
                        else -> value.toString()
                    }
                    android.util.Log.d("HistorialMenores", "$key: ${value?.toString()} -> $result")
                    return result
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

            android.util.Log.d("HistorialMenores", "Historial procesado: ${historial.size} registros")

            adapter.updateDataMenores(historial)
            binding.tvHistorialVacio.visibility = if (historial.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            // Log del error específico para debugging
            android.util.Log.e("NotificationsFragment", "Error al consultar historial de menores: ${e.message}", e)

            // En desarrollo: mostrar el error al usuario para identificar problemas
            // TODO: Remover este Toast en producción
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()

            // Mostrar lista vacía como fallback temporal
            adapter.clearData()
            binding.tvHistorialVacio.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}