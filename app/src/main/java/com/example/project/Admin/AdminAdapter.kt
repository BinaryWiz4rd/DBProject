package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class AdminAdapter(private val adminList: List<Admin>) :
    RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.tvAdminEmail)
        val nameTextView: TextView = itemView.findViewById(R.id.tvAdminName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val currentAdmin = adminList[position]
        holder.emailTextView.text = currentAdmin.email
        holder.nameTextView.text = "${currentAdmin.firstName} ${currentAdmin.lastName}"
    }

    override fun getItemCount() = adminList.size
}