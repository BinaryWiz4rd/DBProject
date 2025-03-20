package com.example.project.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.model.Specialization

class SpecializationAdapter(
    private var specializations: MutableList<Specialization>,
    private val isAdmin: Boolean,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<SpecializationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.specializationName)
        val deleteButton: Button? = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_specialization, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val specialization = specializations[position]
        holder.name.text = specialization.name
        holder.deleteButton?.visibility = if (isAdmin) View.VISIBLE else View.GONE
        holder.deleteButton?.setOnClickListener { onDelete(specialization.id) }
    }

    override fun getItemCount() = specializations.size

    fun updateData(newList: List<Specialization>) {
        specializations.clear()
        specializations.addAll(newList)
        notifyDataSetChanged()
    }
}
