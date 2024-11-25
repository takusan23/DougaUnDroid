package io.github.takusan23.dougaundroid.processor

import android.content.Context
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import io.github.takusan23.dougaundroid.processor.tool.MediaMuxerTool
import io.github.takusan23.dougaundroid.processor.tool.MediaStoreTool
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    ) {
        // 一時ファイル置き場
        val tempFolder = context.getExternalFilesDir(null)?.resolve("temp")?.apply { mkdir() }!!
        val reverseVideoFile = tempFolder.resolve("temp_video_reverse.mp4")
        val reverseAudioFile = tempFolder.resolve("temp_audio_reverse.mp4")
        val resultFile = tempFolder.resolve("android_reverse_video_${System.currentTimeMillis()}.mp4")
        val inputUri = videoInfo.uri

        try {

            // 音声トラック・映像トラックそれぞれ並列で処理
            // 音声トラックがない場合の処理
            var hasAudioTrack = true
            coroutineScope {
                launch {
                    hasAudioTrack = AudioProcessor.start(
                        context = context,
                        inUri = inputUri,
                        outFile = reverseAudioFile,
                        tempFolder = tempFolder
                    )
                }
                launch {
                    VideoProcessor.start(
                        context = context,
                        inUri = inputUri,
                        outFile = reverseVideoFile,
                        onProgressUpdateMs = onProgressUpdate
                    )
                }
            }

            // 音声トラックと映像トラックを合わせる
            if (hasAudioTrack) {
                MediaMuxerTool.mixAvTrack(
                    audioTrackFile = reverseAudioFile,
                    videoTrackFile = reverseVideoFile,
                    resultFile = resultFile
                )
            }

            // 保存する
            // 音声トラックがない場合は映像だけ
            MediaStoreTool.saveToVideoFolder(
                context = context,
                file = if (hasAudioTrack) resultFile else reverseVideoFile,
                fileName = resultFile.name
            )
        } finally {
            // 要らないファイルを消す
            tempFolder.deleteRecursively()
        }
    }

}