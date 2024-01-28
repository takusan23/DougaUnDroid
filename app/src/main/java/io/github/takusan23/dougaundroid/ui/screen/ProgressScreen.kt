package io.github.takusan23.dougaundroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.dougaundroid.R

/**
 * 逆再生の動画を作ってますよ画面
 *
 * @param progress 進捗
 * @param onCancelClick キャンセル押した時
 * @param onNavigate 画面遷移呼ばれた時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    progress: Float,
    onNavigate: (MainScreenPaths) -> Unit,
    onCancelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "作成中です") },
                actions = {
                    IconButton(onClick = { onNavigate(MainScreenPaths.Setting) }) {
                        Icon(painter = painterResource(id = R.drawable.settings_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            LinearProgressIndicator(progress = progress)

            Text(text = "逆再生動画を作成しています。")
            Text(text = "少し時間がかかります。アプリを離れても処理は継続されますので、安心してください。")

            Button(onClick = onCancelClick) {
                Text(text = "キャンセル")
            }
        }
    }
}