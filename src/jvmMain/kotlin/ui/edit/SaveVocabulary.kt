package ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.extras.FlatSVGUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import state.getResourcesFile
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.JFrame
import kotlin.concurrent.schedule

fun notification(
    text:String,
    close: () -> Unit,
    colors: Colors,
) {
    val window = JFrame("")
    window.size = Dimension(126, 43)
    window.isUndecorated = true
    val iconFile = getResourcesFile("logo/logo.svg")
    val iconImages = FlatSVGUtils.createWindowIconImages(iconFile.toURI().toURL())
    window.iconImages = iconImages
    window.setLocationRelativeTo(null)
    window.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            close()
            window.dispose()
        }
    })

    val composePanel = ComposePanel()
    composePanel.setContent {
        NotifactionContent(
            text = text,
            parentWindow = window,
            close = {
                close()
                window.dispose()
            },
            colors = colors,
        )
    }

    window.contentPane.add(composePanel, BorderLayout.NORTH)
    window.isVisible = true
}

@Composable
fun NotifactionContent(
    parentWindow: JFrame,
    close: () -> Unit,
    text:String,
    colors: Colors,
){
    val windowState = rememberWindowState(
        size = DpSize(126.dp, 43.dp),
        position = WindowPosition(parentWindow.location.x.dp,parentWindow.location.y.dp)
    )
    MaterialTheme(colors = colors){
        Window(
            title = "",
            onCloseRequest = close,
            undecorated = true,
            transparent = true,
            state = windowState,
        ) {
            Surface(
                elevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                shape = RectangleShape
            ) {

                Text(text = text,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colors.onBackground,
                )
            }

            LaunchedEffect(Unit){
                window.isAlwaysOnTop = true
                Timer().schedule(2000){
                    close()
                    window.dispose()
                }
            }
            LaunchedEffect(windowState){
                snapshotFlow { windowState.size }
                    .onEach {
                        // 同步窗口和对话框的大小
                        parentWindow.size = windowState.size.toAwtSize()
                    }
                    .launchIn(this)

                snapshotFlow { windowState.position }
                    .onEach {
                        // 同步窗口和对话框的位置
                        parentWindow.location = windowState.position.toPoint()
                    }
                    .launchIn(this)



            }
        }
    }


}