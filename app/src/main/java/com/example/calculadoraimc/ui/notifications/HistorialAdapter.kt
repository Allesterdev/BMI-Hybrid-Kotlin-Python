package com.example.calculadoraimc.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calculadoraimc.R

// Modelo para una medición de adulto
data class HistorialAdulto(
    val peso: Double,
    val altura: Double,
    val imc: Double,
    val fecha: String
)

// Modelo para una medición de menor
data class HistorialMenor(
    val peso: Double,
    val altura: Double,
    val imc: Double,
    val fecha: String,
    val sexo: String,
    val edadMeses: Int,
    val percentil: Double
)

// Clase sellada para representar diferentes tipos de historial
sealed class ItemHistorial {
    data class AdultoItem(val data: HistorialAdulto) : ItemHistorial()
    data class MenorItem(val data: HistorialMenor) : ItemHistorial()
}

class HistorialAdapter(private var items: List<ItemHistorial>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADULTO = 1
        private const val TYPE_MENOR = 2
    }

    class HistorialAdultoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeso: TextView = view.findViewById(R.id.tv_item_peso)
        val tvAltura: TextView = view.findViewById(R.id.tv_item_altura)
        val tvImc: TextView = view.findViewById(R.id.tv_item_imc)
        val tvFecha: TextView = view.findViewById(R.id.tv_item_fecha)
    }

    class HistorialMenorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPeso: TextView = view.findViewById(R.id.tv_item_peso_menor)
        val tvAltura: TextView = view.findViewById(R.id.tv_item_altura_menor)
        val tvImc: TextView = view.findViewById(R.id.tv_item_imc_menor)
        val tvFecha: TextView = view.findViewById(R.id.tv_item_fecha_menor)
        val tvSexo: TextView = view.findViewById(R.id.tv_item_sexo)
        val tvEdad: TextView = view.findViewById(R.id.tv_item_edad)
        val tvPercentil: TextView = view.findViewById(R.id.tv_item_percentil)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ItemHistorial.AdultoItem -> TYPE_ADULTO
            is ItemHistorial.MenorItem -> TYPE_MENOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ADULTO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historial_adulto, parent, false)
                HistorialAdultoViewHolder(view)
            }
            TYPE_MENOR -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historial_menor, parent, false)
                HistorialMenorViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo de view desconocido: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ItemHistorial.AdultoItem -> {
                val adultHolder = holder as HistorialAdultoViewHolder
                val data = item.data
                adultHolder.tvPeso.text = holder.itemView.context.getString(R.string.historial_peso, data.peso)
                adultHolder.tvAltura.text = holder.itemView.context.getString(R.string.historial_altura, data.altura)
                adultHolder.tvImc.text = holder.itemView.context.getString(R.string.historial_imc, data.imc)
                adultHolder.tvFecha.text = holder.itemView.context.getString(R.string.historial_fecha, data.fecha)
            }
            is ItemHistorial.MenorItem -> {
                val menorHolder = holder as HistorialMenorViewHolder
                val data = item.data
                menorHolder.tvPeso.text = holder.itemView.context.getString(R.string.historial_peso, data.peso)
                menorHolder.tvAltura.text = holder.itemView.context.getString(R.string.historial_altura, data.altura)
                menorHolder.tvImc.text = holder.itemView.context.getString(R.string.historial_imc, data.imc)
                menorHolder.tvFecha.text = holder.itemView.context.getString(R.string.historial_fecha, data.fecha)
                menorHolder.tvSexo.text = holder.itemView.context.getString(R.string.historial_sexo, data.sexo)
                menorHolder.tvEdad.text = holder.itemView.context.getString(R.string.historial_edad_meses, data.edadMeses)
                menorHolder.tvPercentil.text = holder.itemView.context.getString(R.string.historial_percentil, data.percentil)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateDataAdultos(newItems: List<HistorialAdulto>) {
        items = newItems.map { ItemHistorial.AdultoItem(it) }
        notifyDataSetChanged()
    }

    fun updateDataMenores(newItems: List<HistorialMenor>) {
        items = newItems.map { ItemHistorial.MenorItem(it) }
        notifyDataSetChanged()
    }

    fun clearData() {
        items = emptyList()
        notifyDataSetChanged()
    }
}
