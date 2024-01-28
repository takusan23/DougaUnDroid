package io.github.takusan23.dougaundroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.dougaundroid.data.InputVideoInfo

/** 入力した動画の情報を表示する */
@Composable
fun InputVideoInfoCard(
    modifier: Modifier = Modifier,
    inputVideoInfo: InputVideoInfo,
    onReSelectClick: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "選択した動画",
                fontSize = 20.sp
            )
            Text(text = "タイトル : ${inputVideoInfo.name}")
            Text(text = "動画時間 : ${inputVideoInfo.videoDurationMs} ミリ秒")

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onReSelectClick
            ) {
                Text(text = "選び直す")
            }
        }
    }
}