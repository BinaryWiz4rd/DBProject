package com.example.project.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.util.*

class FirebaseStorageHelper {
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    companion object {
        const val CHAT_FILES_PATH = "chat_files"
        const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }

    /**
     * Upload a file to Firebase Storage
     * @param context Context for content resolver
     * @param fileUri URI of the file to upload
     * @param chatId Chat ID for organizing files
     * @param senderId ID of the sender
     * @param onProgress Callback for upload progress (percentage)
     * @param onSuccess Callback when upload succeeds with download URL
     * @param onFailure Callback when upload fails
     */
    fun uploadFile(
        context: Context,
        fileUri: Uri,
        chatId: String,
        senderId: String,
        onProgress: (Int) -> Unit = {},
        onSuccess: (String, String, Long, String) -> Unit, // downloadUrl, fileName, fileSize, mimeType
        onFailure: (Exception) -> Unit
    ) {
        try {
            val contentResolver = context.contentResolver
            
            // Get file info
            val fileName = getFileName(context, fileUri) ?: "unknown_file_${System.currentTimeMillis()}"
            val fileSize = getFileSize(context, fileUri)
            val mimeType = getMimeType(context, fileUri) ?: "application/octet-stream"
            
            // Validate file size
            if (fileSize > MAX_FILE_SIZE) {
                onFailure(Exception("File size exceeds maximum allowed size of ${MAX_FILE_SIZE / (1024 * 1024)}MB"))
                return
            }
            
            // Create unique file path
            val timestamp = System.currentTimeMillis()
            val fileExtension = getFileExtension(fileName)
            val uniqueFileName = "${senderId}_${timestamp}_${UUID.randomUUID()}.$fileExtension"
            val filePath = "$CHAT_FILES_PATH/$chatId/$uniqueFileName"
            
            val fileRef = storageRef.child(filePath)
            
            // Create metadata
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("originalFileName", fileName)
                .setCustomMetadata("senderId", senderId)
                .setCustomMetadata("chatId", chatId)
                .setCustomMetadata("uploadTimestamp", timestamp.toString())
                .build()
            
            // Start upload
            val uploadTask = fileRef.putFile(fileUri, metadata)
            
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }.addOnSuccessListener {
                // Get download URL
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString(), fileName, fileSize, mimeType)
                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
            
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    /**
     * Delete a file from Firebase Storage
     */
    fun deleteFile(fileUrl: String): Task<Void> {
        return storage.getReferenceFromUrl(fileUrl).delete()
    }

    /**
     * Get file name from URI
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = it.getString(displayNameIndex)
                    }
                }
            }
        }
        
        if (fileName == null) {
            fileName = uri.path?.let { path ->
                path.substring(path.lastIndexOf('/') + 1)
            }
        }
        
        return fileName
    }

    /**
     * Get file size from URI
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        var fileSize = 0L
        
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        fileSize = it.getLong(sizeIndex)
                    }
                }
            }
        }
        
        return fileSize
    }

    /**
     * Get MIME type from URI
     */
    private fun getMimeType(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }

    /**
     * Get file extension from filename
     */
    private fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            "bin"
        }
    }

    /**
     * Check if file is an image
     */
    fun isImage(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    /**
     * Check if file is a document
     */
    fun isDocument(mimeType: String?): Boolean {
        return when {
            mimeType?.startsWith("application/pdf") == true -> true
            mimeType?.startsWith("application/msword") == true -> true
            mimeType?.startsWith("application/vnd.openxmlformats-officedocument") == true -> true
            mimeType?.startsWith("text/") == true -> true
            else -> false
        }
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
