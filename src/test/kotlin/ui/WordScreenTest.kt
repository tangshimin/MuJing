package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import data.loadMutableVocabulary
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import player.rememberPlayerState
import state.GlobalData
import state.GlobalState
import state.rememberAppState
import ui.word.WordScreenData
import ui.word.WordScreenState
import java.io.File

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

        // 等待 Header 出现
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("Header"),10000)

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
            .isDisplayed()

        composeTestRule.waitForIdle()
        // 模拟鼠标移动，激活单词切换按钮
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("Word"))
                .performMouseInput { click() }
        }


        // 等待 NextButton 出现
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("NextButton"),10000)
        // 测试 NextButton 按钮
        composeTestRule.onNode(hasTestTag("NextButton"))
            .assertExists()
            .isDisplayed()

        // 切换到第二个单词
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("NextButton"))
                .assertExists()
                .performClick()
        }
        composeTestRule.waitForIdle()

        // 等待第二个单词出现
        composeTestRule.waitUntilExactlyOneExists (hasText("2/96"),10000)
        // 测试第二个单词
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("be")
            .isDisplayed()

        // 测试第二个单词的索引
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("2/96")
            .isDisplayed()


        // 再一次模拟鼠标移动，激活单词切换按钮
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("Word"))
                .performMouseInput { click() }
        }

        // 等待 PreviousButton 出现
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("PreviousButton"),10000)
        // 测试 PreviousButton 按钮
        composeTestRule.onNode(hasTestTag("PreviousButton"))
            .assertExists()
            .isDisplayed()


        // 切换回第一个单词
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("PreviousButton"))
                .assertExists()
                .performClick()
        }
        composeTestRule.waitForIdle()

        // 等待第一个单词出现
        composeTestRule.waitUntilExactlyOneExists (hasText("1/96"),10000)
        // 测试第一个单词
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("the")
            .isDisplayed()

        // 测试第一个单词的索引
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertIsDisplayed()
            .assertTextEquals("1/96")
            .isDisplayed()



        // 测试设置按钮
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .assertExists()
            .isDisplayed()

        // 打开侧边栏
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("SettingsButton"))
                .assertExists()
                .performClick()
        }
        // 等待侧边栏出现
        composeTestRule.waitUntilExactlyOneExists (hasTestTag("WordScreenSidebar"),10000)
        // 测试侧边栏
        composeTestRule.onNode(hasTestTag("WordScreenSidebar"))
            .assertExists()
            .isDisplayed()

        // 测试侧边栏的内容
        composeTestRule.onNode(hasText("听写测试")).isDisplayed()
        composeTestRule.onNode(hasText("选择章节")).isDisplayed()
        composeTestRule.onNode(hasText("显示单词")).isDisplayed()
        composeTestRule.onNode(hasText("显示音标")).isDisplayed()
        composeTestRule.onNode(hasText("显示词形")).isDisplayed()
        composeTestRule.onNode(hasText("英文释义")).isDisplayed()
        composeTestRule.onNode(hasText("中文释义")).isDisplayed()
        composeTestRule.onNode(hasText("显示字幕")).isDisplayed()
        composeTestRule.onNode(hasText("击键音效")).isDisplayed()
        composeTestRule.onNode(hasText("提示音效")).isDisplayed()
        composeTestRule.onNode(hasText("自动切换")).isDisplayed()
        composeTestRule.onNode(hasText("外部字幕")).isDisplayed()
        composeTestRule.onNode(hasText("抄写字幕")).isDisplayed()
        composeTestRule.onNode(hasText("音量控制")).isDisplayed()
        composeTestRule.onNode(hasText("发音设置")).isDisplayed()

        // 关闭侧边栏
        composeTestRule.runOnIdle {
            composeTestRule.onNode(hasTestTag("SettingsButton"))
                .performClick()
        }
        // 等待侧边栏消失
        composeTestRule.waitUntilDoesNotExist(hasTestTag("WordScreenSidebar"),10000)


    }


}
