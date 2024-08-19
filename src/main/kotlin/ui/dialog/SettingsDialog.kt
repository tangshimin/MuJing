package ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import ui.word.WordScreenState
import theme.createColors
import ui.flatlaf.updateFlatLaf
import theme.toAwt
import ui.window.windowBackgroundFlashingOnCloseFixHack
import ui.word.*
import util.rememberMonospace


@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SettingsDialog(
    close: () -> Unit,
    state: AppState,
    wordScreenState: WordScreenState,
) {
    Dialog(
        title = "设置",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = true,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(900.dp, 700.dp)
        ),
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        MaterialTheme(colors = state.colors) {
            Surface(
                elevation = 5.dp,
                shape = RectangleShape,
            ) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {

                    Row(Modifier.fillMaxSize()) {
                        var currentPage by remember { mutableStateOf("Theme") }
                        Column(Modifier.width(100.dp).fillMaxHeight()) {

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { currentPage = "Theme" }
                                    .fillMaxWidth()
                                    .height(48.dp)) {
                                Text("主题", modifier = Modifier.padding(start = 16.dp))
                                if (currentPage == "Theme") {
                                    Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { currentPage = "TextStyle" }
                                    .fillMaxWidth()
                                    .height(48.dp)) {
                                Text("字体样式", modifier = Modifier.padding(start = 16.dp))
                                if (currentPage == "TextStyle") {
                                    Spacer(Modifier.fillMaxHeight().width(2.dp).background(MaterialTheme.colors.primary))
                                }
                            }

                        }
                        Divider(Modifier.fillMaxHeight().width(1.dp))
                        when (currentPage) {
                            "Theme" -> {
                                SettingTheme(state)
                            }
                            "TextStyle" -> {
                                SettingTextStyle(state,wordScreenState)
                            }

                        }
                    }
                    Divider(Modifier.align(Alignment.TopCenter))
                    Column(
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth().height(60.dp)
                    ) {
                        Divider()
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)
                        ) {
                            OutlinedButton(
                                onClick = { close() },
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text("关闭")
                            }
                        }
                    }
                }

            }
        }

    }
}



@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SettingTextStyle(
    state: AppState,
    wordScreenState: WordScreenState,
) {
    val fontFamily  = rememberMonospace()
    if (wordScreenState.vocabulary.size > 1) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 60.dp)
        ) {
//            val background = if (MaterialTheme.colors.isLight) Color.LightGray else MaterialTheme.colors.background
            Column(Modifier.width(600.dp)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 50.dp, top = 20.dp, bottom = 10.dp, end = 50.dp)
                ) {

                    Text("单词的样式")
                    Spacer(Modifier.width(15.dp))

                    TextStyleChooser(
                        isWord = true,
                        selectedTextStyle = state.global.wordTextStyle,
                        textStyleChange = {
                            state.global.wordTextStyle = it
                            state.saveGlobalState()
                        }
                    )
                    Spacer(Modifier.width(30.dp))
                    Text("字间隔空")
                    Spacer(Modifier.width(15.dp))
                    Box {
                        var spacingExpanded by remember { mutableStateOf(false) }
                        var spacingText by remember { mutableStateOf("5sp") }
                        OutlinedButton(
                            onClick = { spacingExpanded = true },
                            modifier = Modifier
                                .width(120.dp)
                                .background(Color.Transparent)
                                .border(1.dp, Color.Transparent)
                        ) {
                            Text(text = spacingText)
                            Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
                        }
                        DropdownMenu(
                            expanded = spacingExpanded,
                            onDismissRequest = { spacingExpanded = false },
                            modifier = Modifier.width(120.dp)
                                .height(300.dp)
                        ) {
                            val modifier = Modifier.width(120.dp).height(40.dp)
                            for (i in 0..6) {
                                DropdownMenuItem(
                                    onClick = {
                                        state.global.letterSpacing = (i).sp
                                        spacingText = "${i}sp"
                                        spacingExpanded = false
                                        state.saveGlobalState()
                                    },
                                    modifier = modifier
                                ) {
                                    Text("${i}sp")
                                }
                            }

                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.width(600.dp).padding(bottom = 10.dp)

                ) {
                    var textHeight by remember { mutableStateOf(0.dp) }
                    val smallStyleList =
                        listOf(
                            "H5",
                            "H6",
                            "Subtitle1",
                            "Subtitle2",
                            "Body1",
                            "Body2",
                            "Button",
                            "Caption",
                            "Overline"
                        )
                    val bottom = computeBottom(textStyle = state.global.wordTextStyle, textHeight = textHeight)
                    var previewWord = wordScreenState.getCurrentWord().value
                    if (previewWord.isEmpty()) {
                        previewWord = "Typing"
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.onBackground,
                                    fontFamily = fontFamily,
                                    fontSize = state.global.wordFontSize,
                                    letterSpacing = state.global.letterSpacing
                                )
                            ) {
                                append(previewWord)
                            }
                        },
                        modifier = Modifier
                            .padding(bottom = bottom)
                            .onGloballyPositioned { layoutCoordinates ->
                                textHeight = (layoutCoordinates.size.height).dp
                            }
                    )
                    Spacer(Modifier.width(5.dp))
                    Column(verticalArrangement = Arrangement.SpaceBetween) {
                        val top = (textHeight - 36.dp).div(2)
                        var numberFontSize = LocalTextStyle.current.fontSize
                        if (smallStyleList.contains(state.global.wordTextStyle)) numberFontSize =
                            MaterialTheme.typography.overline.fontSize
                        Spacer(modifier = Modifier.height(top))
                        Text(
                            text = "3",
                            color = MaterialTheme.colors.primary,
                            fontSize = numberFontSize
                        )
                        Spacer(modifier = Modifier.height(top))
                        Text(
                            text = "1",
                            color = Color.Red,
                            fontSize = numberFontSize
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    var volumeTop = textHeight.div(2) - 20.dp
                    if (volumeTop < 0.dp) volumeTop = 0.dp
                    if (state.global.wordTextStyle == "H1") volumeTop = 23.dp

                    Icon(
                        Icons.Filled.VolumeDown,
                        contentDescription = "Localized description",
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(top = volumeTop),
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Column(Modifier.width(600.dp)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 50.dp, top = 20.dp, bottom = 10.dp, end = 100.dp)
                ) {
                    Text("详细信息的样式")
                    Spacer(Modifier.width(15.dp))
                    TextStyleChooser(
                        isWord = false,
                        selectedTextStyle = state.global.detailTextStyle,
                        textStyleChange = {
                            state.global.detailTextStyle = it
                            state.saveGlobalState()
                        }
                    )
                }
                val currentWord = wordScreenState.getCurrentWord()
                Phonetic(
                    word = currentWord,
                    phoneticVisible = true,
                    fontSize = state.global.detailFontSize
                )
                Morphology(
                    word = currentWord,
                    isPlaying = false,
                    searching = false,
                    morphologyVisible = true,
                    fontSize = state.global.detailFontSize
                )
                Definition(
                    word = currentWord,
                    definitionVisible = true,
                    isPlaying = false,
                    fontSize = state.global.detailFontSize
                )
                Translation(
                    word = currentWord,
                    translationVisible = true,
                    isPlaying = false,
                    fontSize = state.global.detailFontSize
                )
            }

        }
    } else {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = "请先选择词库",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }


}

@Composable
fun TextStyleChooser(
    isWord: Boolean,
    selectedTextStyle: String,
    textStyleChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .width(120.dp)
                .background(Color.Transparent)
                .border(1.dp, Color.Transparent)
        ) {
            Text(text = selectedTextStyle)
            Icon(Icons.Default.ExpandMore, contentDescription = "Localized description")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(120.dp)
                .height(if (isWord) 500.dp else 260.dp)
        ) {
            val modifier = Modifier.width(120.dp).height(40.dp)
            if (isWord) {
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("H1")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("H1")
                }
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("H2")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("H2")
                }
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("H3")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("H3")
                }
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("H4")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("H4")
                }
            }

            DropdownMenuItem(
                onClick = {
                    textStyleChange("H5")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("H5")
            }
            DropdownMenuItem(
                onClick = {
                    textStyleChange("H6")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("H6")
            }
            DropdownMenuItem(
                onClick = {
                    textStyleChange("Subtitle1")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("Subtitle1")
            }
            DropdownMenuItem(
                onClick = {
                    textStyleChange("Subtitle2")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("Subtitle2")
            }
            DropdownMenuItem(
                onClick = {
                    textStyleChange("Body1")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("Body1")
            }
            DropdownMenuItem(
                onClick = {
                    textStyleChange("Body2")
                    expanded = false
                },
                modifier = modifier
            ) {
                Text("Body2")
            }
            if (isWord) {
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("Caption")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("Caption")
                }
                DropdownMenuItem(
                    onClick = {
                        textStyleChange("Overline")
                        expanded = false
                    },
                    modifier = modifier
                ) {
                    Text("Overline")
                }

            }

        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SettingTheme(
    appState: AppState,
) {


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        val scope = rememberCoroutineScope()
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {

            Text("深色模式", color = MaterialTheme.colors.onBackground, modifier = Modifier.padding(start = 10.dp))
            Spacer(Modifier.width(15.dp))
            Switch(
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                checked = appState.global.isDarkTheme,
                onCheckedChange = {
                    scope.launch {
                        appState.global.isDarkTheme = it
                        appState.colors = createColors(
                            appState.global.isDarkTheme,
                            appState.global.primaryColor,
                            appState.global.backgroundColor,
                            appState.global.onBackgroundColor
                        )
                        appState.saveGlobalState()
                    }

                },
            )
            Spacer(Modifier.width(80.dp))
        }
        var selectPrimaryColor by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { selectPrimaryColor = true }, Modifier.padding(end = 80.dp)) {
            Text("主色调")
        }
        if(selectPrimaryColor){
            ColorPicker(
                close = {selectPrimaryColor = false},
                mode = "PrimaryColor",
                initColor = appState.global.primaryColor,
                confirm = { selectedColor ->
                    appState.global.primaryColor = selectedColor
                    appState.colors = createColors(
                        appState.global.isDarkTheme,
                        appState.global.primaryColor,
                        appState.global.backgroundColor,
                        appState.global.onBackgroundColor
                    )
                    appState.saveGlobalState()
                    selectPrimaryColor = false
                },
                reset = {
                    // 恢复默认颜色,绿色
                    appState.global.primaryColor = Color(9, 175, 0)
                    appState.colors = createColors(
                        appState.global.isDarkTheme,
                        appState.global.primaryColor,
                        appState.global.backgroundColor,
                        appState.global.onBackgroundColor
                    )
                    appState.saveGlobalState()
                    selectPrimaryColor = false
                },
                appState = appState
            )
        }

        if (!appState.global.isDarkTheme) {

            var selectBackgroundColor by remember { mutableStateOf(false) }
            var selectOnBackgroundColor by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { selectBackgroundColor = true }, Modifier.padding(end = 80.dp)) {
                Text("设置背景颜色")
            }
            if(selectBackgroundColor){
                ColorPicker(
                    close = {selectBackgroundColor = false},
                    mode = "Background",
                    initColor = appState.global.backgroundColor,
                    confirm = { selectedColor ->
                        appState.global.backgroundColor = selectedColor
                        appState.colors = createColors(
                            appState.global.isDarkTheme,
                            appState.global.primaryColor,
                            appState.global.backgroundColor,
                            appState.global.onBackgroundColor
                        )
                        updateFlatLaf(
                            appState.global.isDarkTheme,
                            appState.global.backgroundColor.toAwt(),
                            appState.global.onBackgroundColor.toAwt()
                        )
                        appState.saveGlobalState()
                        selectBackgroundColor = false
                      },
                    reset = {
                        appState.global.backgroundColor = Color.White
                        appState.colors = createColors(
                            appState.global.isDarkTheme,
                            appState.global.primaryColor,
                            appState.global.backgroundColor,
                            appState.global.onBackgroundColor
                        )
                        updateFlatLaf(
                            appState.global.isDarkTheme,
                            appState.global.backgroundColor.toAwt(),
                            appState.global.onBackgroundColor.toAwt()
                        )
                        appState.saveGlobalState()
                        selectBackgroundColor = false
                    },
                    appState = appState
                )
            }
            OutlinedButton(onClick = { selectOnBackgroundColor = true }, Modifier.padding(end = 80.dp)) {
                Text("设置前景颜色")
            }

            if(selectOnBackgroundColor){
                ColorPicker(
                    close = {selectOnBackgroundColor = false},
                    mode = "onBackground",
                    initColor = appState.global.onBackgroundColor,
                    confirm = { selectedColor ->
                        appState.global.onBackgroundColor = selectedColor
                        appState.colors = createColors(
                            appState.global.isDarkTheme,
                            appState.global.primaryColor,
                            appState.global.backgroundColor,
                            appState.global.onBackgroundColor
                        )
                        updateFlatLaf(
                            appState.global.isDarkTheme,
                            appState.global.backgroundColor.toAwt(),
                            appState.global.onBackgroundColor.toAwt()
                        )
                        appState.saveGlobalState()
                        selectOnBackgroundColor = false

                    },
                    reset = {
                        appState.global.onBackgroundColor = Color.Black
                        appState.colors = createColors(
                            appState.global.isDarkTheme,
                            appState.global.primaryColor,
                            appState.global.backgroundColor,
                            appState.global.onBackgroundColor
                        )
                        updateFlatLaf(
                            appState.global.isDarkTheme,
                            appState.global.backgroundColor.toAwt(),
                            appState.global.onBackgroundColor.toAwt()
                        )
                        appState.saveGlobalState()
                        selectOnBackgroundColor = false
                    },
                    appState = appState
                )
            }
        }

    }

}
