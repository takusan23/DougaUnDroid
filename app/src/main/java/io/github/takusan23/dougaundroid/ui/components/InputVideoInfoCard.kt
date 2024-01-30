package io.github.takusan23.dougaundroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.dougaundroid.R
import io.github.takusan23.dougaundroid.data.InputVideoInfo

/**
 * 入力した動画の情報を表示する
 *
 * @param inputVideoInfo 選択した動画の情報
 * @param onReSelectClick 選び直すを押した時
 */
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
                text = stringResource(id = R.string.select_video_info_title),
                fontSize = 20.sp
            )
            Text(text = "${stringResource(id = R.string.select_video_info_video_title)} : ${inputVideoInfo.name}")
            Text(text = "${stringResource(id = R.string.select_video_info_duration)} : ${inputVideoInfo.videoDurationMs} ms")

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onReSelectClick
            ) {
                Icon(painter = painterResource(id = R.drawable.folder_24px), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.select_video_re_select))
            }
        }
    }
}