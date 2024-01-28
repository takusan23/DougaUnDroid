package io.github.takusan23.dougaundroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 端末内で処理されるから安心してねカード */
@Composable
fun HelloCard(
    modifier: Modifier = Modifier,
    onSelectClick: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "逆再生の動画を作るアプリです",
                fontSize = 20.sp
            )
            Text(text = "選択した動画が逆再生になる動画を作成します。逆再生の動画を作成する処理は、端末内で完結します（インターネット接続は不要です。オフラインでも動きます）。")

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSelectClick
            ) {
                Text(text = "動画を選ぶ")
            }
        }
    }
}