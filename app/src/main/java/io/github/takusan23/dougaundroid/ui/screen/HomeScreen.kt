package io.github.takusan23.dougaundroid.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import io.github.takusan23.dougaundroid.DougaUnDroidService
import io.github.takusan23.dougaundroid.R
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import io.github.takusan23.dougaundroid.processor.tool.MediaStoreTool
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val inputVideoInfo = remember { mutableStateOf<InputVideoInfo?>(null) }
    val bindService = remember { DougaUnDroidService.bindService(context, lifecycleOwner.lifecycle) }.collectAsState(initial = null)

    val isEncoding = bindService.value?.isEncoding?.collectAsState()
    val currentProgress = bindService.value?.currentProgress?.collectAsState()

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    // Uri からタイトルとか取る
                    inputVideoInfo.value = MediaStoreTool.getInputVideoInfo(context, uri)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {

            if (isEncoding?.value == true) {
                // 処理中ならお待ち下さい画面
                ProgressScreen(
                    progress = currentProgress?.value ?: 0f,
                    onCancelClick = { bindService.value?.stopProcess() }
                )
            } else {
                // 処理してないなら選択画面
                SelectVideoScreen(
                    inputVideoInfo = inputVideoInfo.value,
                    onSelectClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    onStartClick = {
                        val info = inputVideoInfo.value
                        if (info != null) {
                            bindService.value?.startProcess(info)
                        }
                    }
                )
            }
        }
    }
}
