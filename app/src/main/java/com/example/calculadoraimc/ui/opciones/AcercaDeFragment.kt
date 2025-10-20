package com.example.calculadoraimc.ui.opciones

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.calculadoraimc.BuildConfig
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

        // Configurar el texto "Acerca de" con la versión dinámica desde BuildConfig
        setupAcercaDeText(view)

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

    private fun setupAcercaDeText(view: View) {
        val tvAcercaDe = view.findViewById<TextView>(R.id.tv_acerca_de_texto)

        // Construir el texto con la versión dinámica desde BuildConfig usando strings del XML
        val acercaDeTexto = """
            ${getString(R.string.acerca_de_nombre_app)}<br/><br/>
            <b>${getString(R.string.acerca_de_version)}</b> ${BuildConfig.VERSION_NAME}<br/><br/>
            <b>${getString(R.string.acerca_de_desarrollador)}</b> ${getString(R.string.acerca_de_desarrollador_nombre)}<br/><br/>
            ${getString(R.string.acerca_de_descripcion)}<br/><br/>
            ${getString(R.string.acerca_de_contacto)}
        """.trimIndent()

        // Aplicar el texto con formato HTML
        tvAcercaDe.text = Html.fromHtml(acercaDeTexto, Html.FROM_HTML_MODE_LEGACY)
    }
}
