package com.example.calculadoraimc.ui.opciones

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.calculadoraimc.R

class AcercaDeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflado manual del layout para evitar errores de binding si no se ha generado aún
        return inflater.inflate(R.layout.fragment_acerca_de, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fabVolver = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_volver_acerca_de)
        fabVolver.setOnClickListener {
            findNavController().popBackStack()
        }

        // Botón dedicado para abrir LicencesActivity
        val btnVerLicencias = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_ver_licencias)
        btnVerLicencias.setOnClickListener {
            val intent = Intent(requireContext(), LicencesActivity::class.java)
            startActivity(intent)
        }
    }
}
