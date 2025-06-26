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
        val service = getItem(position) ?: return View(context)

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_service, parent, false)

        val nameTextView = view.findViewById<TextView>(R.id.serviceNameTextView)
        val priceTextView = view.findViewById<TextView>(R.id.servicePriceTextView)
        val durationTextView = view.findViewById<TextView>(R.id.serviceDurationTextView)
        val editButton = view.findViewById<ImageButton>(R.id.editServiceButton)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteServiceButton)

        nameTextView.text = service.name
        priceTextView.text = context.getString(R.string.price_format, service.price)
        durationTextView.text = context.getString(R.string.duration_format, service.duration_minutes)

        if (showActions) {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

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