package com.example.project.doctor.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.Service

class ServiceAdapter(
    private val context: Context,
    initialServices: List<Service>,
    private val showAddItem: Boolean = false,
    private val onItemClick: (Service) -> Unit,
    private val onAddItemClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var services: List<Service> = ArrayList(initialServices)

    companion object {
        private const val VIEW_TYPE_SERVICE = 0
        private const val VIEW_TYPE_ADD = 1
    }

    // ViewHolder for regular service items
    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.serviceNameTextView)
        private val servicePriceTextView: TextView = itemView.findViewById(R.id.servicePriceTextView)
        private val serviceDurationTextView: TextView = itemView.findViewById(R.id.serviceDurationTextView)

        fun bind(service: Service) {
            serviceNameTextView.text = service.name
            servicePriceTextView.text = context.getString(R.string.price_format, service.price)
            serviceDurationTextView.text = context.getString(R.string.duration_format, service.duration_minutes)
            itemView.setOnClickListener { onItemClick(service) }
        }
    }

    // ViewHolder for the "Add New Service" item
    inner class AddServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onAddItemClick() }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showAddItem && position == services.size) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_SERVICE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == VIEW_TYPE_SERVICE) {
            val view = inflater.inflate(R.layout.item_service_doctor, parent, false)
            ServiceViewHolder(view)
        } else { // VIEW_TYPE_ADD
            val view = inflater.inflate(R.layout.item_add_service, parent, false)
            AddServiceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ServiceViewHolder) {
            if (position < services.size) { // Ensure position is valid for services list
                holder.bind(services[position])
            }
        } else if (holder is AddServiceViewHolder) {
            // No specific data to bind, click listener is set in ViewHolder init
        }
    }

    override fun getItemCount(): Int {
        return services.size + (if (showAddItem) 1 else 0)
    }

    fun updateServices(newServices: List<Service>) {
        this.services = ArrayList(newServices) // Create a new list to avoid reference issues
        notifyDataSetChanged()
    }
}