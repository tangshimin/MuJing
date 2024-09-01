package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import data.loadMutableVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import player.rememberPlayerState
import state.GlobalData
import state.GlobalState
import state.rememberAppState
import ui.word.WordScreenData
import ui.word.WordScreenState
import java.awt.AWTException
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs

class WordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalFoundationApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalSerializationApi::class
    )
    @Before
    fun setUp(){
        // 设置测试环境
        composeTestRule.setContent {
            val appState = rememberAppState()
            // 初始化全局状态
            appState.global = GlobalState(GlobalData())
            // 初始化记忆单词界面的状态
            val wordState = remember{ WordScreenState(WordScreenData()) }
            // 设置词库的路径
            wordState.vocabularyPath = File("src/test/resources/Vocabulary.json").absolutePath
            // 加载词库
            wordState.vocabulary = loadMutableVocabulary( wordState.vocabularyPath)
            // 设置词库的名称
            wordState.vocabularyName = "Vocabulary"

            App(
                appState =appState,
                wordState = wordState,
                playerState = rememberPlayerState()
            )
        }
    }

    /**
     * 测试记忆单词界面
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Test WordScreen`(){
        Thread.sleep(5000)

        // 测试第一个单词的索引
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("1/96")
        // 测试第一个单词
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("the")
            // 模拟鼠标移动，激活单词切换按钮
            .performMouseInput { click() }
        Thread.sleep(5000)


        // 切换到第二个单词
        composeTestRule.onNode(hasTestTag("NextButton"))
            .assertExists()
            .performClick()
        Thread.sleep(5000)
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("be")
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("2/96")

        // 切换回第一个单词
        composeTestRule.onNode(hasTestTag("PreviousButton"))
            .assertExists()
            .performClick()
        Thread.sleep(5000)
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("the")
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("1/96")


        // 测试侧边栏
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .assertExists()
            .performClick()
        Thread.sleep(5000)
        composeTestRule.onNode(hasTestTag("WordScreenSidebar"))
            .assertExists()
        composeTestRule.onNode(hasText("听写测试")).assertIsDisplayed()
        composeTestRule.onNode(hasText("选择章节")).assertIsDisplayed()
        composeTestRule.onNode(hasText("显示单词")).assertIsDisplayed()
        composeTestRule.onNode(hasText("显示音标")).assertIsDisplayed()
        composeTestRule.onNode(hasText("显示词形")).assertIsDisplayed()
        composeTestRule.onNode(hasText("英文释义")).assertIsDisplayed()
        composeTestRule.onNode(hasText("中文释义")).assertIsDisplayed()
        composeTestRule.onNode(hasText("显示字幕")).assertIsDisplayed()
        composeTestRule.onNode(hasText("击键音效")).assertIsDisplayed()
        composeTestRule.onNode(hasText("提示音效")).assertIsDisplayed()
        composeTestRule.onNode(hasText("自动切换")).assertIsDisplayed()
        composeTestRule.onNode(hasText("外部字幕")).assertIsDisplayed()
        composeTestRule.onNode(hasText("抄写字幕")).assertIsDisplayed()
        composeTestRule.onNode(hasText("音量控制")).assertIsDisplayed()
        composeTestRule.onNode(hasText("发音设置")).assertIsDisplayed()
        // 关闭侧边栏
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .performClick()

        Thread.sleep(5000)
        composeTestRule.onNode(hasTestTag("WordScreenSidebar"))
            .assertDoesNotExist()
    }

    @Ignore
    @Test
    fun takeScreenshot() {
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .performClick()
        try {
            // 创建 Robot 实例
            val robot = Robot()

            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val centerX = screenSize.width / 2
            val centerY = screenSize.height / 2
            robot.mouseMove(centerX, centerY)
            Thread.sleep(5000)

            // 屏幕尺寸，不保留底部任务栏
            val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize.width, Toolkit.getDefaultToolkit().screenSize.height - 47)

            // 捕获屏幕图像
            val screenFullImage: BufferedImage = robot.createScreenCapture(screenRect)

            // 保存图像到文件
            ImageIO.write(screenFullImage, "PNG", File("src/test/resources/WordScreen-Actual.png"))

            val actual = File("src/test/resources/WordScreen-Actual.png")
            val reference = File("src/test/resources/WordScreen-Reference.png")

            // 使用更高级的图像比较库来比较两个文件
            val actualImage = ImageIO.read(actual)
            val referenceImage = ImageIO.read(reference)
            val diff = compareImages(actualImage, referenceImage)
            println("两个图像的差异：$diff")
            // 使用 contentEquals 比较两个文件的内容可能会遇到问题，有时候截屏会有一些细微的差异，
            // 一样的内容过一段时间截屏可能会有一些细微的差异，如果断言失败了，把 screenshot 文件删除，然后把 screenshot-actual 重命名为 screenshot
            // 然后再次运行测试，测试可能会成功。过一段时间（使用视频播放器播放一段视频）再运行测试可能又会失败。再过一段时间可能又会成功。
            assert(
                actual.readBytes().contentEquals(reference.readBytes())
                        || (diff < 0.0002085)
            ) { "截屏不一致！" }

            Files.delete(actual.toPath())
        } catch (ex: AWTException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
}

fun compareImages(img1: BufferedImage, img2: BufferedImage): Double {
    // 比较两个图像的差异，返回差异百分比
    val width = img1.width
    val height = img1.height
    var diff = 0L
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel1 = img1.getRGB(x, y)
            val pixel2 = img2.getRGB(x, y)
            diff += pixelDiff(pixel1, pixel2)
        }
    }
    val maxDiff = 3L * 255 * width * height
    return diff.toDouble() / maxDiff
}

fun pixelDiff(rgb1: Int, rgb2: Int): Int {
    val r1 = (rgb1 shr 16) and 0xff
    val g1 = (rgb1 shr 8) and 0xff
    val b1 = rgb1 and 0xff
    val r2 = (rgb2 shr 16) and 0xff
    val g2 = (rgb2 shr 8) and 0xff
    val b2 = rgb2 and 0xff
    return abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
}