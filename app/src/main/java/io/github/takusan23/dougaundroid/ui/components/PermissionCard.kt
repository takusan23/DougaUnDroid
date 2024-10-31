package io.github.takusan23.dougaundroid.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.takusan23.dougaundroid.R

/**
 * ファイル書き込み権限を要求する Card
 * Android 9 以下で使う。Android 10 以降は MediaStore に追加するだけなら権限が不要。
 *
 * @param modifier [Modifier]
 */
@Composable
fun PermissionCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isGranted = remember { mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) }

    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted.value = it }
    )

    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Icon(
                painter = painterResource(R.drawable.folder_24px),
                contentDescription = null
            )

            if (isGranted.value) {
                Text(text = stringResource(id = R.string.permission_granted_title))
            } else {
                Text(
                    text = stringResource(id = R.string.permission_request_title),
                    fontSize = 20.sp
                )
                Text(text = stringResource(id = R.string.permission_request_description))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { permissionRequest.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) }
                ) {
                    Text(text = stringResource(id = R.string.permission_request_button))
                }
            }

        }
    }
}