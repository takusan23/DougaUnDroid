package io.github.takusan23.dougaundroid.processor

import android.content.Context
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import io.github.takusan23.dougaundroid.processor.audio.AudioProcessor
import io.github.takusan23.dougaundroid.processor.tool.MediaMuxerTool
import io.github.takusan23.dougaundroid.processor.tool.MediaStoreTool
import io.github.takusan23.dougaundroid.processor.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 逆再生動画を作る処理 */
object DougaUnDroidProcessor {

    /**
     * 動画を逆再生動画にする
     * しばらく時間がかかる
     */
    suspend fun start(
        context: Context,
        videoInfo: InputVideoInfo,
        onProgressUpdate: (currentMs: Long) -> Unit
    ): Unit = withContext(Dispatchers.Default) {
        // 一時ファイル置き場
        val tempFolder = context.getExternalFilesDir(null)?.resolve("temp")?.apply { mkdir() }!!
        val reverseVideoFile = tempFolder.resolve("temp_video_reverse.mp4")
        val reverseAudioFile = tempFolder.resolve("temp_audio_reverse.mp4")
        val resultFile = tempFolder.resolve("android_reverse_video_${System.currentTimeMillis()}.mp4")
        val inputUri = videoInfo.uri

        try {

            // 音声トラック・映像トラックそれぞれ並列で処理
            listOf(
                launch {
                    AudioProcessor.start(
                        context = context,
                        inFile = inputUri,
                        outFile = reverseAudioFile,
                        tempFolder = tempFolder
                    )
                },
                launch {
                    VideoProcessor.start(
                        context = context,
                        inFile = inputUri,
                        outFile = reverseVideoFile,
                        onProgressUpdateMs = onProgressUpdate
                    )
                }
            ).joinAll()

            // 音声トラックと映像トラックを合わせる
            MediaMuxerTool.mixAvTrack(
                audioTrackFile = reverseAudioFile,
                videoTrackFile = reverseVideoFile,
                resultFile = resultFile
            )

            // 保存する
            MediaStoreTool.saveToVideoFolder(
                context = context,
                file = resultFile
            )
        } finally {
            // 要らないファイルを消す
            tempFolder.deleteRecursively()
        }
    }

}