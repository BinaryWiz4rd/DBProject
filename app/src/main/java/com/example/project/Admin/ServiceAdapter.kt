package com.example.project.Admin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.example.project.R
import com.example.project.Service

/**
 * Adapter for displaying services in a ListView with edit and delete actions.
 */
class ServiceAdapter(
    context: Context,
    private val services: List<Service>,
    private val showActions: Boolean = true,
    private val onEditClick: ((Service) -> Unit)? = null,
    private val onDeleteClick: ((Service) -> Unit)? = null
) : ArrayAdapter<Service>(context, 0, services) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val service = getItem(position) ?: return View(context)
        
        // Check if an existing view is being reused, otherwise inflate the view
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_service, parent, false)
        
        // Lookup view for data population
        val nameTextView = view.findViewById<TextView>(R.id.serviceNameTextView)
        val priceTextView = view.findViewById<TextView>(R.id.servicePriceTextView)
        val durationTextView = view.findViewById<TextView>(R.id.serviceDurationTextView)
        val editButton = view.findViewById<ImageButton>(R.id.editServiceButton)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteServiceButton)
        
        // Populate the data into the template view using the data object
        nameTextView.text = service.name
        priceTextView.text = context.getString(R.string.price_format, service.price)
        durationTextView.text = context.getString(R.string.duration_format, service.duration_minutes)
        
        // Show or hide action buttons
        if (showActions) {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE
            
            // Set click listeners for action buttons
            editButton.setOnClickListener {
                onEditClick?.invoke(service)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(service)
            }
        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }
        
        return view
    }
}