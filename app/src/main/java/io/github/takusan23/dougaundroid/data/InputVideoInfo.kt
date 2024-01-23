package io.github.takusan23.dougaundroid.data

import android.net.Uri

/**
 * 選択した動画の情報
 */
data class InputVideoInfo(
    val uri: Uri,
    val name: String,
    val videoDurationMs: Long
)