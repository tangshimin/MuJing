package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.DownloadNetworkException
import util.WhisperDownloadState
import util.WhisperModelOption
import util.defaultWhisperModels
import util.downloadWhisperModel
import util.getWhisperModelsDir
import util.MirrorSettings
import java.awt.Desktop
import java.io.File

@Composable
fun DownloadWhisperModelDialog(
    close: () -> Unit,
    onDownloaded: (String) -> Unit,
    modelIsEmpty: Boolean = false,
) {
    DialogWindow(
        title = "下载 Whisper 模型",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(580.dp, 450.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        DownloadWhisperModelComponent(
            modelIsEmpty = modelIsEmpty,
            close = close,
            onDownloaded = onDownloaded,
        )
    }
}


@Composable
fun DownloadWhisperModelComponent(
    modelIsEmpty: Boolean = false,
    close: () -> Unit,
    onDownloaded: (String) -> Unit,
){
    Surface(elevation = 5.dp) {
        val scope = rememberCoroutineScope()
        var options by remember { mutableStateOf(defaultWhisperModels) }
        var selected by remember { mutableStateOf(options[3]) }// base.en.bin 这里还需要优化，如果这个模型已经下载了呢
        var progress by remember { mutableStateOf(0f) }
        var speedText by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("") }
        var running by remember { mutableStateOf(false) }
        var cancelled by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var useMirror by remember { mutableStateOf(MirrorSettings.isHfMirrorEnabled()) }

        fun modelsDir(): File = getWhisperModelsDir()
        fun finalFile(opt: WhisperModelOption): File = modelsDir().resolve(opt.fileName)
        fun partialFile(opt: WhisperModelOption): File = modelsDir().resolve(opt.fileName + ".part")

        fun isDownloaded(opt: WhisperModelOption): Boolean {
            val file = finalFile(opt)
            return file.exists() && file.length() > 0L
        }

        fun partialBytes(opt: WhisperModelOption): Long = partialFile(opt).let { if (it.exists()) it.length() else 0L }
        fun hasPartial(opt: WhisperModelOption): Boolean = partialFile(opt).exists()

        // 仅恢复上次正在下载的模型名称到下拉选择，不自动开始下载
        LaunchedEffect(Unit) {
            WhisperDownloadState.read()?.let { st ->
                WhisperDownloadState.resolveOption(st)?.let { opt ->
                    selected = opt
                    // 若该模型已下载完成，清理持久化状态
                    if (isDownloaded(opt)) {
                        WhisperDownloadState.clear()
                    }
                }
            }
        }

        fun startDownload(opt: WhisperModelOption) {
            running = true
            cancelled = false
            progress = 0f
            speedText = ""
            status = if (hasPartial(opt)) "继续下载中..." else "准备下载..."
            error = null
            // 保存当前下载状态（只作为恢复下拉选择用途，不触发自动下载）
            WhisperDownloadState.save(opt)

            val startTime = System.currentTimeMillis()
            var lastBytes = 0L
            var lastTime = startTime

            scope.launch(Dispatchers.IO) {
                val result = downloadWhisperModel(
                    option = opt,
                    onProgress = { downloaded, total ->
                        val now = System.currentTimeMillis()
                        val dt = (now - lastTime).coerceAtLeast(1)
                        val db = downloaded - lastBytes
                        val speed = db * 1000.0 / dt.toDouble() // bytes/sec
                        val speedStr = if (speed > 1024 * 1024) {
                            String.format("%.1f MB/s", speed / (1024 * 1024))
                        } else if (speed > 1024) {
                            String.format("%.0f KB/s", speed / 1024)
                        } else {
                            String.format("%.0f B/s", speed)
                        }
                        lastBytes = downloaded
                        lastTime = now
                        speedText = speedStr
                        val pct = if (total != null && total > 0) (downloaded.toDouble() / total.toDouble() * 100).coerceIn(0.0, 100.0) else null
                        val etaText = if (total != null && total > 0 && speed > 0) {
                            val remain = (total - downloaded).coerceAtLeast(0)
                            val etaSec = (remain / speed).toLong()
                            " · 剩余 ${etaSec.toHumanDuration()}"
                        } else ""
                        status = if (total != null) {
                            buildString {
                                append("${downloaded.toHumanBytes()} / ${total.toHumanBytes()}")
                                if (pct != null) append("  (").append(String.format("%.1f%%", pct)).append(")")
                                append(etaText)
                            }
                        } else {
                            "已下载 ${downloaded.toHumanBytes()}"
                        }
                        if (total != null && total > 0) {
                            progress = (downloaded.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
                        } else {
                            progress = -1f // indeterminate
                        }
                    },
                    isCancelled = { cancelled }
                )
                running = false
                if (result.isSuccess) {
                    val file = result.getOrNull()!!
                    status = "下载完成：${file.absolutePath}"
                    error = null
                    // 下载完成后清除持久化状态
                    WhisperDownloadState.clear()
                    onDownloaded(file.absolutePath)
                } else {
                    val ex = result.exceptionOrNull()
                    when (ex) {
                        is InterruptedException -> {
                            status = "已暂停，已保留进度"
                            error = null
                            // 暂停时保留状态文件以便重启继续
                        }
                        is DownloadNetworkException -> {
                            status = "下载中断"
                            error = ex.message
                            // 网络中断时保留状态文件以便重启继续
                        }
                        else -> {
                            error = ex?.message ?: "下载失败"
                            status = "下载失败"
                            // 硬失败清除状态
                            WhisperDownloadState.clear()
                        }
                    }
                }
            }
        }

        fun restartDownload(opt: WhisperModelOption) {
            // 删除已存在的分片与成品，重新开始
            try { partialFile(opt).delete() } catch (_: Exception) {}
            try { finalFile(opt).delete() } catch (_: Exception) {}
            startDownload(opt)
        }

        Column(modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if(modelIsEmpty){
                // 这种情况是用户第一次启动，或者手动删除了模型文件，最主要的原因是第一次启动，要告诉用户为什么要下载模型
                val text = "Whisper 模型是语音识别的核心文件，首次使用前需要下载。下载完成后即可进行语音转文字等相关操作。"
                Text(text, style = MaterialTheme.typography.subtitle1)
            }
            val tip = "以 .en 结尾表示英语；无语言后缀表示多语言。" +
                    "tiny、base、small、medium、large 代表模型大小和能力，模型越大，识别准确率越高，但也更占用存储空间和内存。"
            Text(tip, style = MaterialTheme.typography.caption)

            // Dropdown for model selection
            var expanded by remember { mutableStateOf(false) }
            val selectedDownloaded = isDownloaded(selected)
            val selectedPartialBytes = partialBytes(selected)
            val hasSelectedPartial = hasPartial(selected)

            Row(verticalAlignment = Alignment.CenterVertically,){
                Text("选择模型：", style = MaterialTheme.typography.subtitle1)
                Box{
                    Row(
                        modifier = Modifier
                            .clickable{ expanded = !expanded }
                            .border(
                                border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                            )
                    ) {
                        Text(
                            text = buildString {
                                append(selected.name)
                                if (selected.sizeHint.isNotBlank()) append("  ").append(selected.sizeHint)
                                if (selectedDownloaded) append("  已下载")
                            },
                            modifier = Modifier.padding(start = 16.dp,top = 12.dp, bottom = 12.dp)
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )


                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { opt ->
                            val downloaded = isDownloaded(opt)
                            val partialBytes = partialBytes(opt)
                            val hasPartial = partialFile(opt).exists()
                            DropdownMenuItem(onClick = {
                                if(!downloaded){
                                    selected = opt
                                    expanded = false
                                }

                            },
                                modifier = Modifier.background(
                                    if(opt.name == selected.name) MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                                    else MaterialTheme.colors.surface
                                )) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(opt.name)
                                    if (opt.sizeHint.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(opt.sizeHint, style = MaterialTheme.typography.caption)
                                    }
                                    if (downloaded) {
                                        Spacer(Modifier.width(12.dp))
                                        Text("已下载完成", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.caption)
                                    } else if (hasPartial) {
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            if (partialBytes > 0L) "已下载 ${partialBytes.toHumanBytes()}" else "未完成",
                                            color = MaterialTheme.colors.secondary,
                                            style = MaterialTheme.typography.caption
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (hasSelectedPartial) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (selectedPartialBytes > 0L) "（已下载 ${selectedPartialBytes.toHumanBytes()}）" else "可继续下载",
                        color = MaterialTheme.colors.secondary,
                        style = MaterialTheme.typography.caption
                    )
                }
            }


            Row(verticalAlignment = Alignment.CenterVertically,){
                Text("使用镜像：", style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = useMirror,
                    onCheckedChange = { enabled ->
                        useMirror = enabled
                        MirrorSettings.setHfMirrorEnabled(enabled)
                    }
                )
                val mirrorTip = "使用镜像站 hf-mirror.com 提升下载速度，\n减少因网络问题导致的中断。"
                Text(mirrorTip, style = MaterialTheme.typography.caption)
            }


            // Show target path and downloaded badge
            val targetPath = finalFile(selected).absolutePath
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("保存到：")
                Spacer(Modifier.width(6.dp))
                Text(targetPath, style = MaterialTheme.typography.caption)
                if (selectedDownloaded) {
                    Spacer(Modifier.width(12.dp))
                    Text("已下载", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.caption)
                }
            }

            if (running) {
                if (progress >= 0f) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(status)
                    Text(speedText)
                }
            } else if (status.isNotBlank()) {
                Text(status)
            }

            error?.let { Text(it, color = MaterialTheme.colors.error) }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = {
                    val dir = modelsDir()
                    try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir) } catch (_: Exception) {}
                }) { Text("打开模型目录") }
                Spacer(Modifier.width(8.dp))
                if (running) {
                    OutlinedButton(onClick = { cancelled = true }) { Text("暂停") }
                } else {
                    OutlinedButton(onClick = { close() }) { Text("关闭") }
                    Spacer(Modifier.width(8.dp))
                    if (selectedDownloaded) {
                        Button(onClick = { restartDownload(selected) }) { Text("重新下载") }
                    } else if (hasSelectedPartial) {
                        OutlinedButton(onClick = { restartDownload(selected) }) { Text("重新下载") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { startDownload(selected) }) { Text("继续下载") }
                    } else {
                        Button(onClick = { startDownload(selected) }) { Text("开始下载") }
                    }
                }
            }
        }
    }
}

private fun Long.toHumanBytes(): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        this >= gb -> String.format("%.2f GB", this / gb)
        this >= mb -> String.format("%.2f MB", this / mb)
        this >= kb -> String.format("%.0f KB", this / kb)
        else -> "${this} B"
    }
}

private fun Long.toHumanDuration(): String {
    var sec = this
    if (sec <= 0) return "<1s"
    val h = sec / 3600
    sec %= 3600
    val m = sec / 60
    val s = sec % 60
    return when {
        h > 0 -> String.format("%dh %dm", h, m)
        m > 0 -> String.format("%dm %ds", m, s)
        else -> String.format("%ds", s)
    }
}
