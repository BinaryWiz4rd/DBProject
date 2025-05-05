package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class AdminAdapter(
    private var adminList: List<Admin>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) :
    RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.tvAdminEmail)
        val nameTextView: TextView = itemView.findViewById(R.id.tvAdminName)
        val approveButton: Button? = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button? = itemView.findViewById(R.id.buttonReject)

        fun bind(admin: Admin, onApproveReject: (String, String, Boolean) -> Unit) {
            emailTextView.text = admin.email
            nameTextView.text = "${admin.firstName} ${admin.lastName}"

            if (admin.add == true) {
                approveButton?.visibility = View.VISIBLE
                rejectButton?.visibility = View.VISIBLE
            } else {
                approveButton?.visibility = View.GONE
                rejectButton?.visibility = View.GONE
            }

            approveButton?.setOnClickListener {
                onApproveReject(admin.uid, "add", true)
            }
            rejectButton?.setOnClickListener {
                onApproveReject(admin.uid, "add", false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.bind(adminList[position], onApproveReject)
    }

    override fun getItemCount() = adminList.size

    fun updateList(newAdminList: List<Admin>) {
        adminList = newAdminList
        notifyDataSetChanged()
    }
}