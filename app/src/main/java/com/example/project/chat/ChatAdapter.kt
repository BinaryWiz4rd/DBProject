package com.example.project.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project.R
import com.example.project.util.FirebaseStorageHelper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * An adapter for displaying chat messages in a RecyclerView.
 * It handles different view types for sent and received messages,
 * and supports text, image, and document message types.
 *
 * @property messages The list of messages to be displayed.
 */
class ChatAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val storageHelper = FirebaseStorageHelper()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    /**
     * Returns the list of messages currently held by the adapter.
     */
    fun getMessages(): MutableList<Message> = messages

    /**
     * Determines the view type for a message at a given position.
     * @return [VIEW_TYPE_SENT] if the message was sent by the current user,
     *         [VIEW_TYPE_RECEIVED] otherwise.
     */
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    /**
     * Creates a new [MessageViewHolder] for a message item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = if (viewType == VIEW_TYPE_SENT) {
            layoutInflater.inflate(R.layout.item_chat_sent, parent, false)
        } else {
            layoutInflater.inflate(R.layout.item_chat_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    /**
     * Binds the data of a message at a given position to the [MessageViewHolder].
     */
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    /**
     * Returns the total number of messages.
     */
    override fun getItemCount(): Int = messages.size

    /**
     * Adds a new message to the list and notifies the adapter.
     * @param message The message to be added.
     */
    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /**
     * Updates an existing message with file download information.
     * This is used after a file upload is complete.
     *
     * @param messageId The ID of the message to update.
     * @param downloadUrl The download URL of the file.
     * @param fileName The name of the file.
     * @param fileSize The size of the file.
     * @param mimeType The MIME type of the file.
     */
    fun updateMessage(messageId: String, downloadUrl: String, fileName: String, fileSize: Long, mimeType: String) {
        val position = messages.indexOfFirst { it.id == messageId }
        if (position != -1) {
            val message = messages[position]
            message.fileUrl = downloadUrl
            message.fileName = fileName
            message.fileSize = fileSize
            message.mimeType = mimeType
            notifyItemChanged(position)
        }
    }

    /**
     * ViewHolder for a single chat message.
     * @param itemView The view for the message item.
     */
    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        /**
         * Binds a message's data to the views in the ViewHolder.
         * It handles different message types and sets up click listeners for attachments.
         * @param message The message to bind.
         */
        fun bind(message: Message) {
            timestampTextView.text = message.timestamp?.let { formatter.format(it) } ?: ""
            
            when (message.messageType) {
                MessageType.TEXT -> {
                    messageTextView.text = message.text
                }
                
                MessageType.IMAGE -> {
                    messageTextView.text = if (!message.text.isNullOrEmpty()) {
                        "📸 ${message.text}"
                    } else {
                        "📸 Image: ${message.fileName ?: "photo.jpg"}"
                    }
                    
                    // Make clickable to open image
                    message.fileUrl?.let { url ->
                        messageTextView.setOnClickListener {
                            openFullScreenImage(itemView.context, url)
                        }
                    }
                }
                
                MessageType.DOCUMENT -> {
                    val fileName = message.fileName ?: "document"
                    val fileSize = message.fileSize?.let { storageHelper.formatFileSize(it) } ?: ""
                    messageTextView.text = "📄 $fileName${if (fileSize.isNotEmpty()) " ($fileSize)" else ""}"
                    
                    // Make clickable to open document
                    message.fileUrl?.let { url ->
                        messageTextView.setOnClickListener {
                            openDocument(itemView.context, url, fileName)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Opens an image in a full-screen viewer.
     * @param context The context.
     * @param imageUrl The URL of the image to open.
     */
    private fun openFullScreenImage(context: Context, imageUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(imageUrl), "image/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback - you could show a toast or custom image viewer
        }
    }
    
    /**
     * Opens a document using a system intent.
     * @param context The context.
     * @param documentUrl The URL of the document.
     * @param fileName The name of the document.
     */
    private fun openDocument(context: Context, documentUrl: String, fileName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(documentUrl), "application/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback - you could show a toast or download the file
        }
    }
    
    /**
     * Returns a drawable resource ID for a document icon based on its MIME type.
     * @param mimeType The MIME type of the document.
     * @return The resource ID of the icon.
     */
    private fun getDocumentIcon(mimeType: String?): Int {
        return when {
            mimeType?.contains("pdf") == true -> R.drawable.ic_pdf
            mimeType?.contains("word") == true -> R.drawable.ic_document
            mimeType?.contains("text") == true -> R.drawable.ic_document
            mimeType?.contains("image") == true -> R.drawable.ic_image
            else -> R.drawable.ic_document
        }
    }
} 