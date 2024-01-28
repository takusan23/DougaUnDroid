package io.github.takusan23.dougaundroid.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.takusan23.dougaundroid.R

/** ソースコード */
private const val GitHubUrl = "https://github.com/takusan23/DougaUnDroid"

/** プライバシーポリシー */
private const val PrivacyPolicyUrl = "https://github.com/takusan23/DougaUnDroid/blob/master/PRIVACY_POLICY.md"

/**
 * 設定画面
 *
 * @param onBack 戻る押した時
 * @param onNavigate 画面遷移呼ばれた時
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigate: (MainScreenPaths) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(id = R.drawable.arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {

            item {
                SettingItem(
                    title = "ソースコードをみる",
                    description = "GitHub が開きます",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, GitHubUrl.toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SettingItem(
                    title = "プライバシーポリシー",
                    description = "プラウザが開きます",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, PrivacyPolicyUrl.toUri())
                        context.startActivity(intent)
                    }
                )
            }

            item {
                SettingItem(
                    title = "ライセンス",
                    description = "thx!!!",
                    onClick = { onNavigate(MainScreenPaths.License) }
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 20.dp)
        ) {
            Text(text = title, fontSize = 18.sp)
            Text(text = description)
        }
    }
}