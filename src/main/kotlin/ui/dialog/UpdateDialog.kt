package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import data.GitHubRelease
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.maven.artifact.versioning.ComparableVersion
import player.isWindows
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun UpdateDialog(
    version: String,
    close: () -> Unit,
    autoUpdate:Boolean,
    setAutoUpdate:(Boolean) -> Unit,
    latestVersion:String,
    releaseNote:String,
    ignore:(String) -> Unit,
) {
    Dialog(
        title = "检查更新",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 550.dp)
        ),
    ) {
        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            var detecting by remember { mutableStateOf(true) }
            var downloadable by remember { mutableStateOf(latestVersion.isNotEmpty()) }
            var body by remember { mutableStateOf("") }
            var releaseTagName by remember { mutableStateOf("") }

            fun detectingUpdates(version: String) {
                val client = OkHttpClient()
                val url = "https://api.github.com/repos/tangshimin/mujing/releases/latest"
                val headerName = "Accept"
                val headerValue = "application/vnd.github.v3+json"
                val request = Request.Builder()
                    .url(url)
                    .addHeader(headerName, headerValue)
                    .build()
                try{
                    client.newCall(request).execute().use { response ->
                        detecting = false
                        if (response.code == 200) {
                            if (response.body != null) {
                                val string = response.body!!.string()
                                val format = Json { ignoreUnknownKeys = true }
                                val releases = format.decodeFromString<GitHubRelease>(string)
                                val releaseVersion = ComparableVersion(releases.tag_name)
                                val currentVersion = ComparableVersion(version)
                                body = if (releaseVersion >currentVersion) {
                                    downloadable = true
                                    releaseTagName = releases.tag_name
                                    var releaseContent = "最新版本：${releases.tag_name}\n"
                                    val contentBody = releases.body
                                    if(contentBody != null){
                                        val end = contentBody.indexOf("---")
                                        if(end != -1){
                                            releaseContent += contentBody.substring(0,end)
                                        }
                                    }
                                    releaseContent
                                } else {
                                    downloadable = false
                                    "没有可用更新"
                                }
                            }
                        } else if (response.code == 404) {
                            body = "网页没找到"
                        } else if (response.code == 500) {
                            body = "服务器错误"
                        }
                    }
                }catch (exception: IOException){
                    detecting = false
                    body = exception.toString()

                }

            }

            LaunchedEffect(Unit) {
                if(latestVersion.isEmpty()){
                    Timer("update",false).schedule(500){
                        detectingUpdates(version)
                    }
                }
            }

            Box{
                val stateVertical = rememberScrollState(0)
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().verticalScroll(stateVertical)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("当前版本：$version")
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("自动检查更新")
                        Checkbox(
                            checked = autoUpdate,
                            onCheckedChange = { setAutoUpdate(it) }
                        )
                    }
                    if (latestVersion.isEmpty() && detecting) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Box(Modifier.width(50.dp).height(50.dp)) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Text("正在检查")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp,top = 10.dp,end = 20.dp)
                    ) {
                        if(latestVersion.isNotEmpty()){
                            val note = "最新版本：$latestVersion\n$releaseNote"
                            Text(text = note)
                        }else{
                            Text(body)
                        }
                    }

                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    OutlinedButton(onClick = { close() }) {
                        Text("关闭")
                    }
                    Spacer(Modifier.width(20.dp))
                    val uriHandler = LocalUriHandler.current
                    val latest = "https://github.com/tangshimin/mujing/releases"
                    OutlinedButton(
                        onClick = {
                            uriHandler.openUri(latest)
                            close()
                        },
                        enabled = downloadable
                    ) {
                        Text("下载最新版")
                    }
                    Spacer(Modifier.width(20.dp))
                    val ignoreEnable = latestVersion.isNotEmpty() || releaseTagName.isNotEmpty()
                    OutlinedButton(
                        enabled = ignoreEnable,
                        onClick = {
                        if(latestVersion.isNotEmpty()){
                            ignore(latestVersion)
                        }else{
                            ignore(releaseTagName)
                        }
                            close()
                        }) {
                        Text("忽略")
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(stateVertical)
                )
            }

        }
    }
}

/**
 * 自动检查更新
 */
@OptIn(ExperimentalSerializationApi::class)
fun autoDetectingUpdates(version: String):Triple<Boolean,String,String>{
    val client = OkHttpClient()
    val url = "https://api.github.com/repos/tangshimin/mujing/releases/latest"
    val headerName = "Accept"
    val headerValue = "application/vnd.github.v3+json"
    val request = Request.Builder()
        .url(url)
        .addHeader(headerName, headerValue)
        .build()

    try{
        client.newCall(request).execute().use { response ->
            if (response.code == 200 && response.body != null) {
                val string = response.body!!.string()
                val format = Json { ignoreUnknownKeys = true }
                val releases = format.decodeFromString<GitHubRelease>(string)
                val releaseVersion = ComparableVersion(releases.tag_name)
                val currentVersion = ComparableVersion(version)
                if (releaseVersion >currentVersion) {
                    var note = ""
                    val body = releases.body
                    if(body != null){
                        val end = body.indexOf("---")
                        if(end != -1){
                            note += body.substring(0,end)
                        }
                    }
                    return Triple(true, releases.tag_name,note)
                }
            }
        }
    }catch (exception: IOException){
        exception.printStackTrace()
        return Triple(false, "","")
    }
    return Triple(false, "","")
}
