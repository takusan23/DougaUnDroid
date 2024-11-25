package io.github.takusan23.dougaundroid.processor.tool

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File

object MediaExtractorTool {

    enum class Track(val mimeTypePrefix: String) {
        VIDEO("video/"),
        AUDIO("audio/")
    }

    /**
     * トラックの[MediaFormat]を取り出す
     *
     * @param context [Context]
     * @param track 音声 or 映像
     * @param uri PhotoPicker や SAF の Uri
     * @return [MediaFormat]
     */
    fun getTrackMediaFormat(
        context: Context,
        uri: Uri,
        track: Track
    ): MediaFormat? {
        val mediaExtractor = MediaExtractor().apply {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                setDataSource(it.fileDescriptor)
            }
        }
        val mediaFormat = (0 until mediaExtractor.trackCount)
            .map { index -> mediaExtractor.getTrackFormat(index) }
            .firstOrNull { mediaFormat -> mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith(track.mimeTypePrefix) == true }
        return mediaFormat
    }

    /**
     * [MediaExtractor]を作る
     *
     * @param file ファイル
     * @param track 音声 or 映像
     * @return [MediaExtractor]と選択したトラックの[MediaFormat]。[Track.AUDIO]を指定したのに音声トラックがない場合は null
     */
    fun createMediaExtractor(
        file: File,
        track: Track
    ): Pair<MediaExtractor, MediaFormat>? {
        val mediaExtractor = MediaExtractor().apply {
            setDataSource(file.path)
        }
        val (index, mediaFormat) = mediaExtractor.getTrackMediaFormat(track) ?: return null
        mediaExtractor.selectTrack(index)
        // Extractor / MediaFormat を返す
        return mediaExtractor to mediaFormat
    }

    private fun MediaExtractor.getTrackMediaFormat(track: Track): Pair<Int, MediaFormat>? {
        // トラックを選択する（映像・音声どっち？）
        val trackIndex = (0 until this.trackCount)
            .map { this.getTrackFormat(it) }
            .indexOfFirst { it.getString(MediaFormat.KEY_MIME)?.startsWith(track.mimeTypePrefix) == true }

        // -1 の場合は存在しないため null
        if (trackIndex == -1) return null

        val mediaFormat = this.getTrackFormat(trackIndex)
        // 位置と MediaFormat
        return trackIndex to mediaFormat
    }

}