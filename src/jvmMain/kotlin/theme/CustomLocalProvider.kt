import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import player.LocalAudioPlayerComponent
import player.rememberAudioPlayerComponent
import ui.LocalCtrl
import ui.rememberCtrl
import ui.textSelectionColors

@Composable
fun CustomLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAudioPlayerComponent provides rememberAudioPlayerComponent(),
        LocalCtrl provides rememberCtrl(),
        LocalTextSelectionColors provides textSelectionColors(),
        content = content
    )
}
