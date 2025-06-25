package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

/**
 * Adapter for the admin list RecyclerView.
 *
 * @property adminList The list of admins to display.
 * @property onApproveReject Callback function to handle approve/reject actions.
 */
class AdminAdapter(
    private var adminList: List<Admin>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) :
    RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    /**
     * ViewHolder for the admin list item.
     *
     * @param itemView The view for the admin list item.
     */
    class AdminViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailTextView: TextView = itemView.findViewById(R.id.tvAdminEmail)
        val nameTextView: TextView = itemView.findViewById(R.id.tvAdminName)
        val approveButton: Button? = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button? = itemView.findViewById(R.id.buttonReject)

        /**
         * Binds the admin data to the view.
         *
         * @param admin The admin to bind.
         * @param onApproveReject Callback function to handle approve/reject actions.
         */
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

    /**
     * Creates a new ViewHolder for the admin list item.
     *
     * @param parent The parent view group.
     * @param viewType The view type.
     * @return The new ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin, parent, false)
        return AdminViewHolder(itemView)
    }

    /**
     * Binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder to bind.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.bind(adminList[position], onApproveReject)
    }

    /**
     * Returns the number of items in the list.
     *
     * @return The number of items in the list.
     */
    override fun getItemCount() = adminList.size

    /**
     * Updates the list of admins.
     *
     * @param newAdminList The new list of admins.
     */
    fun updateList(newAdminList: List<Admin>) {
        adminList = newAdminList
        notifyDataSetChanged()
    }
}