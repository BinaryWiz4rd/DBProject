package com.example.project.Patient

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.project.databinding.ViewholderTopDoctor2Binding

/**
 * RecyclerView Adapter for displaying a list of top doctors in a custom layout.
 * Binds doctor data to the view and handles image loading with Glide.
 */
class TopDoctorAdapter2(val items: MutableList<DoctorsModel>) :
    RecyclerView.Adapter<TopDoctorAdapter2.Viewholder>() {
    private var context: Context? = null

    class Viewholder(val binding: ViewholderTopDoctor2Binding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    /**
     * Inflates the item view for each doctor and returns a ViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder instance.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding =
            ViewholderTopDoctor2Binding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    /**
     * Binds doctor data to the view holder at the given position.
     *
     * @param holder The ViewHolder to bind data to.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.binding.nameTxt.text = items[position].Name
        holder.binding.specialTxt.text = items[position].Special
        holder.binding.scoreTxt.text = items[position].Rating.toString()
    holder.binding.ratingBar.rating=items[position].Rating.toFloat()
        holder.binding.scoreTxt.text=items[position].Rating.toString()
        holder.binding.degreeTxt.text="Professional Doctor"

        Glide.with(holder.itemView.context)
            .load(items[position].Picture)
            .apply { RequestOptions().transform(CenterCrop()) }
            .into(holder.binding.img)

        holder.binding.makeBtn.setOnClickListener {
            val intent=Intent(context, DetailActivity::class.java)
            intent.putExtra("object",items[position])
            context?.startActivity(intent)
        }
    }

    /**
     * Returns the total number of top doctor items.
     *
     * @return The size of the items list.
     */
    override fun getItemCount(): Int = items.size
}
