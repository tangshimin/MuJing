import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.application
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import io.github.vinceglb.filekit.FileKit
import kotlinx.serialization.ExperimentalSerializationApi
import theme.isSystemDarkMode
import ui.App


@OptIn(ExperimentalSerializationApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
fun main() = application {
    init()
    App()
}

fun init(){
    FileKit.init(appId = "幕境")
    if(isSystemDarkMode()) {
        FlatDarkLaf.setup()
    }else {
        FlatLightLaf.setup()
    }
}