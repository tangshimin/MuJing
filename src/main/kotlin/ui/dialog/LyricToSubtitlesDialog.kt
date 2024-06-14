package ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lyric.FileManager
import lyric.SongLyric
import ui.createTransferHandler
import java.io.File
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

@Composable
fun LyricToSubtitlesDialog(
    close: () -> Unit,
    futureFileChooser: FutureTask<JFileChooser>,
    openLoadingDialog: () -> Unit,
    closeLoadingDialog: () -> Unit,
){
    Dialog(
        title = "歌词转字幕",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(600.dp, 600.dp)
        ),
    ) {

        val scope = rememberCoroutineScope()
        var inputPath by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("") }
        var convertEnable by remember { mutableStateOf(false) }
        var saveEnable by remember { mutableStateOf(false) }
        var successful by remember { mutableStateOf(false) }
        val songLyric by remember{ mutableStateOf(SongLyric()) }

        val setFile:(File) -> Unit = { file ->
            inputPath = file.absolutePath
            fileName = file.nameWithoutExtension
            convertEnable = true
            saveEnable = false
            successful = false
            songLyric.song.clear()
        }

        //设置窗口的拖放处理函数
        LaunchedEffect(Unit){
            val transferHandler = createTransferHandler(
                singleFile = true,
                showWrongMessage = { message ->
                    JOptionPane.showMessageDialog(window, message)
                },
                parseImportFile = { files ->
                    scope.launch {
                        val file = files.first()
                        if (file.extension == "lrc") {
                            setFile(file)
                        } else {
                            JOptionPane.showMessageDialog(window, "格式不支持")
                        }

                    }
                }
            )
            window.transferHandler = transferHandler
        }



        /** 打开文件对话框 */
        val openFileChooser: () -> Unit = {
            // 打开 windows 的文件选择器很慢，有时候会等待超过2秒
            openLoadingDialog()
            scope.launch (Dispatchers.IO){
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogTitle = "选择 LRC 格式的歌词"
                fileChooser.fileSystemView = FileSystemView.getFileSystemView()
                fileChooser.currentDirectory = FileSystemView.getFileSystemView().defaultDirectory
                fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
                fileChooser.isAcceptAllFileFilterUsed = false
                val fileFilter = FileNameExtensionFilter(" ", "lrc")
                fileChooser.addChoosableFileFilter(fileFilter)
                fileChooser.selectedFile = null
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile
                    setFile(file)
                    closeLoadingDialog()
                } else {
                    closeLoadingDialog()
                }
                fileChooser.selectedFile = null
                fileChooser.isMultiSelectionEnabled = false
                fileChooser.removeChoosableFileFilter(fileFilter)
            }
        }

        val convert: () -> Unit = {
            scope.launch {
                val file = File(inputPath)
                if (file.exists()) {
                    FileManager.readLRC(songLyric, file.absolutePath)
                    saveEnable = true
                    successful = true
                }
            }
        }

        /** 保存文件对话框 */
        val saveFile: () -> Unit = {
            scope.launch (Dispatchers.IO){
                val fileChooser = futureFileChooser.get()
                fileChooser.dialogType = JFileChooser.SAVE_DIALOG
                fileChooser.dialogTitle = "保存字幕"
                val myDocuments = FileSystemView.getFileSystemView().defaultDirectory.path
                fileChooser.selectedFile = File("$myDocuments${File.separator}*.srt")
                val userSelection = fileChooser.showSaveDialog(window)
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    val fileToSave = fileChooser.selectedFile
                    FileManager.writeSRT(songLyric, fileToSave.absolutePath)
                    songLyric.song.clear()
                    fileChooser.selectedFile = null
                    fileName = ""
                    convertEnable = false
                    saveEnable = false
                    successful = false
                }

            }
        }

        Surface(
            elevation = 5.dp,
            shape = RectangleShape,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()){
                Text("把 LRC 格式的歌词转换成 SRT 格式的字幕")
                Spacer(Modifier.height(20.dp))
                if (fileName.isNotEmpty()) {
                    val bottom = if (successful) 5.dp else 20.dp
                    Text(fileName, modifier = Modifier.padding(bottom = bottom))
                }
                if (successful) {
                    Text(
                        text = "转换成功",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.Center){
                    OutlinedButton(onClick = {openFileChooser()}){
                        Text("打开")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {convert()},
                        enabled = convertEnable
                    ){
                        Text("转换")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = {saveFile()},
                        enabled = saveEnable
                    ){
                        Text("保存")
                    }

                }

            }
        }
    }
}