package io.github.takusan23.dougaundroid.processor

import android.content.Context
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** 映像を逆にする */
object VideoProcessor {

    /** 映像を逆にする */
    suspend fun start(
        context: Context,
        inUri: Uri,
        outFile: File,
        onProgressUpdateMs: (Long) -> Unit
    ) {
        // 旧バージョンは後ろから動画のフレームを Bitmap を取りだし、Canvas で描画し、描いた Bitmap エンコードしていたが、
        // 今のバージョンは後ろから動画のフレームを取り出し、OpenGL ES のテクスチャとして描画しエンコードするようにした。10Bit HDR に対応できるようになった。
        // https://github.com/takusan23/DougaUnDroid/blob/50bc777e6324c277b25e036a1f863e3edaedbfcc/app/src/main/java/io/github/takusan23/dougaundroid/processor/video/VideoProcessor.kt

        // 元データからエンコーダーを起動するパラメーターを取り出す
        // MediaExtractor だとなんかフレームレートとかビットレートは取れない？ MediaMetadataRetriever にしてみた
        val inputVideoMediaMetadataRetriever = MediaMetadataRetriever().apply { setDataSource(context, inUri) }
        val bitRate = inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 3_000_000
        val (_, videoHeight, videoWidth) = inputVideoMediaMetadataRetriever.extractVideoSize()
        val durationMs = inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt()!!
        val frameRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            inputVideoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toIntOrNull() ?: 30
        } else {
            30
        }
        val frameMs = 1000 / frameRate
        val tenBitHdrPair = inputVideoMediaMetadataRetriever.extractTenBitHdrPair()
        val isTenBitHdr = tenBitHdrPair != null

        // もう使わない
        inputVideoMediaMetadataRetriever.release()

        // エンコーダー
        val akariVideoEncoder = AkariVideoEncoder().apply {
            prepare(
                output = outFile.toAkariCoreInputOutputData(),
                containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                outputVideoWidth = videoWidth,
                outputVideoHeight = videoHeight,
                frameRate = 30,
                bitRate = bitRate,
                keyframeInterval = 1,
                // HDR の情報がある場合は HEVC にして、エンコーダーにも伝える
                codecName = if (isTenBitHdr) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC,
                tenBitHdrParametersOrNullSdr = if (isTenBitHdr && tenBitHdrPair != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    AkariVideoEncoder.TenBitHdrParameters(
                        colorStandard = tenBitHdrPair.first,
                        colorTransfer = tenBitHdrPair.second
                    )
                } else null
            )
        }

        // OpenGL ES で描画する基盤
        val akariGraphicsProcessor = AkariGraphicsProcessor(
            outputSurface = akariVideoEncoder.getInputSurface(),
            width = videoWidth,
            height = videoHeight,
            isEnableTenBitHdr = isTenBitHdr
        ).apply { prepare() }

        // デコーダー
        val akariGraphicsSurfaceTexture = akariGraphicsProcessor.genTextureId { texId -> AkariGraphicsSurfaceTexture(texId) }
        val akariVideoDecoder = AkariVideoDecoder().apply {
            prepare(
                input = inUri.toAkariCoreInputOutputData(context),
                outputSurface = akariGraphicsSurfaceTexture.surface
            )
        }

        coroutineScope {
            // エンコーダー開始
            val encoderJob = launch { akariVideoEncoder.start() }

            // 描画も開始
            val graphicsJob = launch {
                try {
                    val loopContinueData = AkariGraphicsProcessor.LoopContinueData(isRequestNextFrame = true, currentFrameNanoSeconds = 0)
                    var currentPositionMs = 0L

                    akariGraphicsProcessor.drawLoop {
                        // 取り出すべき時間
                        val reverseCurrentPositionMs = durationMs - currentPositionMs

                        // シークして描画
                        akariVideoDecoder.seekTo(reverseCurrentPositionMs)
                        drawSurfaceTexture(akariGraphicsSurfaceTexture)

                        // 時間を伝え、動画時間を超えた場合はループを抜ける
                        loopContinueData.currentFrameNanoSeconds = currentPositionMs * AkariGraphicsProcessor.LoopContinueData.MILLI_SECONDS_TO_NANO_SECONDS
                        currentPositionMs += frameMs
                        loopContinueData.isRequestNextFrame = currentPositionMs <= durationMs
                        onProgressUpdateMs(currentPositionMs)
                        loopContinueData
                    }
                } finally {
                    akariGraphicsProcessor.destroy()
                    akariVideoDecoder.destroy()
                    akariGraphicsSurfaceTexture.destroy()
                }
            }

            // 描画が終わるまで待ってその後エンコーダーも止める
            graphicsJob.join()
            encoderJob.cancelAndJoin()
        }
    }

    /**
     * [MediaMetadataRetriever]で動画の縦横を取得する
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

    /**
     * [MediaMetadataRetriever]で動画が 10Bit HDR に対応しているかを返す。
     * 詳しくはここ
     * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/av/media/libstagefright/FrameDecoder.cpp
     *
     * @return 色域、ガンマカーブをいれた Pair。null の場合は HDR ではない。
     */
    private fun MediaMetadataRetriever.extractTenBitHdrPair(): Pair<Int, Int>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val colorStandard = extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toInt()
            val colorTransfer = extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toInt()
            // HDR かの判定
            if (colorStandard == MediaFormat.COLOR_STANDARD_BT2020 && (colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 || colorTransfer == MediaFormat.COLOR_TRANSFER_HLG)) {
                return Pair(colorStandard, colorTransfer)
            }
        }
        return null
    }
}
