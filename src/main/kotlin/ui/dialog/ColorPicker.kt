package ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.serialization.ExperimentalSerializationApi
import state.AppState
import ui.window.windowBackgroundFlashingOnCloseFixHack
import util.rememberMonospace
import javax.swing.JOptionPane

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun ColorPicker(
    close: () -> Unit,
    mode:String,
    initColor: Color,
    confirm: (Color) -> Unit,
    reset:() -> Unit,
    appState: AppState
) {

    var selectedColor by remember { mutableStateOf(initColor) }
    val title = when (mode) {
        "Background" -> "设置背景色"
        "onBackground" -> "设置前景色"
        else -> "设置主色调"
    }
    Window(
        onCloseRequest = { close() },
        title = title,
        resizable = false,
        icon = painterResource("logo/logo.png"),
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1080.dp, 795.dp)
        )
    ) {
        windowBackgroundFlashingOnCloseFixHack()
        MaterialTheme(colors = appState.colors) {
            val fontFamily  = rememberMonospace()


            Box(Modifier.fillMaxSize()
                .background(if(mode == "Background") selectedColor else MaterialTheme.colors.background)
            ) {


                Row(Modifier.fillMaxWidth().height(560.dp)) {
                    val width = 56.dp
                    val height = 50.dp
                    val selectColor: (Color) -> Unit = {
                        selectedColor = it
                    }

                    // Red 红色
                    RedColumn(selectColor = selectColor, width)
                    // Pink 粉红
                    PinkColumn(selectColor = selectColor, width)
                    // Purple 紫色
                    PurpleColumn(selectColor = selectColor, width)
                    // Deep Purple 深紫色
                    DeepPurpleColumn(selectColor = selectColor, width)
                    // Indigo  靛蓝
                    IndigoColumn(selectColor = selectColor, width)
                    // Blue 蓝色
                    BlueColumn(selectColor = selectColor, width)
                    // Light Blue 浅蓝
                    LightBlueColumn(selectColor = selectColor, width)
                    // Cyan 青色
                    CyanColumn(selectColor = selectColor, width)
                    // Teal 青绿色
                    TealColumn(selectColor = selectColor, width)
                    // Green 绿色
                    GreenColumn(selectColor = selectColor, width)
                    // Light Green 浅绿
                    LightGreenColumn(selectColor = selectColor, width)
                    // Lime 酸橙色
                    LimeColumn(selectColor = selectColor, width)
                    // Yellow 黄色
                    YellowColumn(selectColor = selectColor, width)
                    // Amber 琥珀色
                    AmberColumn(selectColor = selectColor, width)
                    // Orange  橙色
                    OrangeColumn(selectColor = selectColor, width)
                    // Deep Orange  深橙色
                    DeepOrangeColumn(selectColor = selectColor, width)
                    // Brown  棕色
                    BrownColumn(selectColor = selectColor, width, height)
                    // Gray 灰色
                    GrayColumn(selectColor = selectColor, width, height)
                    // Blue Gray  蓝灰色
                    BlueGrayColumn(selectColor = selectColor, width, height)
                }


                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth().height(220.dp)
                        .background(if(mode == "Background") selectedColor else MaterialTheme.colors.background)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
//                            .background(if(mode == "Background") selectedColor else MaterialTheme.colors.background)
                    ) {
                        val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                        val fontSize = 18.sp
                        Column(Modifier.width(120.dp).height(120.dp)) {
                            Box(Modifier.width(120.dp).height(86.2.dp).background(selectedColor))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(38.33.dp).fillMaxWidth()
                            ) {
                                Text("HEX ", fontSize = fontSize ,color =if(mode == "onBackground") selectedColor else MaterialTheme.colors.onBackground,)
                                BasicTextField(
                                    value = selectedColor.toHex(),
                                    onValueChange = {
                                        selectedColor = it.HextoColor(selectedColor)
                                    },
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                                    textStyle = TextStyle(
                                        fontSize = fontSize,
                                        color = MaterialTheme.colors.onBackground
                                    ),
                                    modifier = Modifier
                                        .width(86.2.dp)
                                        .border(border = border)
                                )
                            }
                        }

                        Spacer(Modifier.width(25.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.width(300.dp).height(120.dp)
                                .border(
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                                    )
                                )
                        ){
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(300.dp).height(70.dp)
                                    .background(if(mode == "Background") selectedColor else MaterialTheme.colors.background)

                            ) {
                                Text(
                                    fontSize = 2.em,
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(color = if(mode == "PrimaryColor") selectedColor else appState.global.primaryColor,
                                                fontFamily = fontFamily)
                                        ) {
                                            append("Lang")
                                        }

                                        withStyle(
                                            style = SpanStyle(color = Color.Red, fontFamily = fontFamily)
                                        ) {
                                            append("u")
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                color =if(mode == "onBackground") selectedColor else MaterialTheme.colors.onBackground,
                                                fontFamily = fontFamily
                                            )
                                        ) {
                                            append("age")
                                        }
                                    }
                                )
                                Spacer(Modifier.width(5.dp))
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "3", color = if(mode == "PrimaryColor") selectedColor else appState.global.primaryColor)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "1", color = Color.Red)
                                }
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.Filled.VolumeUp,
                                    contentDescription = "Localized description",
                                    tint = selectedColor,
                                    modifier = Modifier.padding(top = 8.dp),
                                )

                            }
                            Row( horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()){
                                Text(
                                    text = "美:læŋgwidʒ",
                                    color =if(mode == "onBackground") selectedColor else MaterialTheme.colors.onBackground,
                                    modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "英:'læŋɡwɪdʒ",
                                    color =if(mode == "onBackground") selectedColor else MaterialTheme.colors.onBackground,
                                    modifier = Modifier.padding(start = 5.dp, end = 5.dp)
                                )
                            }
                            }
                        }

                    val resetText = when (mode) {
                        "Background" -> "恢复默认背景色"
                        "onBackground" -> "恢复默认前景色"
                        else -> "恢复默认主色调"

                    }
                    val buttonColors =  ButtonDefaults.outlinedButtonColors(
                        contentColor = if(mode == "PrimaryColor") selectedColor else appState.global.primaryColor,
                            backgroundColor = if(mode == "Background") selectedColor else appState.global.backgroundColor)

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {reset()},
                            colors = buttonColors
                        ) {
                            Text(resetText)
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { confirm(selectedColor) },
                            colors = buttonColors
                        ) {
                            Text("确定")
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { close() },
                            colors = buttonColors
                        ) {
                            Text("取消")
                        }
                    }
                }
                }
            }

        }
    }



fun Color.toHex(): String {
    val argb = this.toArgb()
    return String.format("#%06X", (argb and 0xFFFFFF))
}

fun String.HextoColor(default: Color): Color {
    val colorString = if (this.startsWith("#")) this.substring(1) else this
    try {
        val colorInt = colorString.toInt(16)
        val r = (colorInt shr 16 and 0xff) / 255f
        val g = (colorInt shr 8 and 0xff) / 255f
        val b = (colorInt and 0xff) / 255f
        return Color(r, g, b)
    } catch (e: Exception) {
        JOptionPane.showMessageDialog(null, "颜色值不正确,请重新输入", "错误", JOptionPane.ERROR_MESSAGE)
        return default
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RedColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Red 红色
    val redColor = Color(244, 67, 54)
    var redColumnColor by remember { mutableStateOf(redColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("红色", color = Color.Red)
        Column(Modifier.width(width).fillMaxHeight().background(redColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                redColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                redColumnColor = redColor
            }
        ) {
            if (redColumnColor == Color.Transparent) {
                ColorBlock("50", Color(255, 235, 238), selectColor)
                ColorBlock("100", Color(255, 205, 210), selectColor)
                ColorBlock("200", Color(239, 154, 154), selectColor)
                ColorBlock("300", Color(229, 115, 115), selectColor)
                ColorBlock("400", Color(239, 83, 80), selectColor)
                ColorBlock("500", Color(244, 67, 54), selectColor)
                ColorBlock("600", Color(229, 57, 53), selectColor)
                ColorBlock("700", Color(211, 47, 47), selectColor)
                ColorBlock("800", Color(198, 40, 40), selectColor)
                ColorBlock("900", Color(183, 28, 28), selectColor)
                ColorBlock("A100", Color(255, 138, 128), selectColor)
                ColorBlock("A200", Color(255, 82, 82), selectColor)
                ColorBlock("A400", Color(255, 23, 68), selectColor)
                ColorBlock("A700", Color(213, 0, 0), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinkColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Pink 粉红
    val pinkColor = Color(233, 30, 99)
    var pinkColumnColor by remember { mutableStateOf(pinkColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("粉红", color = pinkColor)
        Column(Modifier.width(width).fillMaxHeight().background(pinkColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                pinkColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                pinkColumnColor = pinkColor
            }
        ) {
            if (pinkColumnColor == Color.Transparent) {
                ColorBlock("50", Color(252, 228, 236), selectColor)
                ColorBlock("100", Color(248, 187, 208), selectColor)
                ColorBlock("200", Color(244, 143, 177), selectColor)
                ColorBlock("300", Color(240, 98, 146), selectColor)
                ColorBlock("400", Color(236, 64, 122), selectColor)
                ColorBlock("500", Color(233, 30, 99), selectColor)
                ColorBlock("600", Color(216, 27, 96), selectColor)
                ColorBlock("700", Color(194, 24, 91), selectColor)
                ColorBlock("800", Color(173, 20, 87), selectColor)
                ColorBlock("900", Color(136, 14, 79), selectColor)
                ColorBlock("A100", Color(255, 128, 171), selectColor)
                ColorBlock("A200", Color(255, 64, 129), selectColor)
                ColorBlock("A400", Color(245, 0, 87), selectColor)
                ColorBlock("A700", Color(197, 17, 98), selectColor)
            }
        }
    }

}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PurpleColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Purple 紫色
    val purpleColor = Color(156, 39, 176)
    var purpleColumnColor by remember { mutableStateOf(purpleColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("紫色", color = purpleColor)
        Column(Modifier.width(width).fillMaxHeight().background(purpleColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                purpleColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                purpleColumnColor = purpleColor
            }
        ) {
            if (purpleColumnColor == Color.Transparent) {
                ColorBlock("50", Color(243, 229, 245), selectColor)
                ColorBlock("100", Color(225, 190, 231), selectColor)
                ColorBlock("200", Color(206, 147, 216), selectColor)
                ColorBlock("300", Color(186, 104, 200), selectColor)
                ColorBlock("400", Color(171, 71, 188), selectColor)
                ColorBlock("500", Color(156, 39, 176), selectColor)
                ColorBlock("600", Color(142, 36, 170), selectColor)
                ColorBlock("700", Color(123, 31, 162), selectColor)
                ColorBlock("800", Color(106, 27, 154), selectColor)
                ColorBlock("900", Color(74, 20, 140), selectColor)
                ColorBlock("A100", Color(234, 128, 252), selectColor)
                ColorBlock("A200", Color(224, 64, 251), selectColor)
                ColorBlock("A400", Color(213, 0, 249), selectColor)
                ColorBlock("A700", Color(170, 0, 255), selectColor)
            }
        }
    }


}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeepPurpleColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Deep Purple 深紫色
    val deepPurpleColor = Color(103, 58, 183)
    var deepPurpleColumnColor by remember { mutableStateOf(deepPurpleColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("深紫色", color = deepPurpleColor)
        Column(Modifier.width(width).fillMaxHeight().background(deepPurpleColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                deepPurpleColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                deepPurpleColumnColor = deepPurpleColor
            }
        ) {
            if (deepPurpleColumnColor == Color.Transparent) {
                ColorBlock("50", Color(237, 231, 246), selectColor)
                ColorBlock("100", Color(209, 196, 233), selectColor)
                ColorBlock("200", Color(179, 157, 219), selectColor)
                ColorBlock("300", Color(149, 117, 205), selectColor)
                ColorBlock("400", Color(126, 87, 194), selectColor)
                ColorBlock("500", Color(103, 58, 183), selectColor)
                ColorBlock("600", Color(94, 53, 177), selectColor)
                ColorBlock("700", Color(81, 45, 168), selectColor)
                ColorBlock("800", Color(69, 39, 160), selectColor)
                ColorBlock("900", Color(49, 27, 146), selectColor)
                ColorBlock("A100", Color(179, 136, 255), selectColor)
                ColorBlock("A200", Color(124, 77, 255), selectColor)
                ColorBlock("A400", Color(101, 31, 255), selectColor)
                ColorBlock("A700", Color(98, 0, 234), selectColor)
            }
        }
    }


}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IndigoColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Indigo  靛蓝
    val indigoColor = Color(63, 81, 181)
    var indigoColumnColor by remember { mutableStateOf(indigoColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("靛蓝", color = indigoColor)
        Column(Modifier.width(width).fillMaxHeight().background(indigoColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                indigoColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                indigoColumnColor = indigoColor
            }
        ) {
            if (indigoColumnColor == Color.Transparent) {
                ColorBlock("50", Color(232, 234, 246), selectColor)
                ColorBlock("100", Color(197, 202, 233), selectColor)
                ColorBlock("200", Color(159, 168, 218), selectColor)
                ColorBlock("300", Color(121, 134, 203), selectColor)
                ColorBlock("400", Color(92, 107, 192), selectColor)
                ColorBlock("500", Color(63, 81, 181), selectColor)
                ColorBlock("600", Color(57, 73, 171), selectColor)
                ColorBlock("700", Color(48, 63, 159), selectColor)
                ColorBlock("800", Color(40, 53, 147), selectColor)
                ColorBlock("900", Color(26, 35, 126), selectColor)
                ColorBlock("A100", Color(140, 158, 255), selectColor)
                ColorBlock("A200", Color(83, 109, 254), selectColor)
                ColorBlock("A400", Color(61, 90, 254), selectColor)
                ColorBlock("A700", Color(48, 79, 254), selectColor)
            }
        }
    }


}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BlueColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Blue 蓝色
    val blueColor = Color(33, 150, 243)
    var blueColumnColor by remember { mutableStateOf(blueColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("蓝色", color = blueColor)
        Column(Modifier.width(width).fillMaxHeight().background(blueColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                blueColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                blueColumnColor = blueColor
            }
        ) {
            if (blueColumnColor == Color.Transparent) {
                ColorBlock("50", Color(227, 242, 253), selectColor)
                ColorBlock("100", Color(187, 222, 251), selectColor)
                ColorBlock("200", Color(144, 202, 249), selectColor)
                ColorBlock("300", Color(100, 181, 246), selectColor)
                ColorBlock("400", Color(66, 165, 245), selectColor)
                ColorBlock("500", Color(33, 150, 243), selectColor)
                ColorBlock("600", Color(30, 136, 229), selectColor)
                ColorBlock("700", Color(25, 118, 210), selectColor)
                ColorBlock("800", Color(21, 101, 192), selectColor)
                ColorBlock("900", Color(13, 71, 161), selectColor)
                ColorBlock("A100", Color(130, 177, 255), selectColor)
                ColorBlock("A200", Color(68, 138, 255), selectColor)
                ColorBlock("A400", Color(41, 121, 255), selectColor)
                ColorBlock("A700", Color(41, 98, 255), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LightBlueColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Light Blue 浅蓝
    val lightBlueColor = Color(3, 169, 244)
    var lightBlueColumnColor by remember { mutableStateOf(lightBlueColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("浅蓝", color = lightBlueColor)
        Column(Modifier.width(width).fillMaxHeight().background(lightBlueColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                lightBlueColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                lightBlueColumnColor = lightBlueColor
            }
        ) {
            if (lightBlueColumnColor == Color.Transparent) {
                ColorBlock("50", Color(225, 245, 254), selectColor)
                ColorBlock("100", Color(179, 229, 252), selectColor)
                ColorBlock("200", Color(129, 212, 250), selectColor)
                ColorBlock("300", Color(79, 195, 247), selectColor)
                ColorBlock("400", Color(41, 182, 246), selectColor)
                ColorBlock("500", Color(3, 169, 244), selectColor)
                ColorBlock("600", Color(3, 155, 229), selectColor)
                ColorBlock("700", Color(2, 136, 209), selectColor)
                ColorBlock("800", Color(2, 119, 189), selectColor)
                ColorBlock("900", Color(1, 87, 155), selectColor)
                ColorBlock("A100", Color(128, 216, 255), selectColor)
                ColorBlock("A200", Color(64, 196, 255), selectColor)
                ColorBlock("A400", Color(0, 176, 255), selectColor)
                ColorBlock("A700", Color(0, 145, 234), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CyanColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Cyan 青色
    val cyanColor = Color(0, 188, 212)
    var cyanColumnColor by remember { mutableStateOf(cyanColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("青色", color = cyanColor)
        Column(Modifier.width(width).fillMaxHeight().background(cyanColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                cyanColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                cyanColumnColor = cyanColor
            }) {
            if (cyanColumnColor == Color.Transparent) {
                ColorBlock("50", Color(224, 247, 250), selectColor)
                ColorBlock("100", Color(178, 235, 242), selectColor)
                ColorBlock("200", Color(128, 222, 234), selectColor)
                ColorBlock("300", Color(77, 208, 225), selectColor)
                ColorBlock("400", Color(38, 198, 218), selectColor)
                ColorBlock("500", Color(0, 188, 212), selectColor)
                ColorBlock("600", Color(0, 172, 193), selectColor)
                ColorBlock("700", Color(0, 151, 167), selectColor)
                ColorBlock("800", Color(0, 131, 143), selectColor)
                ColorBlock("900", Color(0, 96, 100), selectColor)
                ColorBlock("A100", Color(132, 255, 255), selectColor)
                ColorBlock("A200", Color(24, 255, 255), selectColor)
                ColorBlock("A400", Color(0, 229, 255), selectColor)
                ColorBlock("A700", Color(0, 184, 212), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TealColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Teal 青绿色
    val tealColor = Color(0, 150, 136)
    var tealColumnColor by remember { mutableStateOf(tealColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("青绿色", color = tealColor)
        Column(Modifier.width(width).fillMaxHeight().background(tealColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                tealColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                tealColumnColor = tealColor
            }) {
            if (tealColumnColor == Color.Transparent) {
                ColorBlock("50", Color(224, 242, 241), selectColor)
                ColorBlock("100", Color(178, 223, 219), selectColor)
                ColorBlock("200", Color(128, 203, 196), selectColor)
                ColorBlock("300", Color(77, 182, 172), selectColor)
                ColorBlock("400", Color(38, 166, 154), selectColor)
                ColorBlock("500", Color(0, 150, 136), selectColor)
                ColorBlock("600", Color(0, 137, 123), selectColor)
                ColorBlock("700", Color(0, 121, 107), selectColor)
                ColorBlock("800", Color(0, 105, 92), selectColor)
                ColorBlock("900", Color(0, 77, 64), selectColor)
                ColorBlock("A100", Color(167, 255, 235), selectColor)
                ColorBlock("A200", Color(100, 255, 218), selectColor)
                ColorBlock("A400", Color(29, 233, 182), selectColor)
                ColorBlock("A700", Color(0, 191, 165), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GreenColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Green 绿色
    val greenColor = Color(76, 175, 80)
    var greenColumnColor by remember { mutableStateOf(greenColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("绿色", color = greenColor)
        Column(Modifier.width(width).fillMaxHeight().background(greenColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                greenColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                greenColumnColor = greenColor
            }) {
            if (greenColumnColor == Color.Transparent) {
                ColorBlock("50", Color(232, 245, 233), selectColor)
                ColorBlock("100", Color(200, 230, 201), selectColor)
                ColorBlock("200", Color(165, 214, 167), selectColor)
                ColorBlock("300", Color(129, 199, 132), selectColor)
                ColorBlock("400", Color(102, 187, 106), selectColor)
                ColorBlock("500", Color(76, 175, 80), selectColor)
                ColorBlock("600", Color(67, 160, 71), selectColor)
                ColorBlock("700", Color(56, 142, 60), selectColor)
                ColorBlock("800", Color(46, 125, 50), selectColor)
                ColorBlock("900", Color(27, 94, 32), selectColor)
                ColorBlock("A100", Color(185, 246, 202), selectColor)
                ColorBlock("A200", Color(105, 240, 174), selectColor)
                ColorBlock("A400", Color(0, 230, 118), selectColor)
                ColorBlock("A700", Color(0, 200, 83), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LightGreenColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Light Green 浅绿
    val lightGreenColor = Color(139, 195, 74)
    var lightGreenColumnColor by remember { mutableStateOf(lightGreenColor) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("浅绿", color = lightGreenColor)
        Column(Modifier.width(width).fillMaxHeight().background(lightGreenColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                lightGreenColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                lightGreenColumnColor = lightGreenColor
            }) {
            if (lightGreenColumnColor == Color.Transparent) {
                ColorBlock("50", Color(241, 248, 233), selectColor)
                ColorBlock("100", Color(220, 237, 200), selectColor)
                ColorBlock("200", Color(197, 225, 165), selectColor)
                ColorBlock("300", Color(174, 213, 129), selectColor)
                ColorBlock("400", Color(156, 204, 101), selectColor)
                ColorBlock("500", Color(139, 195, 74), selectColor)
                ColorBlock("600", Color(124, 179, 66), selectColor)
                ColorBlock("700", Color(104, 159, 56), selectColor)
                ColorBlock("800", Color(85, 139, 47), selectColor)
                ColorBlock("900", Color(51, 105, 30), selectColor)
                ColorBlock("A100", Color(204, 255, 144), selectColor)
                ColorBlock("A200", Color(178, 255, 89), selectColor)
                ColorBlock("A400", Color(118, 255, 3), selectColor)
                ColorBlock("A700", Color(100, 221, 23), selectColor)

            }
        }

    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LimeColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Lime 酸橙色
    val limeColor = Color(205, 220, 57)
    var limeColumnColor by remember { mutableStateOf(limeColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("酸橙色", color = limeColor)
        Column(Modifier.width(width).fillMaxHeight().background(limeColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                limeColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                limeColumnColor = limeColor
            }) {
            if (limeColumnColor == Color.Transparent) {
                ColorBlock("50", Color(249, 251, 231), selectColor)
                ColorBlock("100", Color(240, 244, 195), selectColor)
                ColorBlock("200", Color(230, 238, 156), selectColor)
                ColorBlock("300", Color(220, 231, 117), selectColor)
                ColorBlock("400", Color(212, 225, 87), selectColor)
                ColorBlock("500", Color(205, 220, 57), selectColor)
                ColorBlock("600", Color(192, 202, 51), selectColor)
                ColorBlock("700", Color(175, 180, 43), selectColor)
                ColorBlock("800", Color(158, 157, 36), selectColor)
                ColorBlock("900", Color(130, 119, 23), selectColor)
                ColorBlock("A100", Color(244, 255, 129), selectColor)
                ColorBlock("A200", Color(238, 255, 65), selectColor)
                ColorBlock("A400", Color(198, 255, 0), selectColor)
                ColorBlock("A700", Color(174, 234, 0), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YellowColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Yellow 黄色
    val yellowColor = Color(255, 235, 59)
    var yellowColumnColor by remember { mutableStateOf(yellowColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("黄色", color = yellowColor)
        Column(Modifier.width(width).fillMaxHeight().background(yellowColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                yellowColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                yellowColumnColor = yellowColor
            }) {
            if (yellowColumnColor == Color.Transparent) {
                ColorBlock("50", Color(255, 253, 231), selectColor)
                ColorBlock("100", Color(255, 249, 196), selectColor)
                ColorBlock("200", Color(255, 245, 157), selectColor)
                ColorBlock("300", Color(255, 241, 118), selectColor)
                ColorBlock("400", Color(255, 238, 88), selectColor)
                ColorBlock("500", Color(255, 235, 59), selectColor)
                ColorBlock("600", Color(253, 216, 53), selectColor)
                ColorBlock("700", Color(251, 192, 45), selectColor)
                ColorBlock("800", Color(249, 168, 37), selectColor)
                ColorBlock("900", Color(245, 127, 23), selectColor)
                ColorBlock("A100", Color(255, 255, 141), selectColor)
                ColorBlock("A200", Color(255, 255, 0), selectColor)
                ColorBlock("A400", Color(255, 234, 0), selectColor)
                ColorBlock("A700", Color(255, 214, 0), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AmberColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Amber 琥珀色
    val amberColor = Color(255, 193, 7)
    var amberColumnColor by remember { mutableStateOf(amberColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("琥珀色", color = amberColor)
        Column(Modifier.width(width).fillMaxHeight().background(amberColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                amberColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                amberColumnColor = amberColor
            }) {
            if (amberColumnColor == Color.Transparent) {
                ColorBlock("50", Color(255, 248, 225), selectColor)
                ColorBlock("100", Color(255, 236, 179), selectColor)
                ColorBlock("200", Color(255, 224, 130), selectColor)
                ColorBlock("300", Color(255, 213, 79), selectColor)
                ColorBlock("400", Color(255, 202, 40), selectColor)
                ColorBlock("500", Color(255, 193, 7), selectColor)
                ColorBlock("600", Color(255, 179, 0), selectColor)
                ColorBlock("700", Color(255, 160, 0), selectColor)
                ColorBlock("800", Color(255, 143, 0), selectColor)
                ColorBlock("900", Color(255, 111, 0), selectColor)
                ColorBlock("A100", Color(255, 229, 127), selectColor)
                ColorBlock("A200", Color(255, 215, 64), selectColor)
                ColorBlock("A400", Color(255, 196, 0), selectColor)
                ColorBlock("A700", Color(255, 171, 0), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OrangeColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Orange 橙色
    val orangeColor = Color(255, 152, 0)
    var orangeColumnColor by remember { mutableStateOf(orangeColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("橙色", color = orangeColor)
        Column(Modifier.width(width).fillMaxHeight().background(orangeColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                orangeColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                orangeColumnColor = orangeColor
            }) {
            if (orangeColumnColor == Color.Transparent) {
                ColorBlock("50", Color(255, 243, 224), selectColor)
                ColorBlock("100", Color(255, 224, 178), selectColor)
                ColorBlock("200", Color(255, 204, 128), selectColor)
                ColorBlock("300", Color(255, 183, 77), selectColor)
                ColorBlock("400", Color(255, 167, 38), selectColor)
                ColorBlock("500", Color(255, 152, 0), selectColor)
                ColorBlock("600", Color(251, 140, 0), selectColor)
                ColorBlock("700", Color(245, 124, 0), selectColor)
                ColorBlock("800", Color(239, 108, 0), selectColor)
                ColorBlock("900", Color(230, 81, 0), selectColor)
                ColorBlock("A100", Color(255, 209, 128), selectColor)
                ColorBlock("A200", Color(255, 171, 64), selectColor)
                ColorBlock("A400", Color(255, 145, 0), selectColor)
                ColorBlock("A700", Color(255, 109, 0), selectColor)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeepOrangeColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp
) {
    // Deep Orange 深橙色
    val deepOrangeColor = Color(255, 87, 34)
    var deepOrangeColumnColor by remember { mutableStateOf(deepOrangeColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("深橙色", color = deepOrangeColor)
        Column(Modifier.width(width).fillMaxHeight().background(deepOrangeColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                deepOrangeColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                deepOrangeColumnColor = deepOrangeColor
            }) {
            if (deepOrangeColumnColor == Color.Transparent) {
                ColorBlock("50", Color(251, 233, 231), selectColor)
                ColorBlock("100", Color(255, 204, 188), selectColor)
                ColorBlock("200", Color(255, 171, 145), selectColor)
                ColorBlock("300", Color(255, 138, 101), selectColor)
                ColorBlock("400", Color(255, 112, 67), selectColor)
                ColorBlock("500", Color(255, 87, 34), selectColor)
                ColorBlock("600", Color(244, 81, 30), selectColor)
                ColorBlock("700", Color(230, 74, 25), selectColor)
                ColorBlock("800", Color(216, 67, 21), selectColor)
                ColorBlock("900", Color(191, 54, 12), selectColor)
                ColorBlock("A100", Color(255, 158, 128), selectColor)
                ColorBlock("A200", Color(255, 110, 64), selectColor)
                ColorBlock("A400", Color(255, 61, 0), selectColor)
                ColorBlock("A700", Color(221, 44, 0), selectColor)

            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BrownColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp,
    height: Dp
) {
    // Brown 棕色
    val brownColor = Color(121, 85, 72)
    var brownColumnColor by remember { mutableStateOf(brownColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("棕色", color = brownColor)
        Column(Modifier.width(width).fillMaxHeight().background(brownColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                brownColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                brownColumnColor = brownColor
            }) {
            if (brownColumnColor == Color.Transparent) {
                ColorBlock("50", Color(239, 235, 233), selectColor, height)
                ColorBlock("100", Color(215, 204, 200), selectColor, height)
                ColorBlock("200", Color(188, 170, 164), selectColor, height)
                ColorBlock("300", Color(161, 136, 127), selectColor, height)
                ColorBlock("400", Color(141, 110, 99), selectColor, height)
                ColorBlock("500", Color(121, 85, 72), selectColor, height)
                ColorBlock("600", Color(109, 76, 65), selectColor, height)
                ColorBlock("700", Color(93, 64, 55), selectColor, height)
                ColorBlock("800", Color(78, 52, 46), selectColor, height)
                ColorBlock("900", Color(62, 39, 35), selectColor, height)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GrayColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp,
    height: Dp
) {
    // Gray 灰色
    val grayColor = Color(158, 158, 158)
    var grayColumnColor by remember { mutableStateOf(grayColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("灰色", color = grayColor)
        Column(Modifier.width(width).fillMaxHeight().background(grayColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                grayColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                grayColumnColor = grayColor
            }) {
            if (grayColumnColor == Color.Transparent) {
                ColorBlock("50", Color(250, 250, 250), selectColor, height)
                ColorBlock("100", Color(245, 245, 245), selectColor, height)
                ColorBlock("200", Color(238, 238, 238), selectColor, height)
                ColorBlock("300", Color(224, 224, 224), selectColor, height)
                ColorBlock("400", Color(189, 189, 189), selectColor, height)
                ColorBlock("500", Color(158, 158, 158), selectColor, height)
                ColorBlock("600", Color(117, 117, 117), selectColor, height)
                ColorBlock("700", Color(97, 97, 97), selectColor, height)
                ColorBlock("800", Color(66, 66, 66), selectColor, height)
                ColorBlock("900", Color(33, 33, 33), selectColor, height)
            }
        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BlueGrayColumn(
    selectColor: (Color) -> Unit = {},
    width: Dp,
    height: Dp
) {
    // Blue Gray 蓝灰色
    val blueGrayColor = Color(96, 125, 139)
    var blueGrayColumnColor by remember { mutableStateOf(blueGrayColor) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("蓝灰色", color = blueGrayColor)
        Column(Modifier.width(width).fillMaxHeight().background(blueGrayColumnColor)
            .onPointerEvent(PointerEventType.Enter) {
                blueGrayColumnColor = Color.Transparent
            }
            .onPointerEvent(PointerEventType.Exit) {
                blueGrayColumnColor = blueGrayColor
            }) {
            if (blueGrayColumnColor == Color.Transparent) {
                ColorBlock("50", Color(236, 239, 241), selectColor, height)
                ColorBlock("100", Color(207, 216, 220), selectColor, height)
                ColorBlock("200", Color(176, 190, 197), selectColor, height)
                ColorBlock("300", Color(144, 164, 174), selectColor, height)
                ColorBlock("400", Color(120, 144, 156), selectColor, height)
                ColorBlock("500", Color(96, 125, 139), selectColor, height)
                ColorBlock("600", Color(84, 110, 122), selectColor, height)
                ColorBlock("700", Color(69, 90, 100), selectColor, height)
                ColorBlock("800", Color(55, 71, 79), selectColor, height)
                ColorBlock("900", Color(38, 50, 56), selectColor, height)
            }
        }
    }

}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorBlock(
    colorID: String,
    color: Color,
    selectColor: (Color) -> Unit = {},
    height: Dp = 36.dp
) {
    val primaryColor = MaterialTheme.colors.primary
    var borderColor by remember { mutableStateOf(Color.Transparent) }
    Box(Modifier.fillMaxWidth().height(height).background(color)
        .clickable { selectColor(color) }
        .border(BorderStroke(3.dp, borderColor))
        .onPointerEvent(PointerEventType.Enter) {
            borderColor = primaryColor
        }
        .onPointerEvent(PointerEventType.Exit) {
            borderColor = Color.Transparent
        }) {
        Text(colorID, modifier = Modifier.align(Alignment.BottomStart).padding(start = 5.dp))
    }
}