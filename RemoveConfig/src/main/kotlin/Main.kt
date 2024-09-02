
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
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
fun main(args: Array<String>) = application {
    val uninstall = args.isNotEmpty() && args[0] == "--uninstall"
    val title = if(uninstall) "删除幕境的配置文件" else "重置"
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
                    var removeConfigAndAudio by remember { mutableStateOf(false) }
                    var removeWordScreenConfig by remember { mutableStateOf(false) }
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
                    if(uninstall){
                        Row(Modifier.align(Alignment.Center).clickable(onClick = {removeConfigAndAudio = !removeConfigAndAudio}),
                            verticalAlignment = Alignment.CenterVertically){
                            Checkbox(
                                checked = removeConfigAndAudio,
                                onCheckedChange = { removeConfigAndAudio = it },
                                modifier = Modifier.padding(start = 20.dp)
                            )
                            Text("删除配置文件和单词发音缓存")
                        }
                    }else{

                        Row(Modifier.align(Alignment.Center).clickable(onClick = {removeWordScreenConfig = !removeWordScreenConfig}),
                            verticalAlignment = Alignment.CenterVertically){
                            Checkbox(
                                checked = removeWordScreenConfig,
                                onCheckedChange = { removeWordScreenConfig = it },
                                modifier = Modifier.padding(start = 20.dp)
                            )
                            Text("重置记忆单词界面的设置")
                        }
                    }

                    Row(Modifier.align(Alignment.BottomCenter)){
                        OutlinedButton(
                            enabled = (removeConfigAndAudio || removeWordScreenConfig),
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    val homeDir = File(System.getProperty("user.home"))
                                    val applicationDir = File(homeDir, ".MuJing")
                                    if(removeConfigAndAudio){
                                        if(applicationDir.exists()){
                                            applicationDir.deleteRecursively()
                                        }
                                    }else if(removeWordScreenConfig){
                                        val wordScreenConfig = File(applicationDir, "TypingWordSettings.json")
                                        if(wordScreenConfig.exists()){
                                            wordScreenConfig.delete()
                                        }
                                    }
                                    exitApplication()
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
                            val text = if(uninstall) "不删除缓存" else "不重置"
                            Text(text)
                        }
                    }
                }
            }
        }
    }
}