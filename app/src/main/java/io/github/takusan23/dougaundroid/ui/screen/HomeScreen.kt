package io.github.takusan23.dougaundroid.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.dougaundroid.DougaUnDroidService
import io.github.takusan23.dougaundroid.R
import io.github.takusan23.dougaundroid.data.InputVideoInfo
import io.github.takusan23.dougaundroid.processor.tool.MediaStoreTool
import io.github.takusan23.dougaundroid.ui.components.HelloCard
import io.github.takusan23.dougaundroid.ui.components.InputVideoInfoCard
import io.github.takusan23.dougaundroid.ui.components.StartButton
import kotlinx.coroutines.launch

/**
 * メイン画面、逆再生動画の作成中もこの画面
 *
 * @param onNavigate 画面遷移の時呼ばれる
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (MainScreenPaths) -> Unit
) {
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

    fun openPicker() {
        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }

    // 処理中ならお待ち下さい画面を早期 return で出す
    if (isEncoding?.value == true) {
        ProgressScreen(
            progress = currentProgress?.value ?: 0f,
            onNavigate = onNavigate,
            onCancelClick = { bindService.value?.stopProcess() }
        )

        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onNavigate(MainScreenPaths.Setting) }) {
                        Icon(painter = painterResource(id = R.drawable.settings_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            if (inputVideoInfo.value == null) {
                item {
                    HelloCard(
                        onSelectClick = { openPicker() }
                    )
                }
            } else {
                item {
                    InputVideoInfoCard(
                        inputVideoInfo = inputVideoInfo.value!!,
                        onReSelectClick = { openPicker() }
                    )
                }
            }

            // 開始ボタンまでスペースが空いていて欲しい
            item {
                Spacer(modifier = Modifier.height(50.dp))
            }

            if (inputVideoInfo.value != null) {
                item {
                    StartButton(
                        onClick = { bindService.value?.startProcess(inputVideoInfo.value!!) }
                    )
                }
            }
        }
    }
}
