package com.example.calculadoraimc.ui.adultos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chaquo.python.Python
import com.example.calculadoraimc.BuildConfig
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentAdultosBinding
import com.example.calculadoraimc.utils.MeasurementUtils
import com.example.calculadoraimc.utils.MeasurementUtils.MeasurementSystem
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import android.view.inputmethod.InputMethodManager
import android.content.Context

class AdultosFragment : Fragment() {

    private var _binding: FragmentAdultosBinding? = null
    private val binding get() = _binding!!
    private lateinit var measurementSystem: MeasurementSystem

    // Variables para AdMob
    private var adLoader: AdLoader? = null
    private var nativeAd: NativeAd? = null
    private var nativeAdView: NativeAdView? = null

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

        // Configurar listeners
        setupClickListeners()

        return root
    }

    fun cargarAnuncios() {
        cargarAnuncioNativo()
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

                // Mostrar campo de altura métrico y ocultar campos imperiales
                binding.tilAltura.visibility = View.VISIBLE
                binding.layoutAlturaImperial.visibility = View.GONE
            }
            MeasurementSystem.IMPERIAL -> {
                // Sistema imperial (lb, in)
                binding.tilPeso.hint = getString(R.string.hint_peso_imperial)

                // Ocultar campo de altura métrico y mostrar campos imperiales
                binding.tilAltura.visibility = View.GONE
                binding.layoutAlturaImperial.visibility = View.VISIBLE
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

        try {
            // Convertir valores al sistema métrico si es necesario
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    val alturaText = binding.etAltura.text.toString()

                    if (pesoText.isEmpty() || alturaText.isEmpty()) {
                        Toast.makeText(context, getString(R.string.error_campos_vacios_adultos), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toDouble(), alturaText.toDouble())
                }
                MeasurementSystem.IMPERIAL -> {
                    val piesText = binding.etAlturaPies.text.toString()
                    val pulgadasText = binding.etAlturaPulgadas.text.toString()

                    if (pesoText.isEmpty() || (piesText.isEmpty() && pulgadasText.isEmpty())) {
                        Toast.makeText(context, getString(R.string.error_campos_vacios_adultos), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Convertir de libras a kg
                    val pesoLbs = pesoText.toDouble()
                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)

                    // Convertir pies y pulgadas a centímetros
                    val pies = if (piesText.isEmpty()) 0 else piesText.toInt()
                    val pulgadas = if (pulgadasText.isEmpty()) 0 else pulgadasText.toInt()

                    // Total de pulgadas = (pies * 12) + pulgadas
                    val totalPulgadas = (pies * 12) + pulgadas
                    val alturaCm = MeasurementUtils.inchesToCm(totalPulgadas.toDouble())

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
        try {
            // Convertir valores al sistema métrico para guardar
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    val pesoText = binding.etPeso.text.toString()
                    val alturaText = binding.etAltura.text.toString()

                    if (pesoText.isEmpty() || alturaText.isEmpty()) {
                        Toast.makeText(context, getString(R.string.error_calcular_primero_adultos), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toDouble(), alturaText.toDouble())
                }
                MeasurementSystem.IMPERIAL -> {
                    val pesoText = binding.etPeso.text.toString()
                    val piesText = binding.etAlturaPies.text.toString()
                    val pulgadasText = binding.etAlturaPulgadas.text.toString()

                    if (pesoText.isEmpty() || (piesText.isEmpty() && pulgadasText.isEmpty())) {
                        Toast.makeText(context, getString(R.string.error_calcular_primero_adultos), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Convertir de libras a kg
                    val pesoLbs = pesoText.toDouble()
                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)

                    // Convertir pies y pulgadas a centímetros
                    val pies = if (piesText.isEmpty()) 0 else piesText.toInt()
                    val pulgadas = if (pulgadasText.isEmpty()) 0 else pulgadasText.toInt()

                    // Total de pulgadas = (pies * 12) + pulgadas
                    val totalPulgadas = (pies * 12) + pulgadas
                    val alturaCm = MeasurementUtils.inchesToCm(totalPulgadas.toDouble())

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
            binding.etAlturaPies.text?.clear()
            binding.etAlturaPulgadas.text?.clear()
            binding.barraImc.clearIndicator()
            binding.cardResultado.visibility = View.GONE

            // Mostrar anuncio intersticial después de guardar el cálculo
            activity?.let {
                com.example.calculadoraimc.utils.AdManager.getInstance(requireContext()).showAdAfterSaving(it)
            }

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

    // Método para cargar anuncio nativo
    private fun cargarAnuncioNativo() {
        try {
            adLoader = AdLoader.Builder(requireContext(), BuildConfig.ADMOB_NATIVE_ADULTOS_ID)
                .forNativeAd { ad ->
                    try {
                        if (!isAdded) {
                            ad.destroy()
                            return@forNativeAd
                        }

                        nativeAd?.destroy()
                        nativeAd = ad

                        val inflater = LayoutInflater.from(requireContext())
                        nativeAdView = inflater.inflate(R.layout.layout_anuncio_nativo, binding.adContainer, false) as NativeAdView

                        mostrarAnuncioNativo()
                    } catch (e: Exception) {
                        android.util.Log.e("AdMob", "Error al procesar anuncio nativo: ${e.message}")
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        android.util.Log.e("AdMob", "Error al cargar anuncio: ${adError.message}")
                        try {
                            if (isAdded) {
                                binding.adContainer.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AdMob", "Error al ocultar contenedor: ${e.message}")
                        }
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .build()
                )
                .build()

            val adRequest = AdRequest.Builder().build()
            adLoader?.loadAd(adRequest)
        } catch (e: Exception) {
            android.util.Log.e("AdMob", "Error general en cargarAnuncioNativo: ${e.message}")
        }
    }

    // Método para mostrar el anuncio nativo
    private fun mostrarAnuncioNativo() {
        try {
            val adView = nativeAdView
            val ad = nativeAd

            if (!isAdded || adView == null || ad == null) {
                return
            }

            try {
                val headlineView = adView.findViewById<TextView>(R.id.ad_title)
                val bodyView = adView.findViewById<TextView>(R.id.ad_body)
                val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
                val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
                val starsView = adView.findViewById<RatingBar>(R.id.ad_rating)
                val mediaView = adView.findViewById<MediaView>(R.id.ad_media)

                headlineView.text = ad.headline
                adView.headlineView = headlineView

                ad.body?.let {
                    bodyView.visibility = View.VISIBLE
                    bodyView.text = it
                    adView.bodyView = bodyView
                } ?: run {
                    bodyView.visibility = View.INVISIBLE
                }

                ad.callToAction?.let {
                    callToActionView.visibility = View.VISIBLE
                    callToActionView.text = it
                    adView.callToActionView = callToActionView
                } ?: run {
                    callToActionView.visibility = View.INVISIBLE
                }

                ad.icon?.let {
                    iconView.visibility = View.VISIBLE
                    iconView.setImageDrawable(it.drawable)
                    adView.iconView = iconView
                } ?: run {
                    iconView.visibility = View.GONE
                }

                ad.starRating?.let {
                    starsView.visibility = View.VISIBLE
                    starsView.rating = it.toFloat()
                    adView.starRatingView = starsView
                } ?: run {
                    starsView.visibility = View.INVISIBLE
                }
                
                ad.mediaContent?.let {
                    mediaView.visibility = View.VISIBLE
                    mediaView.mediaContent = it
                    adView.mediaView = mediaView
                } ?: run {
                    mediaView.visibility = View.GONE
                }

                adView.setNativeAd(ad)

                if (isAdded && _binding != null) {
                    binding.adContainer.removeAllViews()
                    binding.adContainer.addView(adView)
                    binding.adContainer.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                android.util.Log.e("AdMob", "Error al configurar vistas del anuncio nativo: ${e.message}")
                if (isAdded && _binding != null) {
                    binding.adContainer.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdMob", "Error general en mostrarAnuncioNativo: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nativeAd?.destroy()
        nativeAd = null
        _binding = null
    }
}
