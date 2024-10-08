package io.github.takusan23.dougaundroid.akaricorev5.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.yield

class AkariVideoDecoder {

    private var decodeMediaCodec: MediaCodec? = null
    private var mediaExtractor: MediaExtractor? = null

    private var prevSeekToMs = 0L

    fun prepare(
        context: Context,
        inputUri: Uri,
        outputSurface: Surface
    ) {
        val mediaExtractor = MediaExtractor().apply {
            context.contentResolver.openFileDescriptor(inputUri, "r")?.use {
                setDataSource(it.fileDescriptor)
            }
        }
        this.mediaExtractor = mediaExtractor

        val videoTrackIndex = (0 until mediaExtractor.trackCount)
            .map { mediaExtractor.getTrackFormat(it) }
            .withIndex()
            .first { it.value.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            .index

        mediaExtractor.selectTrack(videoTrackIndex)
        val mediaFormat = mediaExtractor.getTrackFormat(videoTrackIndex)
        val codecName = mediaFormat.getString(MediaFormat.KEY_MIME)!!

        decodeMediaCodec = MediaCodec.createDecoderByType(codecName).apply {
            configure(mediaFormat, outputSurface, null, 0)
        }
        decodeMediaCodec?.start()
    }

    suspend fun seekTo(seekToMs: Long) {
        val decodeMediaCodec = decodeMediaCodec!!
        val mediaExtractor = mediaExtractor!!

        // シークする。SEEK_TO_PREVIOUS_SYNC なので、シーク位置にキーフレームがない場合はキーフレームがある場所まで戻る
        // エンコードサれたデータを順番通りに送るわけではない（隣接したデータじゃない）ので flush する
        mediaExtractor.seekTo(seekToMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        decodeMediaCodec.flush()

        // デコーダーに渡す
        var isRunning = true
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning) {
            // キャンセル時
            yield()

            // コンテナフォーマットからサンプルを取り出し、デコーダーに渡す
            // while で繰り返しているのは、シーク位置がキーフレームのため戻った場合に、狙った時間のフレームが表示されるまで繰り返しデコーダーに渡すため
            val inputBufferIndex = decodeMediaCodec.dequeueInputBuffer(TIMEOUT_US)
            if (0 <= inputBufferIndex) {
                val inputBuffer = decodeMediaCodec.getInputBuffer(inputBufferIndex)!!
                // デコーダーへ流す
                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                decodeMediaCodec.queueInputBuffer(inputBufferIndex, 0, size, mediaExtractor.sampleTime, 0)
                // 狙ったフレームになるまでデータを進める
                mediaExtractor.advance()
            }

            // デコーダーから映像を受け取る部分
            var isDecoderOutputAvailable = true
            while (isDecoderOutputAvailable) {
                // デコード結果が来ているか
                val outputBufferIndex = decodeMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // リトライが必要
                        isDecoderOutputAvailable = false
                    }

                    0 <= outputBufferIndex -> {
                        // Surface へ描画
                        val doRender = bufferInfo.size != 0
                        decodeMediaCodec.releaseOutputBuffer(outputBufferIndex, doRender)
                        // 欲しいフレームの時間に到達した場合、ループを抜ける
                        val presentationTimeMs = bufferInfo.presentationTimeUs / 1000
                        if (doRender) {
                            if (seekToMs <= presentationTimeMs) {
                                isRunning = false
                                isDecoderOutputAvailable = false
                                prevSeekToMs = presentationTimeMs
                            }
                        }
                    }
                }
            }

            // もうない場合
            if (mediaExtractor.sampleTime == -1L) break
        }
    }

    fun destroy() {
        decodeMediaCodec?.stop()
        decodeMediaCodec?.release()
        mediaExtractor?.release()
    }

    companion object {
        /** MediaCodec タイムアウト */
        private const val TIMEOUT_US = 0L
    }

}