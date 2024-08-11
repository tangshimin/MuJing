import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.application
import kotlinx.serialization.ExperimentalSerializationApi
import ui.App


@OptIn(ExperimentalSerializationApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
fun main() = application {
    App()
}