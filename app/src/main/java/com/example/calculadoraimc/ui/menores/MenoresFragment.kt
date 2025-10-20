package com.example.calculadoraimc.ui.menores

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.calculadoraimc.BuildConfig
import com.example.calculadoraimc.R
import com.example.calculadoraimc.databinding.FragmentMenoresBinding
import com.chaquo.python.Python
import com.example.calculadoraimc.utils.MeasurementUtils
import com.example.calculadoraimc.utils.MeasurementUtils.MeasurementSystem
import com.example.calculadoraimc.utils.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

class MenoresFragment : Fragment() {

    private var _binding: FragmentMenoresBinding? = null
    private val binding get() = _binding!!
    private lateinit var measurementSystem: MeasurementSystem

    // Variables para AdMob nativo
    private var adLoader: AdLoader? = null
    private var nativeAd: NativeAd? = null
    private var nativeAdView: NativeAdView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenoresBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Determinar sistema de medición según localización
        measurementSystem = MeasurementUtils.getPreferredSystem(requireContext())

        // Configurar etiquetas según sistema de medición
        setupMeasurementLabels()

        // Configurar eventos
        setupClickListeners()
        setupTextWatchers()

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

    private fun showSoftKeyboard(view: View) {
        if (view.requestFocus()) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupClickListeners() {
        binding.btnCalcular.setOnClickListener {
            calcularPercentil()
        }

        binding.btnGuardar.setOnClickListener {
            guardarMedicion()
        }

        // Botón para mostrar/ocultar información sobre percentiles
        binding.btnToggleInfo.setOnClickListener {
            toggleInfoPercentiles()
        }
    }

    private fun toggleInfoPercentiles() {
        val infoTextView = binding.tvInfoPercentiles
        val toggleButton = binding.btnToggleInfo

        if (infoTextView.visibility == View.GONE) {
            // Mostrar información
            infoTextView.visibility = View.VISIBLE
            toggleButton.text = getString(R.string.btn_ocultar_info)
            toggleButton.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            // Ocultar información
            infoTextView.visibility = View.GONE
            toggleButton.text = getString(R.string.btn_mostrar_info)
            toggleButton.setIconResource(android.R.drawable.ic_dialog_info)
        }
    }

    private fun setupTextWatchers() {
        // Implementación recomendada: campo único, solo dígitos, máscara según locale
        val dateOrder = detectDateOrder()
        val watcher = object : TextWatcher {
            var selfChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (selfChange) return
                val original = s?.toString() ?: ""
                val digits = original.replace(Regex("[^0-9]"), "")

                // Limitar a 8 dígitos (DDMMYYYY / MMDDYYYY / YYYYMMDD)
                val maxDigits = 8
                val limited = if (digits.length > maxDigits) digits.substring(0, maxDigits) else digits

                val formatted = formatDigitsAsDate(limited, dateOrder)

                try {
                    selfChange = true
                    binding.etFechaNacimiento.setText(formatted)
                    binding.etFechaNacimiento.setSelection(formatted.length)
                } finally {
                    selfChange = false
                }

                // Validar cuando tengamos la longitud esperada
                val expectedDigits = 8
                if (limited.length == expectedDigits) {
                    val pattern = patternWithSeparators(dateOrder)
                    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                    sdf.isLenient = false
                    try {
                        sdf.parse(formatted)
                        binding.tilFechaNacimiento.error = null
                        hideKeyboard(binding.etFechaNacimiento)
                        binding.etFechaNacimiento.clearFocus()
                    } catch (e: Exception) {
                        binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
                    }
                } else {
                    binding.tilFechaNacimiento.error = null
                }
            }
        }

        binding.etFechaNacimiento.addTextChangedListener(watcher)
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Detecta orden preferido: "DMY", "MDY" o "YMD"
    private fun detectDateOrder(): String {
        return try {
            val best = android.text.format.DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "yMd")
            val lower = best.lowercase(java.util.Locale.getDefault())
            val dIndex = lower.indexOf('d')
            val mIndex = lower.indexOf('M')
            val yIndex = lower.indexOf('y')
            if (dIndex >= 0 && mIndex >= 0 && dIndex < mIndex) {
                "DMY"
            } else if (mIndex >= 0 && dIndex >= 0 && mIndex < dIndex) {
                "MDY"
            } else if (yIndex >= 0 && yIndex < Math.min(dIndex.takeIf { it>=0 } ?: Int.MAX_VALUE, mIndex.takeIf { it>=0 } ?: Int.MAX_VALUE)) {
                "YMD"
            } else {
                "DMY"
            }
        } catch (_: Exception) {
            "DMY"
        }
    }

    private fun formatDigitsAsDate(digits: String, order: String): String {
        when (order) {
            "DMY" -> {
                val sb = StringBuilder()
                if (digits.length >= 2) sb.append(digits.substring(0, 2)) else sb.append(digits)
                if (digits.length > 2) sb.append('/').append(if (digits.length >= 4) digits.substring(2, 4) else digits.substring(2))
                if (digits.length > 4) sb.append('/').append(digits.substring(4))
                return sb.toString()
            }
            "MDY" -> {
                val sb = StringBuilder()
                if (digits.length >= 2) sb.append(digits.substring(0, 2)) else sb.append(digits)
                if (digits.length > 2) sb.append('/').append(if (digits.length >= 4) digits.substring(2, 4) else digits.substring(2))
                if (digits.length > 4) sb.append('/').append(digits.substring(4))
                return sb.toString()
            }
            "YMD" -> {
                val sb = StringBuilder()
                if (digits.length >= 4) sb.append(digits.substring(0, 4)) else sb.append(digits)
                if (digits.length > 4) sb.append('/').append(if (digits.length >= 6) digits.substring(4, 6) else digits.substring(4))
                if (digits.length > 6) sb.append('/').append(digits.substring(6))
                return sb.toString()
            }
        }
        return digits
    }

    private fun patternWithSeparators(order: String): String {
        return when (order) {
            "DMY" -> "dd/MM/yyyy"
            "MDY" -> "MM/dd/yyyy"
            "YMD" -> "yyyy/MM/dd"
            else -> "dd/MM/yyyy"
        }
    }

    private fun calcularPercentil() {
        val pesoText = binding.etPeso.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.text.toString()

        // Validar que los campos requeridos no estén vacíos
        if (pesoText.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_campos_vacios_menores), Toast.LENGTH_SHORT).show()
            return
        }

        // Validar que tengamos datos de altura según el sistema de medición
        when (measurementSystem) {
            MeasurementSystem.METRIC -> {
                val alturaText = binding.etAltura.text.toString()
                if (alturaText.isEmpty()) {
                    Toast.makeText(context, getString(R.string.error_campos_vacios_menores), Toast.LENGTH_SHORT).show()
                    return
                }
            }
            MeasurementSystem.IMPERIAL -> {
                val piesText = binding.etAlturaPies.text.toString()
                val pulgadasText = binding.etAlturaPulgadas.text.toString()

                if (piesText.isEmpty() && pulgadasText.isEmpty()) {
                    Toast.makeText(context, getString(R.string.error_campos_vacios_menores), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        // Validar fecha localmente: formato, año de 4 dígitos, no futura y rango 5-19 años
        val order = detectDateOrder()
        val pattern = patternWithSeparators(order)
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        sdf.isLenient = false
        val dobDate = try {
            // Aceptamos la fecha formateada con separadores (dd/MM/yyyy etc.)
            val parsed = sdf.parse(fechaNacimiento)
            parsed
        } catch (e: Exception) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        // Comprobar fecha futura
        val today = java.util.Calendar.getInstance()
        val dobCal = java.util.Calendar.getInstance().apply { time = dobDate }
        if (dobCal.after(today)) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        // Calcular edad en años
        var ageYears = today.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
        val todayMonth = today.get(java.util.Calendar.MONTH)
        val dobMonth = dobCal.get(java.util.Calendar.MONTH)
        val todayDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        val dobDay = dobCal.get(java.util.Calendar.DAY_OF_MONTH)
        if (todayMonth < dobMonth || (todayMonth == dobMonth && todayDay < dobDay)) {
            ageYears -= 1
        }

        if (ageYears < 5 || ageYears > 19) {
            binding.tilFechaNacimiento.error = getString(R.string.error_edad_rango)
            Toast.makeText(context, getString(R.string.error_edad_rango), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Para Python siempre necesitamos usar "Masculino" o "Femenino" (valores fijos en español)
            val sexoPython = if (binding.rbMasculino.isChecked) "Masculino" else "Femenino"

            // Convertir valores al sistema métrico si es necesario
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    val alturaText = binding.etAltura.text.toString()
                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toDouble(), alturaText.toDouble())
                }
                MeasurementSystem.IMPERIAL -> {
                    // Convertir de libras a kg
                    val pesoLbs = pesoText.toDouble()
                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)

                    // Usar los campos separados de pies y pulgadas
                    val piesText = binding.etAlturaPies.text.toString()
                    val pulgadasText = binding.etAlturaPulgadas.text.toString()

                    // Convertir pies y pulgadas a centímetros
                    val pies = if (piesText.isEmpty()) 0 else piesText.toInt()
                    val pulgadas = if (pulgadasText.isEmpty()) 0 else pulgadasText.toInt()

                    // Total de pulgadas = (pies * 12) + pulgadas
                    val totalPulgadas = (pies * 12) + pulgadas
                    val alturaCm = MeasurementUtils.inchesToCm(totalPulgadas.toDouble())

                    Pair(pesoKg, alturaCm)
                }
            }

            // Debug: Mostrar qué datos estamos enviando
            android.util.Log.d("MenoresFragment", "Enviando a Python:")
            android.util.Log.d("MenoresFragment", "  Sexo Python: $sexoPython")
            android.util.Log.d("MenoresFragment", "  Fecha: '$fechaNacimiento'")
            android.util.Log.d("MenoresFragment", "  Peso (convertido a kg): '$pesoKg'")
            android.util.Log.d("MenoresFragment", "  Altura (convertida a cm): '$alturaCm'")

            // Llamar a la función de Python que usa fecha de nacimiento
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Python maneja la validación y conversión de fecha automáticamente
            // Enviamos los valores ya convertidos a sistema métrico
            // IMPORTANTE: Sexo debe ser "Masculino" o "Femenino" para Python
            val resultado = funcionesModule.callAttr(
                "calcular_imc_menor_por_fecha",
                sexoPython,
                fechaNacimiento,
                pesoKg.toString(),
                alturaCm.toString()
            )

            // Debug: Mostrar qué devuelve Python
            android.util.Log.d("MenoresFragment", "Resultado Python completo: $resultado")

            // Verificar si hay error en el resultado
            if (resultado.containsKey("error")) {
                val error = resultado["error"]?.toString() ?: getString(R.string.error_desconocido)
                android.util.Log.e("MenoresFragment", "Error de Python: $error")
                Toast.makeText(context, getString(R.string.error_percentiles, error), Toast.LENGTH_LONG).show()
                return
            }

            // Extraer valores usando el método correcto para objetos Python en Chaquopy
            val imc = resultado.callAttr("get", "imc")?.toDouble() ?: 0.0
            val percentil = resultado.callAttr("get", "percentil")?.toDouble() ?: 0.0
            // La función Python interpretar_percentil ahora devuelve una clave de recurso.
            val interpretacionKey = resultado.callAttr("get", "interpretacion")?.toString() ?: ""
            val edadAnios = resultado.callAttr("get", "edad_años")?.toDouble() ?: 0.0

            // Debug: Mostrar valores extraídos
            android.util.Log.d("MenoresFragment", "Valores extraídos:")
            android.util.Log.d("MenoresFragment", "  IMC: $imc")
            android.util.Log.d("MenoresFragment", "  Percentil: $percentil")
            android.util.Log.d("MenoresFragment", "  Interpretación clave: '$interpretacionKey'")
            android.util.Log.d("MenoresFragment", "  Edad años: $edadAnios")

            // Mostrar resultados (incluyendo edad calculada)
            binding.tvImcValor.text = getString(R.string.formato_imc, imc)
            binding.tvPercentil.text = getString(R.string.formato_percentil, percentil)
            val interpretacionTexto = when (interpretacionKey) {
                "interpretacion_bajo_peso" -> getString(R.string.interpretacion_bajo_peso)
                "interpretacion_peso_saludable" -> getString(R.string.interpretacion_peso_saludable)
                "interpretacion_sobrepeso" -> getString(R.string.interpretacion_sobrepeso)
                "interpretacion_obesidad" -> getString(R.string.interpretacion_obesidad)
                "" -> getString(R.string.sin_interpretacion)
                else -> {
                    // Python devolvió una clave inesperada: registrar y usar el texto por defecto
                    android.util.Log.w("MenoresFragment", "Clave de interpretación desconocida recibida de Python: $interpretacionKey")
                    getString(R.string.sin_interpretacion)
                }
            }

            // Mostrar tarjeta de resultados
            binding.tvInterpretacion.text = getString(R.string.interpretacion_con_edad, interpretacionTexto, edadAnios)
            binding.barraPercentilMenores.setPercentil(percentil)
            binding.barraPercentilMenores.visibility = View.VISIBLE
            binding.cardResultado.visibility = View.VISIBLE

            // Auto-scroll para mostrar los resultados y el botón de guardar
            binding.root.post {
                binding.root.smoothScrollTo(0, binding.cardResultado.bottom)
            }
        } catch (e: Exception) {
            android.util.Log.e("MenoresFragment", "Excepción en calcularPercentil", e)
            Toast.makeText(context, getString(R.string.error_calculo, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarMedicion() {
        val pesoText = binding.etPeso.text.toString()
        val fechaNacimiento = binding.etFechaNacimiento.text.toString()

        // Validar que los campos requeridos no estén vacíos según el sistema de medición
        if (pesoText.isEmpty() || fechaNacimiento.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_calcular_primero_menores), Toast.LENGTH_SHORT).show()
            return
        }

        // Validar que tengamos datos de altura según el sistema de medición
        when (measurementSystem) {
            MeasurementSystem.METRIC -> {
                val alturaText = binding.etAltura.text.toString()
                if (alturaText.isEmpty()) {
                    Toast.makeText(context, getString(R.string.error_calcular_primero_menores), Toast.LENGTH_SHORT).show()
                    return
                }
            }
            MeasurementSystem.IMPERIAL -> {
                val piesText = binding.etAlturaPies.text.toString()
                val pulgadasText = binding.etAlturaPulgadas.text.toString()

                if (piesText.isEmpty() && pulgadasText.isEmpty()) {
                    Toast.makeText(context, getString(R.string.error_calcular_primero_menores), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        // Validar fecha de nuevo antes de guardar
        val orderSave = detectDateOrder()
        val patternSave = patternWithSeparators(orderSave)
        val sdfSave = java.text.SimpleDateFormat(patternSave, java.util.Locale.getDefault())
        sdfSave.isLenient = false
        val dobDateSave = try {
            sdfSave.parse(fechaNacimiento)
        } catch (e: Exception) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        val todaySave = java.util.Calendar.getInstance()
        val dobCalSave = java.util.Calendar.getInstance().apply { time = dobDateSave }
        if (dobCalSave.after(todaySave)) {
            binding.tilFechaNacimiento.error = getString(R.string.error_fecha_invalida)
            Toast.makeText(context, getString(R.string.error_fecha_invalida), Toast.LENGTH_SHORT).show()
            return
        }

        var ageYearsSave = todaySave.get(java.util.Calendar.YEAR) - dobCalSave.get(java.util.Calendar.YEAR)
        if (todaySave.get(java.util.Calendar.MONTH) < dobCalSave.get(java.util.Calendar.MONTH) ||
            (todaySave.get(java.util.Calendar.MONTH) == dobCalSave.get(java.util.Calendar.MONTH) &&
                    todaySave.get(java.util.Calendar.DAY_OF_MONTH) < dobCalSave.get(java.util.Calendar.DAY_OF_MONTH))) {
            ageYearsSave -= 1
        }

        if (ageYearsSave < 5 || ageYearsSave > 19) {
            binding.tilFechaNacimiento.error = getString(R.string.error_edad_rango)
            Toast.makeText(context, getString(R.string.error_edad_rango), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Usar el valor localizado del recurso (Male/Female) en lugar del valor fijo en español
            val sexoLocalizado = if (binding.rbMasculino.isChecked)
                getString(R.string.sexo_masculino)
            else
                getString(R.string.sexo_femenino)

            // Convertir valores al sistema métrico si es necesario
            val (pesoKg, alturaCm) = when (measurementSystem) {
                MeasurementSystem.METRIC -> {
                    val alturaText = binding.etAltura.text.toString()
                    // Ya están en kg y cm, no necesita conversión
                    Pair(pesoText.toDouble(), alturaText.toDouble())
                }
                MeasurementSystem.IMPERIAL -> {
                    // Convertir de libras a kg
                    val pesoLbs = pesoText.toDouble()
                    val pesoKg = MeasurementUtils.lbsToKg(pesoLbs)

                    // Usar los campos separados de pies y pulgadas
                    val piesText = binding.etAlturaPies.text.toString()
                    val pulgadasText = binding.etAlturaPulgadas.text.toString()

                    // Convertir pies y pulgadas a centímetros
                    val pies = if (piesText.isEmpty()) 0 else piesText.toInt()
                    val pulgadas = if (pulgadasText.isEmpty()) 0 else pulgadasText.toInt()

                    // Total de pulgadas = (pies * 12) + pulgadas
                    val totalPulgadas = (pies * 12) + pulgadas
                    val alturaCm = MeasurementUtils.inchesToCm(totalPulgadas.toDouble())

                    Pair(pesoKg, alturaCm)
                }
            }

            // Python maneja todas las conversiones automáticamente
            val python = Python.getInstance()
            val funcionesModule = python.getModule("funciones_imc_android")

            // Calculamos nuevamente usando la nueva función por fecha y enviando los valores ya convertidos
            // Usamos sexoPython para el cálculo (requiere "Masculino" o "Femenino")
            val sexoPython = if (binding.rbMasculino.isChecked) "Masculino" else "Femenino"
            val resultado = funcionesModule.callAttr("calcular_imc_menor_por_fecha", sexoPython, fechaNacimiento, pesoKg.toString(), alturaCm.toString())

            if (resultado.containsKey("error") != true) {
                // Usar la sintaxis correcta para extraer valores de objetos Python
                val imc = resultado.callAttr("get", "imc")?.toDouble() ?: 0.0
                val percentil = resultado.callAttr("get", "percentil")?.toDouble() ?: 0.0
                val edadMeses = resultado.callAttr("get", "edad_meses")?.toInt() ?: 0

                // Log para verificar que los valores son correctos antes de guardar
                android.util.Log.d("MenoresFragment", "Guardando medición:")
                android.util.Log.d("MenoresFragment", "  Peso (kg): $pesoKg")
                android.util.Log.d("MenoresFragment", "  Altura (cm): $alturaCm")
                android.util.Log.d("MenoresFragment", "  IMC: $imc")
                android.util.Log.d("MenoresFragment", "  Sexo localizado: $sexoLocalizado")
                android.util.Log.d("MenoresFragment", "  Edad meses: $edadMeses")
                android.util.Log.d("MenoresFragment", "  Percentil: $percentil")

                // Guardamos el valor localizado (Male/Female) para el historial
                funcionesModule.callAttr("guardar_medicion", pesoKg, alturaCm, imc, sexoLocalizado, edadMeses, percentil)

                Toast.makeText(context, getString(R.string.exito_guardado), Toast.LENGTH_SHORT).show()

                // Limpiar campos
                binding.etPeso.text?.clear()
                binding.etAltura.text?.clear()
                binding.etAlturaPies.text?.clear()
                binding.etAlturaPulgadas.text?.clear()
                binding.etFechaNacimiento.text?.clear()
                binding.rbMasculino.isChecked = true
                binding.cardResultado.visibility = View.GONE
            }

                // Mostrar anuncio intersticial después de guardar el cálculo
                activity?.let {
                    AdManager.getInstance(requireContext()).showAdAfterSaving(it)
                }


        } catch (e: Exception) {
            android.util.Log.e("MenoresFragment", "Error al guardar medición", e)
            Toast.makeText(context, getString(R.string.error_guardar, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    // Método para cargar el anuncio nativo
    private fun cargarAnuncioNativo() {
        if (adLoader?.isLoading == true) {
            return
        }
        try {
            adLoader = AdLoader.Builder(requireContext(), BuildConfig.ADMOB_NATIVE_MENORES_ID)
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
