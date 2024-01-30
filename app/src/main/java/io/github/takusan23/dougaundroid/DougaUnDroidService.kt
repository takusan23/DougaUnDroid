package io.github.takusan23.dougaundroid

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import io.github.takusan23.dougaundroid.processor.DougaUnDroidProcessor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/** MediaCodec を使って、逆再生動画を作るが、時間がかかるためサービスで行う */
class DougaUnDroidService : Service() {
    private val scope = MainScope()
    private val localBinder = LocalBinder(this)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }

    private val _isEncoding = MutableStateFlow(false)
    private val _currentProgress = MutableStateFlow(0f)

    val isEncoding = _isEncoding.asStateFlow()
    val currentProgress = _currentProgress.asStateFlow()

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onDestroy() {
        super.onDestroy()
        stopProcess()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // エンコード中でなければタスクキル時に終了
        if (!isEncoding.value) {
            stopSelf()
        }
    }

    /** 処理を開始する */
    fun startProcess(info: InputVideoInfo) {
        scope.launch {
            // フォアグラウンドサービスに昇格する
            setForegroundNotification()

            // 処理中の位置を見る
            val progressCollectJob = launch {
                _currentProgress.collect { progress ->
                    setForegroundNotification(progress)
                }
            }

            try {
                // 開始
                _isEncoding.value = true
                DougaUnDroidProcessor.start(
                    context = this@DougaUnDroidService,
                    videoInfo = info,
                    onProgressUpdate = { currentMs ->
                        _currentProgress.value = currentMs / info.videoDurationMs.toFloat()
                    }
                )
            } finally {
                // 終了時・コルーチンキャンセル時
                _isEncoding.value = false
                _currentProgress.value = 0f
                // フォアグラウンドサービス終了
                progressCollectJob.cancel()
                ServiceCompat.stopForeground(this@DougaUnDroidService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
    }

    /** 処理を終了する */
    fun stopProcess() {
        scope.coroutineContext.cancelChildren()
    }

    private fun setForegroundNotification(currentPosition: Float = 0f) {
        // 通知ちゃんねる無ければ作る
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW).apply {
                setName(getString(R.string.service_notification_channel_name))
            }.build()
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle(getString(R.string.service_notification_title))
            setContentText(getString(R.string.service_notification_description))
            setProgress(10, (currentPosition * 10).toInt(), false)
            setSmallIcon(R.drawable.android_douga_undroid)
        }.build()
        // 一応 compat で
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            foregroundServiceType
        )
    }


    private class LocalBinder(service: DougaUnDroidService) : Binder() {
        val serviceRef = WeakReference(service)
        val service: DougaUnDroidService
            get() = serviceRef.get()!!
    }

    companion object {
        private const val NOTIFICATION_ID = 4545
        private const val NOTIFICATION_CHANNEL_ID = "running_foreground_service"

        fun bindService(
            context: Context,
            lifecycle: Lifecycle
        ) = callbackFlow {
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val encoderService = (service as LocalBinder).service
                    trySend(encoderService)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    trySend(null)
                }
            }
            // ライフサイクルを監視してバインド、バインド解除する
            val lifecycleObserver = object : DefaultLifecycleObserver {
                val intent = Intent(context, DougaUnDroidService::class.java)
                override fun onStart(owner: LifecycleOwner) {
                    super.onStart(owner)
                    context.startService(intent)
                    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onStop(owner)
                    context.unbindService(serviceConnection)
                }
            }
            lifecycle.addObserver(lifecycleObserver)
            awaitClose { lifecycle.removeObserver(lifecycleObserver) }
        }
    }

}
