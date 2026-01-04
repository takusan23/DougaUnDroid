package io.github.takusan23.dougaundroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

/** 画面遷移先一覧 */
sealed interface MainScreenPaths : NavKey {
    @Serializable
    /** メイン画面、処理中もこれ */
    data object Home : MainScreenPaths

    /** 設定画面 */
    @Serializable
    data object Setting : MainScreenPaths

    /** ライセンス */
    @Serializable
    data object License : MainScreenPaths
}

@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(MainScreenPaths.Home)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<MainScreenPaths.Home> {
                HomeScreen(
                    onNavigate = { backStack += it }
                )
            }
            entry<MainScreenPaths.Setting> {
                SettingScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { backStack += it }
                )
            }
            entry<MainScreenPaths.License> {
                LicenseScreen(
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        }
    )
}