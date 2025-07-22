import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.application
import kotlinx.serialization.ExperimentalSerializationApi
import ui.App


@OptIn(ExperimentalSerializationApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
fun main() = application {
    // 让 Compose 能显示在 Swing 组件上面
    System.setProperty("compose.interop.blending", "true")
    App()
}