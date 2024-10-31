package io.github.takusan23.dougaundroid.processor.tool

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreTool {

    /** 端末の動画フォルダに保存する */
    suspend fun saveToVideoFolder(
        context: Context,
        file: File
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val contentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.MIME_TYPE to "video/mp4",
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MOVIES}/DougaUnDroid"
            )
        } else {
            contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to file.name,
                MediaStore.MediaColumns.MIME_TYPE to "video/mp4"
            )
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext
        // コピーする
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Uri から動画タイトルを取得する
     *
     * @param uri uri
     * @return [InputVideoInfo]
     */
    suspend fun getInputVideoInfo(
        context: Context,
        uri: Uri
    ): InputVideoInfo? = withContext(Dispatchers.IO) {
        val columns = arrayOf(MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION)
        context.contentResolver.query(
            uri,
            columns,
            null,
            null,
            null
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            cursor.moveToFirst()

            InputVideoInfo(
                uri = uri,
                name = cursor.getString(nameColumn),
                videoDurationMs = cursor.getLong(durationColumn)
            )
        }
    }

}