package com.example.project.doctor.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.Service

/**
 * `ServiceAdapter` is a `RecyclerView.Adapter` for displaying a list of `Service` objects.
 * It supports an optional "Add New Service" item.
 *
 * @param context The context from which the adapter is created.
 * @param initialServices The initial list of services to display.
 * @param showAddItem A boolean indicating whether to show the "Add New Service" item.
 * @param onItemClick A lambda function invoked when a service item is clicked.
 * @param onAddItemClick A lambda function invoked when the "Add New Service" item is clicked.
 */
class ServiceAdapter(
    private val context: Context,
    initialServices: List<Service>,
    private val showAddItem: Boolean = false,
    private val onItemClick: (Service) -> Unit,
    private val onAddItemClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var services: List<Service> = ArrayList(initialServices)

    /**
     * Companion object holding view type constants.
     */
    companion object {
        private const val VIEW_TYPE_SERVICE = 0
        private const val VIEW_TYPE_ADD = 1
    }

    /**
     * `ServiceViewHolder` represents a single service item view in the `RecyclerView`.
     * @param itemView The root view of a single service item.
     */
    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceNameTextView: TextView = itemView.findViewById(R.id.serviceNameTextView)
        private val servicePriceTextView: TextView = itemView.findViewById(R.id.servicePriceTextView)
        private val serviceDurationTextView: TextView = itemView.findViewById(R.id.serviceDurationTextView)

        /**
         * Binds a `Service` object to the `TextView` elements within the ViewHolder.
         * @param service The `Service` object to bind.
         */
        fun bind(service: Service) {
            serviceNameTextView.text = service.name
            servicePriceTextView.text = context.getString(R.string.price_format, service.price)
            serviceDurationTextView.text = context.getString(R.string.duration_format, service.duration_minutes)
            itemView.setOnClickListener { onItemClick(service) }
        }
    }

    /**
     * `AddServiceViewHolder` represents the "Add New Service" item view in the `RecyclerView`.
     * It handles the click event for this special item.
     * @param itemView The root view of the "Add New Service" item.
     */
    inner class AddServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onAddItemClick() }
        }
    }

    /**
     * Returns the view type of the item at `position` for the purposes of view recycling.
     * @param position The position to query.
     * @return An integer representing the type of `View` to be created for the position.
     */
    override fun getItemViewType(position: Int): Int {
        return if (showAddItem && position == services.size) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_SERVICE
        }
    }

    /**
     * Called when `RecyclerView` needs a new `ViewHolder` of the given `viewType`.
     * @param parent The `ViewGroup` into which the new `View` will be added.
     * @param viewType The view type of the new `View`.
     * @return A new `RecyclerView.ViewHolder` that holds a `View` of the given `viewType`.
     */
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

    /**
     * Called by `RecyclerView` to display the data at the specified `position`.
     * @param holder The `ViewHolder` which should be updated to represent the contents of the item at the given `position`.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ServiceViewHolder) {
            if (position < services.size) {
                holder.bind(services[position])
            }
        } else if (holder is AddServiceViewHolder) {
            // nic nie ma tuuu
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items.
     */
    override fun getItemCount(): Int {
        return services.size + (if (showAddItem) 1 else 0)
    }

    /**
     * Updates the list of services and notifies the adapter to refresh the `RecyclerView`.
     * @param newServices The new list of services to display.
     */
    fun updateServices(newServices: List<Service>) {
        this.services = ArrayList(newServices)
        notifyDataSetChanged()
    }
}