package com.example.project.Patient

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project.databinding.ViewholderCategoryBinding

/**
 * RecyclerView adapter for displaying a list of categories.
 *
 * This adapter is responsible for binding [CategoryModel] objects to the views in a RecyclerView,
 * typically displaying a category name and an associated image.
 *
 * @param items The mutable list of [CategoryModel] objects to be displayed.
 */
class CategoryAdapter(val items: MutableList<CategoryModel>) :
    RecyclerView.Adapter<CategoryAdapter.Viewholder>() {
    private lateinit var context: Context

    /**
     * ViewHolder for individual category items in the RecyclerView.
     *
     * @param binding The ViewBinding object for a single category list item.
     */
    inner class Viewholder(val binding: ViewholderCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Called when RecyclerView needs a new [Viewholder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [Viewholder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        context = parent.context
        val binding = ViewholderCategoryBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * This method updates the contents of the [itemView] to reflect the item at the given
     * position. It sets the category name and loads the category image using Glide.
     *
     * @param holder The [Viewholder] which should be updated to represent the contents
     * of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        val item = items[position]
        holder.binding.titleTxt.text = item.Name

        Glide.with(context)
            .load(item.Picture)
            .into(holder.binding.img)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = items.size
}