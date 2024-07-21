package ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import state.getResourcesFile
import java.io.File


/**
 * 用在生成词库界面，使用内置词库过滤单词
 */
@Composable
fun BuiltInVocabularyMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    addVocabulary:(File) -> Unit
) {
    var selectedDirectory by remember { mutableStateOf<File?>(null) }
    var showList by remember { mutableStateOf(false) }
    val width = if(showList) 441.dp else 240.dp

    DropdownMenu(
        expanded = expanded,
        offset = DpOffset(20.dp, 0.dp),
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(width).height(400.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.width(width).fillMaxHeight()){
            Column(Modifier.width(240.dp).height(400.dp)){
                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/大学英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/大学英语")
                    }
                )

                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/出国"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/出国")
                    }
                )

                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/牛津核心词"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/牛津核心词")
                    }
                )

                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/北师大版高中英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/北师大版高中英语")
                    }
                )


                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/人教版英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/人教版英语")
                    }
                )

                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/外研版英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/外研版英语")
                    }
                )
                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/新概念英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/新概念英语")
                    }
                )

                VocabularyDirectory(
                    directory = getResourcesFile("vocabulary/商务英语"),
                    selectedDirectory = selectedDirectory,
                    onClick = {
                        showList = true
                        selectedDirectory = getResourcesFile("vocabulary/商务英语")
                    }
                )

            }

            if(showList){
                Divider(Modifier.width(1.dp).height(400.dp))
               VocabularyList(
                   directory = selectedDirectory!!,
                   selectedFile = { file ->
                       addVocabulary(file)
                       onDismissRequest()
                   }
               )
            }
        }

    }
}

@Composable
fun VocabularyDirectory(
    directory: File,
    selectedDirectory: File?,
    onClick: () -> Unit
){
    val selectedBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.12f)
    val background = if(MaterialTheme.colors.isLight) MaterialTheme.colors.background else Color(32, 33, 34)
    DropdownMenuItem(onClick = onClick ,modifier = Modifier.background(if (selectedDirectory == directory) selectedBackground else background)) {

        Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()){
            Text(
                text = directory.nameWithoutExtension,
                color = if (selectedDirectory == directory) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
            )
            Icon(
                imageVector =  Icons.Default.ArrowRight,
                contentDescription = null,
                tint = if (selectedDirectory == directory) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground)
        }
    }
}

@Composable
fun VocabularyList(
    directory: File,
    selectedFile: (File) -> Unit
) {
    if (directory.isDirectory && !directory.listFiles().isNullOrEmpty()) {
        val files = directory.listFiles()
        if (directory.nameWithoutExtension == "人教版英语" ||
            directory.nameWithoutExtension == "外研版英语" ||
            directory.nameWithoutExtension == "北师大版高中英语"
        ) {
            files!!.sortBy { it.nameWithoutExtension.split(" ")[0].toFloat() }
        }

        Box(Modifier.width(200.dp).height(400.dp)){
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(200.dp)
                    .height(400.dp),
                state = listState
            ) {
                items(files!!) { file ->
                    val name = formatName(file, directory)
                    DropdownMenuItem(onClick = { selectedFile(file) }) {
                        Text(
                            text = name,
                            color = MaterialTheme.colors.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }

                }

            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = listState)
            )
        }


    }
}

private fun formatName(file: File, directory: File): String {
    var name = file.nameWithoutExtension
    if (directory.nameWithoutExtension == "人教版英语" ||
        directory.nameWithoutExtension == "外研版英语" ||
        directory.nameWithoutExtension == "北师大版高中英语"
    ) {
        if (name.contains(" ")) {
            name = name.split(" ")[1]
        }

    }
    return name
}