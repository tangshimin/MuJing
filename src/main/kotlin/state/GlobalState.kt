package state

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** 全局的数据类 */
@ExperimentalSerializationApi
@Serializable
data class GlobalData(
    val type: ScreenType = ScreenType.WORD,
    val isDarkTheme: Boolean = true,
    val audioVolume: Float = 0.8F,
    val videoVolume: Float = 80F,
    val keystrokeVolume: Float = 0.75F,
    val isPlayKeystrokeSound: Boolean = false,
    val primaryColorValue: ULong = 18377412168996880384UL,
    val backgroundColorValue:ULong = 18446744069414584320UL,
    val onBackgroundColorValue:ULong = 18374686479671623680UL,
    val wordTextStyle: String = "H2",
    val detailTextStyle: String = "Body1",
    val letterSpacing: Float = 5F,
    val x:Float = 100F,
    val y:Float = 100F,
    val width:Float = 1030F,
    val height:Float = 862F,
    val placement:WindowPlacement = WindowPlacement.Maximized,
    val autoUpdate:Boolean = true,
    val ignoreVersion:String = "",
    val bnc:Int = 1000,
    val frq:Int = 1000,
    val maxSentenceLength:Int = 25,
)

/** 全局状态的需要持久化的部分 */
@OptIn(ExperimentalSerializationApi::class)
class GlobalState(globalData: GlobalData) {
    /**
     * 练习的类型
     */
    var type by mutableStateOf(globalData.type)

    /**
     * 是否是深色模式
     */
    var isDarkTheme by mutableStateOf(globalData.isDarkTheme)

    /**
     * 单词发音的音量
     */
    var audioVolume by mutableStateOf(globalData.audioVolume)

    /**
     * 视频播放的音量
     */
    var videoVolume by mutableStateOf(globalData.videoVolume)

    /**
     * 按键音效音量
     */
    var keystrokeVolume by mutableStateOf(globalData.keystrokeVolume)

    /**
     * 是否播放按键音效
     */
    var isPlayKeystrokeSound by mutableStateOf(globalData.isPlayKeystrokeSound)

    /**
     * 主色调，默认为绿色
     */
    var primaryColor by mutableStateOf(Color(globalData.primaryColorValue))

    /**
     * 浅色主题的背景色
     */
    var backgroundColor by mutableStateOf(Color(globalData.backgroundColorValue))

    /**
     * 浅色主题的背景色
     */
    var onBackgroundColor by mutableStateOf(Color(globalData.onBackgroundColorValue))

    /**
     * 单词的字体样式，需要持久化
     */
    var wordTextStyle by mutableStateOf(globalData.wordTextStyle)

    /**
     * 详细信息的字体样式，需要持久化
     */
    var detailTextStyle by mutableStateOf(globalData.detailTextStyle)

    /**
     * 单词的字体大小，不用持久化
     */
    var wordFontSize by mutableStateOf(TextUnit.Unspecified)

    /**
     * 详细信息的的字体大小，不用持久化
     */
    var detailFontSize by mutableStateOf(TextUnit.Unspecified)

    /**
     *  字间隔空
     */
    var letterSpacing by mutableStateOf((globalData.letterSpacing).sp)

    /**
     * 主窗口的位置
     */
    var position by mutableStateOf(WindowPosition(globalData.x.dp,globalData.y.dp))

    /**
     * 主窗口的尺寸
     */
    var size by mutableStateOf(DpSize(globalData.width.dp,globalData.height.dp))

    /**
     * 描述如何放置窗口在屏幕
     */
    var placement by mutableStateOf(globalData.placement)

    /**
     * 自动检查更新
     */
    var autoUpdate by mutableStateOf(globalData.autoUpdate)

    /**
     * 忽略的版本
     */
    var ignoreVersion by mutableStateOf(globalData.ignoreVersion)

    /**
     * 过滤 BNC 词频最常见的单词数量，默认为 1000
     */
    var bncNum by mutableStateOf(globalData.bnc)

    /**
     * 过滤 COCA 词频最常见的单词数量，默认为 1000
     */
    var frqNum by mutableStateOf(globalData.frq)

    /**
     * 单词所在句子的最大单词数, 默认为 25
     */
    var maxSentenceLength by mutableStateOf(globalData.maxSentenceLength)

}
@Composable
 fun computeFontSize(textStyle: String): TextUnit {
   return when(textStyle){
        "H1" ->{
            MaterialTheme.typography.h1.fontSize
        }
        "H2" ->{
            MaterialTheme.typography.h2.fontSize
        }
        "H3" ->{
            MaterialTheme.typography.h3.fontSize
        }
        "H4" ->{
            MaterialTheme.typography.h4.fontSize
        }
        "H5" ->{
            MaterialTheme.typography.h5.fontSize
        }
        "H6" ->{
            MaterialTheme.typography.h6.fontSize
        }
        "Subtitle1" ->{
            MaterialTheme.typography.subtitle1.fontSize
        }
        "Subtitle2" ->{
            MaterialTheme.typography.subtitle2.fontSize
        }
        "Body1" ->{
            MaterialTheme.typography.body1.fontSize
        }
        "Body2" ->{
            MaterialTheme.typography.body2.fontSize
        }
        "Caption" ->{
            MaterialTheme.typography.caption.fontSize
        }
        "Overline" ->{
            MaterialTheme.typography.overline.fontSize
        }
        else ->{ MaterialTheme.typography.h2.fontSize
        }

    }
}
