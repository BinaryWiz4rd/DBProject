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

/**
 * Helper class for handling file uploads and downloads with Firebase Storage.
 * Provides methods for uploading files, retrieving metadata, and managing chat file storage.
 */
class FirebaseStorageHelper {
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    companion object {
        const val CHAT_FILES_PATH = "chat_files"
        const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }

    /**
     * Uploads a file to Firebase Storage.
     *
     * @param context The context of the application.
     * @param fileUri The URI of the file to upload.
     * @param storagePath The path in Firebase Storage where the file will be uploaded.
     * @return Task<Void> representing the asynchronous operation.
     */
    fun uploadFile(context: Context, fileUri: Uri, storagePath: String): Task<Void> {
        val storageReference = FirebaseStorage.getInstance().reference.child(storagePath)
        return storageReference.putFile(fileUri).continueWith { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: Exception("File upload failed")
            }
            null
        }
    }

    /**
     * Uploads a file to Firebase Storage with progress tracking and callbacks.
     *
     * @param context Context for content resolver.
     * @param fileUri URI of the file to upload.
     * @param chatId Chat ID for organizing file storage path.
     * @param senderId ID of the sender uploading the file.
     * @param onProgress Callback invoked with upload progress percentage.
     * @param onSuccess Callback invoked when upload succeeds with download URL, file name, file size, and MIME type.
     * @param onFailure Callback invoked when upload fails with an exception.
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
     * Deletes a file from Firebase Storage.
     *
     * @param fileUrl The URL of the file to delete.
     * @return Task<Void> representing the delete operation.
     */
    fun deleteFile(fileUrl: String): Task<Void> {
        return storage.getReferenceFromUrl(fileUrl).delete()
    }

    /**
     * Retrieves the file name from a URI.
     *
     * @param context Context for content resolver.
     * @param uri URI of the file.
     * @return File name as a String, or null if not found.
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
     * Retrieves the file size from a URI.
     *
     * @param context Context for content resolver.
     * @param uri URI of the file.
     * @return File size in bytes.
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
     * Retrieves the MIME type from a file URI.
     *
     * @param context Context for content resolver.
     * @param uri URI of the file.
     * @return MIME type as a String, or null if unknown.
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
     * Extracts the file extension from a file name.
     *
     * @param fileName The name of the file.
     * @return File extension without the dot, or 'bin' if none.
     */
    private fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            "bin"
        }
    }

    /**
     * Checks if a given MIME type corresponds to an image.
     *
     * @param mimeType The MIME type to check.
     * @return True if the MIME type starts with 'image/', false otherwise.
     */
    fun isImage(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    /**
     * Checks if a given MIME type corresponds to a document.
     *
     * @param mimeType The MIME type to check.
     * @return True if the MIME type indicates a document, false otherwise.
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
     * Formats a file size in bytes to a human-readable string.
     *
     * @param bytes File size in bytes.
     * @return Formatted file size string (e.g., '1.2 MB').
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
