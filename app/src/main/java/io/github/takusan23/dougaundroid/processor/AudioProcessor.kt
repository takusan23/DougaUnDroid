package io.github.takusan23.dougaundroid.processor

import android.content.Context
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import io.github.takusan23.akaricore.audio.AudioEncodeDecodeProcessor
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.dougaundroid.processor.tool.MediaExtractorTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/** 音声を逆にする */
object AudioProcessor {

    /**
     * 音声を逆再生にする
     *
     * @return 動画に音声トラックがない場合は false
     */
    suspend fun start(
        context: Context,
        inUri: Uri,
        outFile: File,
        tempFolder: File
    ): Boolean {
        // そもそも音声トラックがない場合は早期 return。 false
        val inputMediaFormat = MediaExtractorTool.getTrackMediaFormat(context, inUri, MediaExtractorTool.Track.AUDIO) ?: return false
        var outputMediaFormat: MediaFormat? = null

        // 音声の生データ置き場
        val rawDataFile = tempFolder.resolve("raw_file")
        val reverseRawDataFile = tempFolder.resolve("reverse_raw_data")

        try {
            withContext(Dispatchers.Default) {
                // デコードする（AAC を PCM に）
                AudioEncodeDecodeProcessor.decode(
                    input = inUri.toAkariCoreInputOutputData(context),
                    output = rawDataFile.toAkariCoreInputOutputData(),
                    onOutputFormat = { outputFormat ->
                        outputMediaFormat = outputFormat
                    }
                )

                // PCM を操作して、逆再生になるように音声データを並び替える
                reversePcmAudioData(
                    rawPcmFile = rawDataFile,
                    outFile = reverseRawDataFile,
                    samplingRate = outputMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    channelCount = outputMediaFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    // Duration は MediaCodec#outputFormat ではなく MediaExtractor から
                    durationUs = inputMediaFormat.getLong(MediaFormat.KEY_DURATION)
                )

                // PCM を AAC にエンコードする
                AudioEncodeDecodeProcessor.encode(
                    input = reverseRawDataFile.toAkariCoreInputOutputData(),
                    output = outFile.toAkariCoreInputOutputData(),
                    samplingRate = outputMediaFormat!!.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    channelCount = outputMediaFormat!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    bitRate = 192_000,
                    containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                    codecName = MediaFormat.MIMETYPE_AUDIO_AAC
                )
            }
        } finally {
            // 要らないファイルを消す
            withContext(Dispatchers.IO + NonCancellable) {
                rawDataFile.delete()
                reverseRawDataFile.delete()
            }
        }
        return true
    }

    /** PCM 音声データを逆再生できるようにする */
    private suspend fun reversePcmAudioData(
        rawPcmFile: File,
        outFile: File,
        samplingRate: Int,
        channelCount: Int,
        durationUs: Long
    ) = withContext(Dispatchers.IO) {
        // 量子化ビット数を出す（16bit とか 8bit とか。バイトに直すので 16 bitなら 2 byte）
        // 計算しないとダメなの？
        val durationSec = durationUs / 1_000 / 1_000
        val bitDepth = (((rawPcmFile.length() / durationSec) / samplingRate) / channelCount).toInt()

        // 逆にしていく
        // RandomAccessFile にするか、PCM データをメモリに乗せるかのどっちかだと思う。
        RandomAccessFile(rawPcmFile, "r").use { randomAccessFile ->
            outFile.outputStream().use { outputStream ->
                var nextReadPosition = rawPcmFile.length()
                // 量子化ビット数 * チャンネル数 ごとに取り出す
                val audioData = ByteArray(bitDepth * channelCount)
                while (isActive) {
                    // データを逆から読み出す
                    // 現在位置を調整してバイト配列に入れる
                    randomAccessFile.seek(nextReadPosition)
                    randomAccessFile.read(audioData)
                    // 次取り出す位置
                    nextReadPosition -= audioData.size
                    // 書き込む
                    outputStream.write(audioData)
                    // もう次がない場合は
                    if (nextReadPosition < 0) {
                        break
                    }
                }
            }
        }
    }
}