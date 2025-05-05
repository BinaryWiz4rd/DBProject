package com.example.project.Admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R

class PatientAdapter(
    private var patientList: List<Patient>,
    private val onApproveReject: (String, String, Boolean) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewEmail: TextView = itemView.findViewById(R.id.textViewEmail)
        val approveButton: Button? = itemView.findViewById(R.id.buttonApprove)
        val rejectButton: Button? = itemView.findViewById(R.id.buttonReject)

        fun bind(patient: Patient, onApproveReject: (String, String, Boolean) -> Unit) {
            textViewName.text = "${patient.firstName} ${patient.lastName}"
            textViewEmail.text = patient.email

            if (patient.add == true) {
                approveButton?.visibility = View.VISIBLE
                rejectButton?.visibility = View.VISIBLE
            } else {
                approveButton?.visibility = View.GONE
                rejectButton?.visibility = View.GONE
            }

            approveButton?.setOnClickListener {
                onApproveReject(patient.uid, "add", true)
            }
            rejectButton?.setOnClickListener {
                onApproveReject(patient.uid, "add", false)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patientList[position], onApproveReject)
    }

    override fun getItemCount(): Int {
        return patientList.size
    }

    fun updateList(newPatientList: List<Patient>) {
        patientList = newPatientList
        notifyDataSetChanged()
    }
}