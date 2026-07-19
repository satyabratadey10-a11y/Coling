package com.example.coling.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.example.coling.ui.screens.MediaMetadata
import com.arthenica.ffmpegkit.FFprobeKit

/**
 * Extract display file name from a content:// Uri.
 */
fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = cursor.getString(idx) ?: "Unknown"
                }
            }
        }
    } catch (e: Exception) {
        // Fallback: use last path segment
        name = uri.lastPathSegment ?: "Unknown"
    }
    return name
}

/**
 * Get file size from a content:// Uri.
 */
fun getFileSize(context: Context, uri: Uri): Long {
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) {
                    size = cursor.getLong(idx)
                }
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return size
}

/**
 * Open a ParcelFileDescriptor for the given Uri.
 * Caller is responsible for closing it.
 */
fun openPfd(context: Context, uri: Uri): ParcelFileDescriptor? {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")
    } catch (e: Exception) {
        null
    }
}

/**
 * Probe media metadata from a content:// Uri using FFprobe via a ParcelFileDescriptor pipe,
 * falling back to MediaMetadataRetriever on error.
 */
fun probeMediaFromUri(context: Context, uri: Uri, fileName: String): MediaMetadata {
    var pfd: ParcelFileDescriptor? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) throw Exception("Failed to open ParcelFileDescriptor")
        val fd = pfd.fd
        val pipePath = "pipe:$fd"

        val session = FFprobeKit.getMediaInformation(pipePath)
        val info = session.mediaInformation
        if (info != null) {
            val videoStream = info.streams.firstOrNull { it.type == "video" }
            val audioStream = info.streams.firstOrNull { it.type == "audio" }

            val durationMs = info.duration?.toDoubleOrNull() ?: 0.0
            val durationStr = if (durationMs > 0.0) "${"%.2f".format(durationMs)}s" else "N/A"

            val fileSize = getFileSize(context, uri)
            val sizeStr = if (fileSize > 0) "${"%.1f".format(fileSize / (1024.0 * 1024.0))}\u00A0MB" else "N/A"

            val resolution = if (videoStream != null) "${videoStream.width}x${videoStream.height}" else "N/A"

            MediaMetadata(
                fileName = fileName,
                filePath = uri.toString(),
                format = info.format ?: "unknown",
                duration = durationStr,
                size = sizeStr,
                videoCodec = videoStream?.codec ?: "none",
                audioCodec = audioStream?.codec ?: "none",
                resolution = resolution
            )
        } else {
            throw Exception("Failed to get media info from FFprobe")
        }
    } catch (e: Exception) {
        probeMediaFallback(context, uri, fileName)
    } finally {
        try { pfd?.close() } catch (_: Exception) {}
    }
}

private fun probeMediaFallback(context: Context, uri: Uri, fileName: String): MediaMetadata {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)

        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.let { "${"%.2f".format(it / 1000.0)}s" } ?: "N/A"

        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "0"
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "0"
        val resolution = if (width != "0" && height != "0") "${width}x${height}" else "N/A"

        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "unknown"
        val fileSize = getFileSize(context, uri)
        val sizeStr = if (fileSize > 0) "${"%.1f".format(fileSize / (1024.0 * 1024.0))}\u00A0MB" else "N/A"

        MediaMetadata(
            fileName = fileName,
            filePath = uri.toString(),
            format = mimeType,
            duration = duration,
            size = sizeStr,
            videoCodec = mimeType.substringAfter("/", "unknown"),
            audioCodec = "unknown",
            resolution = resolution
        )
    } catch (e: Exception) {
        MediaMetadata(
            fileName = fileName,
            filePath = uri.toString(),
            format = "unknown",
            duration = "N/A",
            size = "N/A",
            videoCodec = "unknown",
            audioCodec = "unknown",
            resolution = "N/A"
        )
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}

/**
 * Extract a thumbnail/frame from a video Uri.
 * Returns null if extraction fails.
 */
fun extractThumbnail(context: Context, uri: Uri, timeUs: Long = 0): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (e: Exception) {
        null
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}
