package io.github.takusan23.dougaundroid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.dougaundroid.R

/**
 * 端末内で処理されるから安心してねカード
 *
 * @param onSelectClick 動画を選ぶを押したときに呼ばれる
 */
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
            Icon(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                painter = painterResource(id = R.drawable.android_douga_undroid),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            Text(
                text = stringResource(id = R.string.hello_message_title),
                fontSize = 20.sp
            )
            Text(text = stringResource(id = R.string.hello_message_description))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSelectClick
            ) {
                Icon(painter = painterResource(id = R.drawable.folder_24px), contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(id = R.string.select_video))
            }
        }
    }
}