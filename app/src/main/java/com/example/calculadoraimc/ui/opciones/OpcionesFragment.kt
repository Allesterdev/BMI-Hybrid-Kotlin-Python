package com.example.calculadoraimc.ui.opciones

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentOpcionesBinding
import androidx.navigation.fragment.findNavController

class OpcionesFragment : Fragment() {

    private var _binding: FragmentOpcionesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOpcionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBorrarHistorialAdultos.setOnClickListener {
            mostrarDialogoConfirmacion(
                title = getString(R.string.confirmacion_borrar_titulo),
                message = getString(R.string.confirmacion_borrar_adultos),
                onConfirm = { borrarHistorialAdultos() }
            )
        }
        binding.btnBorrarHistorialMenores.setOnClickListener {
            mostrarDialogoConfirmacion(
                title = getString(R.string.confirmacion_borrar_titulo),
                message = getString(R.string.confirmacion_borrar_menores),
                onConfirm = { borrarHistorialMenores() }
            )
        }
        binding.btnBorrarTodosHistoriales.setOnClickListener {
            mostrarDialogoConfirmacion(
                title = getString(R.string.confirmacion_borrar_titulo),
                message = getString(R.string.confirmacion_borrar_todos),
                onConfirm = { borrarTodosHistoriales() }
            )
        }
        binding.btnAvisoLegal.setOnClickListener {
            findNavController().navigate(R.id.action_opciones_to_avisoLegal)
        }
        binding.btnAcercaDe.setOnClickListener {
            findNavController().navigate(R.id.action_opciones_to_acercaDe)
        }
    }

    private fun mostrarDialogoConfirmacion(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        val dialog = AlertDialog.Builder(requireContext(), R.style.AppAlertDialogTheme)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
