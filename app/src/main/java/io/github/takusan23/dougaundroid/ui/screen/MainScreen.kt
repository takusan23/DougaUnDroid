package io.github.takusan23.dougaundroid.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** 画面遷移先一覧 */
enum class MainScreenPaths(val path: String) {
    /** メイン画面、処理中もこれ */
    Home("home"),

    /** 設定画面 */
    Setting("setting"),

    /** ライセンス */
    License("license")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = MainScreenPaths.Home.path
    ) {
        composable(MainScreenPaths.Home.path) {
            HomeScreen(
                onNavigate = { navController.navigate(it.path) }
            )
        }
        composable(MainScreenPaths.Setting.path) {
            SettingScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it.path) }
            )
        }
        composable(MainScreenPaths.License.path) {
            LicenseScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}