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

// Definicje typów widoków
private const val VIEW_TYPE_SENT_TEXT = 1
private const val VIEW_TYPE_RECEIVED_TEXT = 2
private const val VIEW_TYPE_SENT_IMAGE = 3
private const val VIEW_TYPE_RECEIVED_IMAGE = 4
private const val VIEW_TYPE_SENT_DOCUMENT = 5
private const val VIEW_TYPE_RECEIVED_DOCUMENT = 6

class ChatAdapter(private val currentUserId: String) :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSentByUser = message.senderId == currentUserId

        return when (message.messageType) {
            MessageType.TEXT -> if (isSentByUser) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
            MessageType.IMAGE -> if (isSentByUser) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
            MessageType.DOCUMENT -> if (isSentByUser) VIEW_TYPE_SENT_DOCUMENT else VIEW_TYPE_RECEIVED_DOCUMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentTextViewHolder(
                inflater.inflate(R.layout.activity_list_chat_item_sent_text, parent, false)
            )
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedTextViewHolder(
                inflater.inflate(R.layout.activity_list_chat_item_received_text, parent, false)
            )
            // TODO: Dodać tworzenie ViewHolderów dla obrazów i dokumentów
            // VIEW_TYPE_SENT_IMAGE -> SentImageViewHolder(...)
            // VIEW_TYPE_RECEIVED_IMAGE -> ReceivedImageViewHolder(...)
            // VIEW_TYPE_SENT_DOCUMENT -> SentDocumentViewHolder(...)
            // VIEW_TYPE_RECEIVED_DOCUMENT -> ReceivedDocumentViewHolder(...)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val formattedTime = timeFormatter.format(Date(message.timestamp))

        when (holder) {
            is SentTextViewHolder -> holder.bind(message, formattedTime)
            is ReceivedTextViewHolder -> holder.bind(message, formattedTime)
            // TODO: Dodać bindowanie dla obrazów i dokumentów
            // is SentImageViewHolder -> holder.bind(message, formattedTime)
            // is ReceivedImageViewHolder -> holder.bind(message, formattedTime)
            // is SentDocumentViewHolder -> holder.bind(message, formattedTime)
            // is ReceivedDocumentViewHolder -> holder.bind(message, formattedTime)
        }
    }

    // --- ViewHoldery ---

    inner class SentTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)

        fun bind(message: ChatMessage, time: String) {
            contentTextView.text = message.textContent
            timestampTextView.text = time
            // TODO: Obsługa statusu wysyłania (np. zmiana wyglądu)
        }
    }

    inner class ReceivedTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentTextView: TextView = itemView.findViewById(R.id.textViewMessageContent)
        private val timestampTextView: TextView = itemView.findViewById(R.id.textViewTimestamp)

        fun bind(message: ChatMessage, time: String) {
            contentTextView.text = message.textContent
            timestampTextView.text = time
        }
    }

    // TODO: Dodać klasy ViewHolderów dla obrazów (z ImageView) i dokumentów (np. z ikoną pliku, nazwą, rozmiarem)
    // np. inner class SentImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { ... }


    // --- DiffUtil Callback ---
    class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id // Porównaj po ID
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem // Porównaj całą zawartość
        }
    }
}