package io.github.takusan23.dougaundroid.processor.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import io.github.takusan23.akaricore.v5.video.AkariGraphicsProcessor
import io.github.takusan23.akaricore.v5.video.gl.AkariGraphicsSurfaceTexture
import io.github.takusan23.dougaundroid.akaricorev5.video.AkariVideoDecoder
import io.github.takusan23.dougaundroid.akaricorev5.video.AkariVideoEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
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

        // エンコーダーをつくる
        val akariVideoEncoder = AkariVideoEncoder().apply {
            prepare(
                outFile = outFile,
                bitRate = bitRate,
                frameRate = frameRate,
                outputVideoWidth = videoWidth,
                outputVideoHeight = videoHeight
            )
        }
        // OpenGL ES の用意をする
        val akariGraphicsProcessor = AkariGraphicsProcessor(
            outputSurface = akariVideoEncoder.getInputSurface(),
            isEnableTenBitHdr = true,
            width = videoWidth,
            height = videoHeight
        ).apply { prepare() }
        // 動画フレームを OpenGL ES のテクスチャとして使う SurfaceTexture を、Processor で使う
        val akariSurfaceTexture = akariGraphicsProcessor.genTextureId { texId -> AkariGraphicsSurfaceTexture(texId) }
        // 動画デコーダー
        val akariVideoDecoder = AkariVideoDecoder().apply { prepare(context, inFile, akariSurfaceTexture.surface) }

        // エンコーダーを起動する
        val encoderJob = launch {
            akariVideoEncoder.start()
        }

        // 描画ループを回す
        val oneFrameMs = 1_000 / frameRate
        val graphicsJob = launch {
            var nextVideoFrameMs = durationMs.toLong()
            var currentPositionMs = 0L
            akariGraphicsProcessor.drawLoop {
                // 返り値
                val info = AkariGraphicsProcessor.DrawInfo(
                    isRunning = 0 <= nextVideoFrameMs,
                    currentFrameMs = currentPositionMs
                )
                onProgressUpdateMs(currentPositionMs)
                // シークして動画フレームを描画する
                akariVideoDecoder.seekTo(nextVideoFrameMs)
                drawSurfaceTexture(akariSurfaceTexture)
                // 動画フレームを後ろから取得なので減らす
                nextVideoFrameMs -= oneFrameMs
                // 動画時間を進める
                currentPositionMs += oneFrameMs
                info
            }
        }

        // graphicsJob が終わったらキャンセルする
        try {
            graphicsJob.join()
            encoderJob.cancelAndJoin()
        } finally {
            akariGraphicsProcessor.destroy()
            akariVideoDecoder.destroy()
            akariSurfaceTexture.destroy()
            inputVideoMediaMetadataRetriever.release()
        }
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
