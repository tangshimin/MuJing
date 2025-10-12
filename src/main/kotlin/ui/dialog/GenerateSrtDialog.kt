/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */
package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import ffmpeg.startWhisperSrt
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import state.getSettingsDirectory
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.getWhisperModelsDir
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun GenerateSrtDialog(
    close: () -> Unit,
    videoPath: String = "",
    triggeredFrom:String,
    updateStrPath:(String)->Unit = {}
) {

    DialogWindow(
        title = "生成字幕",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(940.dp, 700.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        Surface(elevation = 5.dp) {
            Box(Modifier.fillMaxSize()) {
                val scope = rememberCoroutineScope()
                var clickLocked by remember { mutableStateOf(false) }

                var videoPath by remember { mutableStateOf(videoPath) }
                var language by remember { mutableStateOf("en") }

                // Helper to compute default output path based on video path
                fun defaultOutputFor(path: String): String {
                    if(path.isEmpty()) return ""
                    val file = File(path)
                    val baseName = file.nameWithoutExtension.let { it.ifEmpty { "output" } }
                    val parent = file.parentFile ?: File(".")
                    return File(parent, "$baseName.$language.ai.srt").absolutePath
                }

                var modelPath by remember { mutableStateOf(TextFieldValue("")) }
                var showSettings by remember { mutableStateOf(false) }
                var queueText by remember { mutableStateOf("3") }
                // GPU options (defaults align with ffmpeg whisper docs)
                var useGpu by remember { mutableStateOf(true) }
                var gpuDeviceText by remember { mutableStateOf("0") }
                var outputPath by remember { mutableStateOf(TextFieldValue(defaultOutputFor(videoPath))) }
                var message by remember { mutableStateOf("") }
                var running by remember { mutableStateOf(false) }
                var success by remember { mutableStateOf<Boolean?>(null) }
                var process by remember { mutableStateOf<Process?>(null) }
                var cancelled by remember { mutableStateOf(false) }
                // Progress states
                var totalSec by remember { mutableStateOf<Double?>(null) }
                var curSec by remember { mutableStateOf<Double?>(null) }
                var progressValue by remember { mutableStateOf<Float?>(null) }
                var speedDisp by remember { mutableStateOf("") }
                var etaDisp by remember { mutableStateOf("") }
                var elapsedDisp by remember { mutableStateOf("00:00:00") }
                var totalDisp by remember { mutableStateOf("") }
                // Download dialog
                var showDownloadDialog by remember { mutableStateOf(false) }
                var modelIsEmpty by remember { mutableStateOf(false) }

                var documentWindowVisible by remember { mutableStateOf(false) }


                fun fmtHMS(sec: Double?): String {
                    if (sec == null) return "--:--:--"
                    val s = sec.toInt().coerceAtLeast(0)
                    val h = s / 3600
                    val m = (s % 3600) / 60
                    val ss = s % 60
                    return String.format("%02d:%02d:%02d", h, m, ss)
                }

                // Load saved settings once
                var settingsLoaded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val s = loadGenerateSrtSettings()
                    language = s.language
                    queueText = s.queue.coerceIn(1, 64).toString()
                    useGpu = s.useGpu
                    gpuDeviceText = s.gpuDevice.coerceAtLeast(0).toString()
                    s.modelFileName?.takeIf { it.isNotBlank() }?.let { name ->
                        val dir = getWhisperModelsDir()
                        val candidate = File(dir, name)
                        if (candidate.exists() && candidate.isFile) {
                            modelPath = TextFieldValue(candidate.absolutePath)
                        }
                    }

                    // if model path is empty, show download dialog
                    modelIsEmpty = modelPath.text.isBlank()
                    if (modelIsEmpty) showDownloadDialog = true

                    settingsLoaded = true
                }

                // Persist when fields change
                LaunchedEffect(language, queueText, useGpu, gpuDeviceText, modelPath) {
                    if (!settingsLoaded) return@LaunchedEffect
                    val queue = queueText.toIntOrNull()?.coerceIn(1, 64) ?: 3
                    val gpu = gpuDeviceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val name = File(modelPath.text).name.takeIf { it.isNotBlank() }
                    saveGenerateSrtSettings(
                        GenerateSrtSettings(
                            modelFileName = name,
                            language = language,
                            queue = queue,
                            useGpu = useGpu,
                            gpuDevice = gpu,
                        )
                    )
                }

                // Auto-update outputPath whenever videoPath changes
                LaunchedEffect(videoPath) {
                    outputPath = TextFieldValue(defaultOutputFor(videoPath))
                    message = ""
                    // reset progress when switching inputs
                    totalSec = null; curSec = null; progressValue = null
                    speedDisp = ""; etaDisp = ""; elapsedDisp = "00:00:00"; totalDisp = ""
                }



                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val videoPicker = rememberFilePickerLauncher(
                            title = "选择视频或音频文件",
                            type = FileKitType.File(extensions = listOf("mp3", "wav", "aac", "mkv", "mp4")),

                            mode = FileKitMode.Single,
                        ) { file ->
                            scope.launch(Dispatchers.IO){
                                file?.let {
                                    videoPath = it.file.absolutePath
                                }
                            }
                        }
                        Text("输入视频/音频：", style = MaterialTheme.typography.subtitle1)
                        CustomTextField(
                            value = videoPath,
                            onValueChange = { videoPath = it },
                            modifier = Modifier.width(600.dp).height(35.dp)
                        )
                        IconButton(
                            onClick = {videoPicker.launch() },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically){

                        Text("Whisper 模型文件：", style = MaterialTheme.typography.subtitle1)

                        DropDownBox(
                            enabled = !running,
                            selectedPath = modelPath.text,
                            onSelect = { path -> modelPath = TextFieldValue(path) },
                        )

                        val modelPicker = rememberFilePickerLauncher(
                            title = "选择模型文件",
                            type = FileKitType.File(extensions = listOf("bin")),
                            mode = FileKitMode.Single,
                        ) { file ->
                            scope.launch(Dispatchers.IO){
                                file?.let {
                                    modelPath = TextFieldValue(it.file.absolutePath)
                                }
                            }
                        }
                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "选择本地已经下载的模型", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomCenter,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            ),
                        ) {
                            IconButton(
                                onClick = {modelPicker.launch()},
                                modifier = Modifier.padding(start = 12.dp)
                            ){
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                )
                            }
                        }

                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "下载模型", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomCenter,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            ),
                        ) {
                            IconButton(
                                onClick = { showDownloadDialog = true },
                                modifier = Modifier.padding(start = 4.dp)
                            ){
                                Icon(
                                    icons.Download,
                                    contentDescription = null,
                                )
                            }
                        }

                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "设置", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomCenter,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            ),
                        ) {
                            IconButton(onClick = { showSettings = !showSettings }) {
                                Icon(
                                    Icons.Filled.Tune,
                                    contentDescription = "Localized description",
                                    tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                                )
                            }
                        }


                        TooltipArea(
                            tooltip = {
                                Surface(
                                    elevation = 4.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                                    shape = RectangleShape
                                ) {
                                    Text(text = "帮助文档", modifier = Modifier.padding(10.dp))
                                }
                            },
                            delayMillis = 300,
                            tooltipPlacement = TooltipPlacement.ComponentRect(
                                anchor = Alignment.BottomCenter,
                                alignment = Alignment.BottomCenter,
                                offset = DpOffset.Zero
                            ),
                        ) {
                            IconButton(onClick = { documentWindowVisible = true }) {
                                Icon(
                                    Icons.Filled.Help,
                                    contentDescription = "Localized description",
                                    tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground,
                                )
                            }
                        }
                        if(documentWindowVisible){
                            DocumentWindow(
                                close = {documentWindowVisible = false},
                                currentPage = "GenerateSrtHelp",
                                setCurrentPage = {}

                            )
                        }

                        if(showDownloadDialog){
                            DownloadWhisperModelDialog(
                                modelIsEmpty = modelIsEmpty,
                                close = { showDownloadDialog = false },
                                onDownloaded = { path ->
                                    modelPath = TextFieldValue(path)
                                    showDownloadDialog = false
                                }
                            )
                        }
                    }

                    if(showSettings){
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Column(Modifier.weight(1f)) {
                                Text("语言：", style = MaterialTheme.typography.subtitle1)
                                val langs = listOf("auto", "en", "zh")
                                langs.forEach { lang ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = language == lang, onClick = { language = lang })
                                        Text(lang)
                                    }
                                }
                            }

                            Column(Modifier.weight(1f)) {
                                var tooltipVisible by remember { mutableStateOf(false) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("音频队列时长 ", style = MaterialTheme.typography.subtitle1)
                                    Box{
                                        CustomTextField(
                                            value = queueText,
                                            onValueChange = { new ->
                                                queueText = new.filter { it.isDigit() }
                                            },
                                            modifier = Modifier.height(30.dp).width(36.dp)
                                                .onPointerEvent(PointerEventType.Press){
                                                    if(!tooltipVisible) tooltipVisible = true
                                                }
                                                .onFocusChanged{
                                                    if(it.isFocused) tooltipVisible = true
                                                }
                                        )


                                        if(tooltipVisible){
                                            DropdownMenu(
                                                expanded = true,
                                                onDismissRequest = { tooltipVisible = false },
                                                offset = DpOffset(200.dp, 0.dp),
                                            ){
                                                Text(
                                                    "FFmpeg whisper 滤镜的 queue 表示音频队列大小（时长）。滤镜会维护一个用于识别的音频缓冲窗口，其长度由该值控制；同时该值也作为 VAD 的最大语音时长上限。\n\n" +
                                                            "数值越大：可容纳更长的连续语音，稳定性通常更好；但端到端延迟会更长。\n\n" +
                                                            "建议 3～8 秒：偏实时用 3～5 秒，偏稳定用 6～8 秒。",
                                                    modifier = Modifier.padding(10.dp).widthIn(max = 400.dp)
                                                )
                                            }

                                        }
                                    }

                                    Text(" 秒")
                                }

                            }

                            Column(Modifier.weight(1f)) {
                                Column(Modifier) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("GPU 加速：", style = MaterialTheme.typography.subtitle1)
                                        Checkbox(checked = useGpu, onCheckedChange = { useGpu = it })
                                        Text(if (useGpu) "已启用" else "未启用",
                                            modifier = Modifier.padding(start = 4.dp),
                                            style = MaterialTheme.typography.subtitle1
                                        )
                                    }
                                    Text("启用后将使用 GPU（若可用）运行 whisper，\n以提升性能", style = MaterialTheme.typography.caption)
                                }
                                Spacer(Modifier.height(16.dp))
                                Column(Modifier) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("GPU 设备索引", style = MaterialTheme.typography.subtitle1)
                                        CustomTextField(
                                            value = gpuDeviceText,
                                            onValueChange = { new -> gpuDeviceText = new.filter { it.isDigit() } },
                                            modifier = Modifier.height(30.dp).width(32.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("默认 0", style = MaterialTheme.typography.subtitle1)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("适用于多 GPU 环境", style = MaterialTheme.typography.caption)

                                }
                            }


                        }
                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("输出 SRT 字幕：", style = MaterialTheme.typography.subtitle1)
                        CustomTextField(
                            value = outputPath,
                            onValueChange = { outputPath = it },
                            modifier = Modifier.width(600.dp).height(35.dp)
                        )

                    }

                    if (running) {
                        Column(Modifier.fillMaxWidth()) {
                            if (progressValue != null) {
                                LinearProgressIndicator(progress = progressValue!!, modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val left = buildString {
                                    append("已处理 ")
                                    append(elapsedDisp)
                                    append(" / ")
                                    append(totalDisp.ifEmpty { "--:--:--" })
                                    if (progressValue != null) {
                                        append("  (")
                                        append(((progressValue!! * 100).toInt()).coerceIn(0, 100))
                                        append("%)")
                                    }
                                }
                                Text(left)
                                val right = "速度 " + (speedDisp.ifEmpty { "0" }) +
                                        "    剩余 " + (etaDisp.ifEmpty { "∞" })
                                Text(right)
                            }
                        }
                    }

                    if (message.isNotBlank()) {
                        val color = when (success) {
                            true -> MaterialTheme.colors.primary
                            false -> MaterialTheme.colors.error
                            else -> MaterialTheme.colors.onBackground
                        }
                        Text(message, color = color)
                    }

                    Spacer(Modifier.weight(1f))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (success == true && !running) {
                            OutlinedButton(onClick = {
                                val file = File(outputPath.text)
                                try {
                                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.parentFile)
                                } catch (_: Exception) {}
                            }) { Text("打开输出目录") }
                            Spacer(Modifier.width(8.dp))
                        }
                        OutlinedButton(onClick = {
                            close()
                            if(running){
                                try { process?.destroy() } catch (_: Exception) {}
                            }
                        }) {
                            Text(
                                text = "关闭",
                            )
                         }
                        if(running){
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                cancelled = true
                                message = "正在中断…"
                                try { process?.destroy() } catch (_: Exception) {}
                            }) {
                                Icon(Icons.Filled.Stop,
                                    contentDescription = null,
                                    tint = if (running) Color.Red else MaterialTheme.colors.onBackground
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(text = "停止")
                            }
                        }

                        if(message.startsWith("生成完成") && triggeredFrom != "menu"){
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                updateStrPath(outputPath.text)
                                close()
                            }) {
                                Text(text = "使用此字幕")
                            }
                        }

                        Spacer(Modifier.width(8.dp))
                        val enabled = !running && File(videoPath).exists() && modelPath.text.isNotBlank() && outputPath.text.isNotBlank()
                        OutlinedButton(
                            enabled = enabled,
                            onClick = {
                                if (clickLocked) return@OutlinedButton
                                clickLocked = true
                                val queue = queueText.toIntOrNull()?.coerceIn(1, 64) ?: 3
                                val gpuDevice = gpuDeviceText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                val model = modelPath.text
                                val out = outputPath.text
                                running = true
                                success = null
                                message = ""
                                cancelled = false
                                // reset progress
                                totalSec = null; curSec = null; progressValue = null
                                speedDisp = ""; etaDisp = ""; elapsedDisp = "00:00:00"; totalDisp = ""
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val started = startWhisperSrt(
                                            input = videoPath,
                                            output = out,
                                            modelPath = model,
                                            language = language,
                                            queue = queue,
                                            useGpu = useGpu,
                                            gpuDevice = gpuDevice,
                                            onProgress = { t, total, speed ->
                                                // hop to main to update UI
                                                launch(Dispatchers.Main) {
                                                    if (t != null) {
                                                        curSec = t
                                                        elapsedDisp = fmtHMS(t)
                                                    }
                                                    if (total != null) {
                                                        totalSec = total
                                                        totalDisp = fmtHMS(total)
                                                    }
                                                    progressValue = if (curSec != null && totalSec != null && totalSec!! > 0) {
                                                        (curSec!! / totalSec!!).toFloat().coerceIn(0f, 1f)
                                                    } else null
                                                    speedDisp = speed ?: ""
                                                    val sx = speed?.removeSuffix("x")?.toDoubleOrNull()
                                                    etaDisp = if (sx != null && sx > 0 && curSec != null && totalSec != null) {
                                                        fmtHMS((totalSec!! - curSec!!) / sx)
                                                    } else ""
                                                }
                                            }
                                        )

                                        if (started.isFailure) {
                                            withContext(Dispatchers.Main) {
                                                running = false
                                                success = false
                                                message = started.exceptionOrNull()?.message ?: "启动失败"
                                            }
                                            return@launch
                                        }
                                        process = started.getOrNull()
                                        val exit = process!!.waitFor()
                                        withContext(Dispatchers.Main) {
                                            running = false
                                            process = null
                                            if (cancelled) {
                                                success = false
                                                message = "已中断"
                                            } else {
                                                val file = File(out)
                                                if (exit == 0 && file.exists() && file.length() > 0L) {
                                                    success = true
                                                    message = "生成完成：$out"
                                                } else {
                                                    success = false
                                                    message = "生成失败 (exit=$exit)"
                                                }
                                            }
                                        }
                                    } finally {
                                        clickLocked = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if(enabled) Color.Green else MaterialTheme.colors.onBackground
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("开始生成")
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropDownBox(
    enabled: Boolean = true,
    selectedPath: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
    ){

        var expanded by remember { mutableStateOf(false) }
        val dir = remember { getWhisperModelsDir() }
        // Collect local models (ggml-*.bin) each compose; directory size is small
        val files = remember(expanded) {
            dir.listFiles { f -> f.isFile && f.name.endsWith(".bin", ignoreCase = true) }?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        }
        val selectedName = remember(selectedPath) { File(selectedPath).name }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = enabled){ expanded = true }
                .height(36.dp)
                .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)))
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = selectedName.ifBlank { "选择本地模型" },
                color = if (enabled) MaterialTheme.colors.onBackground else MaterialTheme.colors.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.widthIn(min = 120.dp)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint =  MaterialTheme.colors.onSurface.copy(alpha = 0.38f),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(-12.dp, 0.dp),
            ) {
                if (files.isEmpty()) {
                    DropdownMenuItem(onClick = { expanded = false }) {
                        Text("未发现本地模型，请先下载")
                    }
                } else {
                    files.forEach { f ->
                        DropdownMenuItem(onClick = {
                            onSelect(f.absolutePath)
                            expanded = false
                        }) {
                            val sizeMB = maxOf(1L, f.length() / (1024 * 1024)).toString() + " MB"
                            ListItem (
                                text = { Text(f.name) },
                                trailing = {Text(sizeMB,) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
   value: TextFieldValue,
   onValueChange: (TextFieldValue) -> Unit,
   modifier: Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = modifier
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))),
        singleLine = true,
        textStyle = TextStyle(
            lineHeight = 29.sp,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
fun CustomTextField(
   value: String,
   onValueChange: (String) -> Unit,
   modifier: Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        modifier = modifier
            .border(border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))),
        singleLine = true,
        textStyle = TextStyle(
            lineHeight = 29.sp,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onBackground
        ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
            ) {
                innerTextField()
            }
        }
    )
}

@Serializable
private data class GenerateSrtSettings(
    val modelFileName: String? = null,
    val language: String = "en",
    val queue: Int = 3,
    val useGpu: Boolean = true,
    val gpuDevice: Int = 0,
)

private fun getGenerateSrtSettingsFile(): File = File(getSettingsDirectory(), "GenerateSrtSettings.json")

private val generateSrtJsonPretty = Json { prettyPrint = true; encodeDefaults = true }
private val generateSrtJsonLoose = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class)
private fun loadGenerateSrtSettings(): GenerateSrtSettings {
    val f = getGenerateSrtSettingsFile()
    return if (f.exists()) runCatching {
        generateSrtJsonLoose.decodeFromString(GenerateSrtSettings.serializer(), f.readText())
    }.getOrElse { it.printStackTrace(); GenerateSrtSettings() } else GenerateSrtSettings()
}

private fun saveGenerateSrtSettings(s: GenerateSrtSettings) {
    runCatching {
        val f = getGenerateSrtSettingsFile()
        f.parentFile?.mkdirs()
        f.writeText(generateSrtJsonPretty.encodeToString(GenerateSrtSettings.serializer(), s))
    }.onFailure { it.printStackTrace() }
}
