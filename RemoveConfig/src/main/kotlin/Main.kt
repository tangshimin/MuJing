
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalFoundationApi
@ExperimentalAnimationApi
fun main() = application {
    val title =  "删除幕境的配置文件"
    Window(
        title = title,
        icon = painterResource("logo.png"),
        state = rememberWindowState(
            size = DpSize(462.dp, 249.dp),
            position = WindowPosition(Alignment.Center),
        ),
        onCloseRequest = ::exitApplication,
        alwaysOnTop = true,
        transparent = true,
        undecorated = true,
        resizable = false
    ) {
        val isDarkTheme = isSystemInDarkTheme()
        MaterialTheme(colors = if(isDarkTheme) darkColors() else lightColors()) {
            Surface{
                Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
                    var removeConfig by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    Text(title,
                        fontSize = MaterialTheme.typography.h5.fontSize,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp))
                    IconButton(
                        onClick = {
                            exitApplication()
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Localized description",
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                    Row(Modifier.align(Alignment.Center).clickable(onClick = {removeConfig = !removeConfig}),
                        verticalAlignment = Alignment.CenterVertically){
                        Checkbox(
                            checked = removeConfig,
                            onCheckedChange = { removeConfig = it },
                            modifier = Modifier.padding(start = 20.dp)
                        )
                        Text("删除配置文件和单词发音缓存")
                    }
                    Row(Modifier.align(Alignment.BottomCenter)){
                        OutlinedButton(
                            enabled = removeConfig,
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    if(removeConfig){
                                        val homeDir = File(System.getProperty("user.home"))
                                        val applicationDir = File(homeDir, ".MuJing")
                                        if(applicationDir.exists()){
                                            applicationDir.deleteRecursively()
                                        }
                                        exitApplication()
                                    }
                                }
                            },
                        ) {
                            Text("确定")
                        }

                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = {
                                exitApplication()
                            },
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}

