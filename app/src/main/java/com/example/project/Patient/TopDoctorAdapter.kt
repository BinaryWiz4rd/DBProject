package com.example.project.Patient

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.project.databinding.ViewholderTopDoctorBinding

/**
 * RecyclerView Adapter for displaying a list of top doctors.
 * Binds doctor data to the view and handles click events for doctor selection.
 */
class TopDoctorAdapter(val items: MutableList<DoctorsModel>) :
    RecyclerView.Adapter<TopDoctorAdapter.Viewholder>() {
    private var context: Context? = null

    class Viewholder(val binding: ViewholderTopDoctorBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding =
            ViewholderTopDoctorBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.binding.nameTxt.text = items[position].Name
        holder.binding.specialTxt.text = items[position].Special
        holder.binding.scoreTxt.text = items[position].Rating.toString()
        holder.binding.yearTxt.text = items[position].Expriense.toString() + " Year"

        Glide.with(holder.itemView.context)
            .load(items[position].Picture)
            .apply { RequestOptions().transform(CenterCrop()) }
            .into(holder.binding.img)

        holder.itemView.setOnClickListener {
            val intent=Intent(context, DetailActivity::class.java)
            intent.putExtra("object",items[position])
            context?.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}
