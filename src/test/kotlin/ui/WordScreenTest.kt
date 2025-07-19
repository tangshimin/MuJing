package ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import data.loadMutableVocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import player.rememberPlayerState
import state.GlobalData
import state.GlobalState
import state.rememberAppState
import ui.wordscreen.WordScreenData
import ui.wordscreen.WordScreenState
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试记忆单词界面
     */
    @OptIn(ExperimentalFoundationApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalSerializationApi::class,
        ExperimentalTestApi::class
    )
    @Test
    fun `Test WordScreen`(){
        // 使用CountDownLatch来等待UI完全加载
        val setupCompletedLatch = CountDownLatch(1)

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
            wordState.vocabulary = loadMutableVocabulary(wordState.vocabularyPath)
            // 设置词库的名称
            wordState.vocabularyName = "Vocabulary"

            // 使用DisposableEffect确保在主线程完成UI设置后触发latch
            DisposableEffect(Unit) {
                setupCompletedLatch.countDown()
                onDispose { }
            }

            App(
                appState = appState,
                wordState = wordState,
                playerState = rememberPlayerState()
            )
        }

        // 等待UI设置完成
        val uiInitialized = setupCompletedLatch.await(15, TimeUnit.SECONDS)
        assertTrue("初始化UI超时", uiInitialized)

        // 通过同步测试确保所有操作在同一个线程上执行
        runTestSequence()
    }

    /**
     * 执行测试序列
     */
    @OptIn(ExperimentalTestApi::class)
    private fun runTestSequence() {
        // 测试第一个单词的索引和内容
        composeTestRule.waitUntilExactlyOneExists(hasTestTag("Header"), 15000)
        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertTextEquals("1/96")

        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertTextEquals("the")

        // 点击单词激活切换按钮
        safeClickWithTestTag("Word")

        // 测试NextButton出现并点击
        composeTestRule.waitUntilExactlyOneExists(hasTestTag("NextButton"), 15000)

        // 使用精确的TestTag选择器切换到第二个单词
        safeClickWithTestTag("NextButton")

        // 确保UI完全更新到第二个单词
        composeTestRule.waitUntilExactlyOneExists(hasText("2/96"), 15000)
        composeTestRule.waitForIdle()

        // 测试第二个单词
        composeTestRule.onNode(hasTestTag("Word"))
            .assertExists()
            .assertTextEquals("be")

        composeTestRule.onNode(hasTestTag("Header"))
            .assertExists()
            .assertTextEquals("2/96")

        // 再次点击单词区域，确保激活按钮
        safeClickWithTestTag("Word")
        composeTestRule.waitForIdle()

        // 等待PreviousButton出现并点击
        composeTestRule.waitUntilExactlyOneExists(hasTestTag("PreviousButton"), 10000)
        safeClickWithTestTag("PreviousButton")

        // 等待回到第一个单词
        composeTestRule.waitUntilExactlyOneExists(hasText("1/96"), 15000)

        // 测试设置按钮
        composeTestRule.onNode(hasTestTag("SettingsButton"))
            .assertExists()

        // 打开侧边栏
        safeClickWithTestTag("SettingsButton")

        // 测试侧边栏
        composeTestRule.waitUntilExactlyOneExists(hasTestTag("WordScreenSidebar"), 15000)
        composeTestRule.onNode(hasTestTag("WordScreenSidebar"))
            .assertExists()

        // 测试侧边栏内容 - 简化检查，只检查几个关键项
        listOf("听写测试", "选择章节", "显示单词", "英文释义", "中文释义").forEach {
            composeTestRule.onNodeWithText(it).assertExists()
        }

        // 关闭侧边栏
        safeClickWithTestTag("SettingsButton")

        // 等待侧边栏消失
        composeTestRule.waitUntilDoesNotExist(hasTestTag("WordScreenSidebar"), 15000)
    }


    /**
     * 用TestTag安全点击，避免多个元素匹配问题
     */
    private fun safeClickWithTestTag(tag: String) {
        runBlocking {
            withContext(Dispatchers.Main) {
                // 使用TestTag是最精确的选择方式
                composeTestRule.onNode(hasTestTag(tag)).performClick()
                composeTestRule.waitForIdle()
            }
        }
    }
}
