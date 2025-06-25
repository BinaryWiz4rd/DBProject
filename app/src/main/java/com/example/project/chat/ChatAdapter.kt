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

class ChatAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val storageHelper = FirebaseStorageHelper()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun getMessages(): MutableList<Message> = messages

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = if (viewType == VIEW_TYPE_SENT) {
            layoutInflater.inflate(R.layout.item_chat_sent, parent, false)
        } else {
            layoutInflater.inflate(R.layout.item_chat_received, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

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

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

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