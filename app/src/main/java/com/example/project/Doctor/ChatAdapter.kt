package com.example.project.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.project.doctor.ChatMessage
import com.example.project.doctor.MessageType
import com.example.project.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val VIEW_TYPE_SENT_TEXT = 1
private const val VIEW_TYPE_RECEIVED_TEXT = 2
private const val VIEW_TYPE_SENT_IMAGE = 3
private const val VIEW_TYPE_RECEIVED_IMAGE = 4
private const val VIEW_TYPE_SENT_DOCUMENT = 5
private const val VIEW_TYPE_RECEIVED_DOCUMENT = 6

/**
 * An adapter for displaying chat messages using a [ListAdapter] for efficient updates.
 *
 * @property currentUserId The ID of the currently logged-in user.
 */
class ChatAdapter(private val currentUserId: String) :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Determines the view type for a message at a given position based on its type and sender.
     */
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSentByUser = message.senderId == currentUserId

        return when (message.messageType) {
            MessageType.TEXT -> if (isSentByUser) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
            MessageType.IMAGE -> if (isSentByUser) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
            MessageType.DOCUMENT -> if (isSentByUser) VIEW_TYPE_SENT_DOCUMENT else VIEW_TYPE_RECEIVED_DOCUMENT
        }
    }

    /**
     * Creates a new [RecyclerView.ViewHolder] for a message item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentTextViewHolder(
                inflater.inflate(R.layout.activity_list_chat_item_sent_text, parent, false)
            )
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedTextViewHolder(
                inflater.inflate(R.layout.activity_list_chat_item_received_text, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    /**
     * Binds the data of a message at a given position to the [RecyclerView.ViewHolder].
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val formattedTime = timeFormatter.format(Date(message.timestamp))

        when (holder) {
            is SentTextViewHolder -> holder.bind(message, formattedTime)
            is ReceivedTextViewHolder -> holder.bind(message, formattedTime)
        }
    }

    /**
     * ViewHolder for a sent text message.
     */
    inner class SentTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)

        /**
         * Binds a sent text message's data to the views.
         * @param message The message to bind.
         * @param time The formatted timestamp.
         */
        fun bind(message: ChatMessage, time: String) {
            contentTextView.text = message.textContent
            timestampTextView.text = time
        }
    }

    /**
     * ViewHolder for a received text message.
     */
    inner class ReceivedTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)

        /**
         * Binds a received text message's data to the views.
         * @param message The message to bind.
         * @param time The formatted timestamp.
         */
        fun bind(message: ChatMessage, time: String) {
            contentTextView.text = message.textContent
            timestampTextView.text = time
        }
    }

    /**
     * A [DiffUtil.ItemCallback] for calculating the difference between two [ChatMessage] lists.
     */
    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        /**
         * Checks if two items represent the same object.
         */
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Checks if the contents of two items are the same.
         */
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}