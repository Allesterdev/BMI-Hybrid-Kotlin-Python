package com.example.calculadoraimc.ui.opciones

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentOpcionesBinding
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.calculadoraimc.utils.MeasurementUtils
import com.example.calculadoraimc.utils.MeasurementUtils.MeasurementSystem
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.ump.UserMessagingPlatform

class OpcionesFragment : Fragment() {

    private var _binding: FragmentOpcionesBinding? = null
    private val binding get() = _binding!!

    // Referencias directas a vistas
    private lateinit var switchSistemaMedida: SwitchMaterial
    private lateinit var tvSistemaActualValor: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_opciones, container, false)
        _binding = FragmentOpcionesBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar referencias directas a vistas
        try {
            switchSistemaMedida = view.findViewById(R.id.switch_sistema_medida)
            tvSistemaActualValor = view.findViewById(R.id.tv_sistema_actual_valor)

            setupMeasurementSystemSwitch()
        } catch (e: Exception) {
            android.util.Log.e("OpcionesFragment", "Error al inicializar vistas: ${e.message}", e)
        }

        setupClickListeners(view)
    }

    private fun setupMeasurementSystemSwitch() {
        try {
            // Obtener el sistema actual de medición
            val currentSystem = MeasurementUtils.getPreferredSystem(requireContext())

            // Inicializar el switch según el sistema actual
            // true = Imperial, false = Métrico
            switchSistemaMedida.isChecked = currentSystem == MeasurementSystem.IMPERIAL

            // Actualizar el texto que muestra el sistema actual
            updateCurrentSystemText(currentSystem)

            // Configurar el listener para cambios en el switch
            switchSistemaMedida.setOnCheckedChangeListener { _, isChecked ->
                val newSystem = if (isChecked) MeasurementSystem.IMPERIAL else MeasurementSystem.METRIC

                // Guardar la nueva preferencia
                MeasurementUtils.setPreferredSystem(requireContext(), newSystem)

                // Actualizar el texto que muestra el sistema actual
                updateCurrentSystemText(newSystem)

                // Mostrar un mensaje de confirmación del cambio
                val systemName = getString(
                    if (isChecked) R.string.sistema_imperial else R.string.sistema_metrico
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.cambio_sistema_confirmado, systemName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            // Registrar la excepción para diagnóstico
            android.util.Log.e("OpcionesFragment", "Error al configurar el switch: ${e.message}", e)
        }
    }

    private fun updateCurrentSystemText(system: MeasurementSystem) {
        try {
            val systemText = when (system) {
                MeasurementSystem.METRIC -> getString(R.string.sistema_metrico)
                MeasurementSystem.IMPERIAL -> getString(R.string.sistema_imperial)
            }
            tvSistemaActualValor.text = systemText
        } catch (e: Exception) {
            // Registrar la excepción para diagnóstico
            android.util.Log.e("OpcionesFragment", "Error al actualizar el texto: ${e.message}", e)
        }
    }

    private fun setupClickListeners(view: View) {
        try {
            view.findViewById<MaterialButton>(R.id.btn_borrar_historial_adultos).setOnClickListener {
                mostrarDialogoConfirmacion(
                    title = getString(R.string.confirmacion_borrar_titulo),
                    message = getString(R.string.confirmacion_borrar_adultos),
                    onConfirm = { borrarHistorialAdultos() }
                )
            }

            view.findViewById<MaterialButton>(R.id.btn_borrar_historial_menores).setOnClickListener {
                mostrarDialogoConfirmacion(
                    title = getString(R.string.confirmacion_borrar_titulo),
                    message = getString(R.string.confirmacion_borrar_menores),
                    onConfirm = { borrarHistorialMenores() }
                )
            }

            view.findViewById<MaterialButton>(R.id.btn_borrar_todos_historiales).setOnClickListener {
                mostrarDialogoConfirmacion(
                    title = getString(R.string.confirmacion_borrar_titulo),
                    message = getString(R.string.confirmacion_borrar_todos),
                    onConfirm = { borrarTodosHistoriales() }
                )
            }

            view.findViewById<MaterialButton>(R.id.btn_aviso_legal).setOnClickListener {
                findNavController().navigate(R.id.action_opciones_to_avisoLegal)
            }

            view.findViewById<MaterialButton>(R.id.btn_politicas_privacidad).setOnClickListener {
                abrirPoliticasPrivacidad()
            }

            view.findViewById<MaterialButton>(R.id.btn_opciones_privacidad).setOnClickListener {
                UserMessagingPlatform.showPrivacyOptionsForm(requireActivity()) { formError: com.google.android.ump.FormError? ->
                    if (formError != null) {
                        android.util.Log.e("UMP", "Error al mostrar el formulario de opciones de privacidad: ${formError.message}")
                        Toast.makeText(requireContext(), "Error al abrir opciones de privacidad", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            view.findViewById<MaterialButton>(R.id.btn_acerca_de).setOnClickListener {
                findNavController().navigate(R.id.action_opciones_to_acercaDe)
            }
        } catch (e: Exception) {
            android.util.Log.e("OpcionesFragment", "Error al configurar click listeners: ${e.message}", e)
        }
    }

    private fun mostrarDialogoConfirmacion(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AppAlertDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.btn_confirmar)) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .create()

        dialog.show()
    }

    private fun borrarHistorialAdultos() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(requireContext()))
            }

            val python = Python.getInstance()
            val utilidadesModule = python.getModule("utilidades")

            utilidadesModule.callAttr("borrar_historial_adultos")

            Toast.makeText(
                requireContext(),
                getString(R.string.exito_borrar_adultos),
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_borrar_historial, e.message ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun borrarHistorialMenores() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(requireContext()))
            }

            val python = Python.getInstance()
            val utilidadesModule = python.getModule("utilidades")

            utilidadesModule.callAttr("borrar_historial_menores")

            Toast.makeText(
                requireContext(),
                getString(R.string.exito_borrar_menores),
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_borrar_historial, e.message ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun borrarTodosHistoriales() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(requireContext()))
            }

            val python = Python.getInstance()
            val utilidadesModule = python.getModule("utilidades")

            utilidadesModule.callAttr("borrar_todos_historiales")

            Toast.makeText(
                requireContext(),
                getString(R.string.exito_borrar_todos),
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_borrar_historiales, e.message ?: ""),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirPoliticasPrivacidad() {
        try {
            val url = "https://allesterdev.github.io/privacy-policy/"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al abrir las políticas de privacidad",
                Toast.LENGTH_SHORT
            ).show()
            android.util.Log.e("OpcionesFragment", "Error al abrir URL: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
