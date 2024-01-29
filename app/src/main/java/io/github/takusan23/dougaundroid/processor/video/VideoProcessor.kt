package io.github.takusan23.dougaundroid.processor.video

import android.content.Context
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 映像を逆にする */
object VideoProcessor {

    /** 映像を逆にする */
    suspend fun start(
        context: Context,
        inFile: Uri,
        outFile: File,
        onProgressUpdateMs: (Long) -> Unit
    ) = withContext(Dispatchers.Default) {
        // 元データからエンコーダーを起動するパラメーターを取り出す
        // MediaExtractor だとなんかフレームレートとかビットレートは取れない？ MediaMetadataRetriever にしてみた
        val inputVideoMediaMetadataRetriever = MediaMetadataRetriever().apply { setDataSource(context, inFile) }
        val bitRate = inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 3_000_000
        val (_, videoHeight, videoWidth) = inputVideoMediaMetadataRetriever.extractVideoSize()
        val durationMs = inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()!!
        val frameRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toIntOrNull() ?: 30
        } else {
            30
        }

        // 映像を逆にする処理
        // Canvas から動画を作るやつを使う
        // https://takusan.negitoro.dev/posts/android_canvas_to_video/
        // Canvas で描画しろって毎フレーム呼ばれるので、そこに入力動画のフレームを Bitmap で描画する
        val paint = Paint()
        CanvasVideoProcessor.start(
            outFile = outFile,
            bitRate = bitRate,
            frameRate = frameRate,
            outputVideoWidth = videoWidth,
            outputVideoHeight = videoHeight,
            onCanvasDrawRequest = { currentPositionMs ->
                onProgressUpdateMs(currentPositionMs)
                // 逆再生したときの、動画のフレームを取り出して、Canvas に書く。
                // getFrameAtTime はマイクロ秒なので注意
                val reverseCurrentPositionMs = durationMs - currentPositionMs
                val bitmap = inputVideoMediaMetadataRetriever.getFrameAtTime(reverseCurrentPositionMs * 1_000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    drawBitmap(bitmap, 0f, 0f, paint)
                }
                currentPositionMs <= durationMs
            }
        )
    }

    /**
     * MediaMetadataRetriever で動画の縦横を取得する
     *
     * @return 1個目が縦かどうか、2個目が Height、3個目が Width
     */
    private fun MediaMetadataRetriever.extractVideoSize(): Triple<Boolean, Int, Int> {
        // Android のメディア系（ Retriever だけでなく、MediaExtractor お前もだぞ）
        // 縦動画の場合、縦と横が入れ替わるワナが存在する
        // ROTATION を見る必要あり
        val videoWidth = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
        val videoHeight = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
        val rotation = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        return when (rotation) {
            // 縦だけ入れ替わるので
            90, 270 -> Triple(true, videoWidth, videoHeight)
            else -> Triple(false, videoHeight, videoWidth)
        }
    }
}
