package io.github.takusan23.dougaundroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.dougaundroid.R
import io.github.takusan23.dougaundroid.data.InputVideoInfo

/**
 * 動画を選ぶ画面
 */
@Composable
fun SelectVideoScreen(
    modifier: Modifier = Modifier,
    inputVideoInfo: InputVideoInfo?,
    onSelectClick: () -> Unit,
    onStartClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = """
                    選択した動画が逆再生になる動画ファイルを作るアプリです。
                    逆再生動画を作る処理は端末の中で行われます。
                """.trimIndent()
            )
        }

        Button(onClick = onSelectClick) {
            Text(text = stringResource(id = R.string.home_screen_button_select_video))
        }

        if (inputVideoInfo != null) {

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "選択した動画",
                    fontSize = 24.sp
                )
                Text(text = "タイトル : ${inputVideoInfo.name}")
                Text(text = "動画時間 : ${inputVideoInfo.videoDurationMs} ミリ秒")

                Button(onClick = onStartClick) {
                    Text(text = "処理を開始")
                }
            }

        }
    }
}