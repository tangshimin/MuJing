package player.danmaku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

/**
 * Canvas 弹幕容器
 * 整合了弹幕状态管理器和 Canvas 渲染器
 */
@Composable
fun CanvasDanmakuContainer(
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Default,
    fontSize: Int = 18,
    isEnabled: Boolean = true,
    speed: Float = 3f,
    maxDanmakuCount: Int = 50,
    mediaTimeFlow: Flow<Long>? = null, // 媒体时间流
    onDanmakuManagerCreated: (DanmakuStateManager) -> Unit = {},
    onTimelineSynchronizerCreated: (TimelineSynchronizer) -> Unit = {} // 时间轴同步器回调
) {
    val density = LocalDensity.current
    val lineHeight = with(density) { (fontSize + 8).dp.toPx() }

    // 创建弹幕状态管理器
    val danmakuManager = remember {
        DanmakuStateManager().apply {
            this.speed = speed
            this.maxDanmakuCount = maxDanmakuCount
            this.isEnabled = isEnabled
        }
    }

    // 创建时间轴同步器（如果提供了媒体时间流）
    val timelineSynchronizer = remember(mediaTimeFlow) {
        if (mediaTimeFlow != null) {
            danmakuManager.initializeTimelineSync(mediaTimeFlow)
        } else null
    }

    // 向外暴露弹幕管理器和时间轴同步器
    LaunchedEffect(danmakuManager) {
        onDanmakuManagerCreated(danmakuManager)
    }

    LaunchedEffect(timelineSynchronizer) {
        timelineSynchronizer?.let { onTimelineSynchronizerCreated(it) }
    }

    // 监听媒体时间变化
    LaunchedEffect(mediaTimeFlow) {
        mediaTimeFlow?.collect { timeMs ->
            danmakuManager.updateMediaTime(timeMs)
        }
    }

    // 更新管理器配置
    LaunchedEffect(isEnabled, speed, maxDanmakuCount) {
        danmakuManager.isEnabled = isEnabled
        danmakuManager.speed = speed
        danmakuManager.maxDanmakuCount = maxDanmakuCount
    }

    // 定期清理不活跃的弹幕并处理等待队列
    LaunchedEffect(danmakuManager) {
        while (true) {
            delay(500) // 每0.5秒清理一次，提高响应性
            danmakuManager.cleanup()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                danmakuManager.setCanvasSize(size.width.toFloat(), size.height.toFloat())
                danmakuManager.setLineHeight(lineHeight)
            }
    ) {
        CanvasDanmakuRenderer(
            danmakuItems = danmakuManager.activeDanmakus,
            fontFamily = fontFamily,
            fontSize = fontSize,
            speed = speed,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 演示用的弹幕容器，包含复杂的测试弹幕
 * 展示第二阶段轨道管理与碰撞避免功能
 */
@Composable
fun DemoDanmakuContainer(
    modifier: Modifier = Modifier
) {
    var danmakuManager by remember { mutableStateOf<DanmakuStateManager?>(null) }

    CanvasDanmakuContainer(
        modifier = modifier,
        fontSize = 18,
        speed = 2.5f,
        maxDanmakuCount = 40, // 增加最大弹幕数量以展示轨道管理
        onDanmakuManagerCreated = { manager ->
            danmakuManager = manager
        }
    )

    // 复杂的演示弹幕序列
    LaunchedEffect(danmakuManager) {
        danmakuManager?.let { manager ->
            delay(1000)

            // 第一阶段：基础轨道分配演示
            manager.addDanmaku("🎯 轨道管理演示开始", color = Color(0xFF4CAF50))
            delay(800)

            // 快速添加多条弹幕，测试轨道分配
            val colors = listOf(
                Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5),
                Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFFFF9800)
            )

            repeat(8) { i ->
                manager.addDanmaku(
                    text = "轨道 #${i + 1} - 有序排列测试",
                    color = colors[i % colors.size]
                )
                delay(400) // 间隔添加，观察轨道分配效果
            }

            delay(2000)

            // 第二阶段：等待队列机制演示
            manager.addDanmaku("📋 等待队列机制演示", color = Color(0xFFFF5722))
            delay(500)

            // 快速批量添加，触发等待队列
            repeat(15) { i ->
                manager.addDanmaku(
                    text = "批量弹幕 #$i ${if (i % 2 == 0) "🔥" else "⚡"}",
                    color = Color(
                        red = 0.3f + (i * 0.05f),
                        green = 0.6f + (i * 0.03f),
                        blue = 0.9f - (i * 0.04f),
                        alpha = 1f
                    )
                )
                delay(50) // 非常快速添加，大部分会进入等待队列
            }

            delay(3000)

            // 第三阶段：长短文本混合测试
            manager.addDanmaku("📏 长短文本混合测试", color = Color(0xFF607D8B))
            delay(800)

            val testTexts = listOf(
                "短" to Color(0xFF4CAF50),
                "中等长度的测试弹幕" to Color(0xFF2196F3),
                "这是一条非常非常长的弹幕文本，用来测试轨道管理器如何处理不同长度的内容 🚀" to Color(0xFFE91E63),
                "Hello" to Color(0xFFFF9800),
                "Mixed 中英文混合 Text Testing" to Color(0xFF9C27B0),
                "Unicode 测试: 🎮🎯🚀⚡🔥💫" to Color(0xFF00BCD4),
                "超长英文单词测试 Internationalization" to Color(0xFFFF5722)
            )

            testTexts.forEach { (text, color) ->
                manager.addDanmaku(text = text, color = color)
                delay(600)
            }

            delay(4000)

            // 第四阶段：高密度弹幕压力测试
            manager.addDanmaku("💥 高密度压力测试", color = Color(0xFFD32F2F))
            delay(1000)

            // 模拟高峰期弹幕
            val peakMessages = listOf(
                "666", "牛逼", "Amazing!", "太强了", "🔥🔥🔥",
                "Nice!", "厉害", "Awesome", "棒棒哒", "Perfect!",
                "哈哈哈", "笑死", "有趣", "Funny", "LOL",
                "加油", "Come on", "Fighting", "赞", "👍"
            )

            repeat(25) { i ->
                manager.addDanmaku(
                    text = peakMessages[i % peakMessages.size] + " #$i",
                    color = Color(
                        red = Random.nextFloat() * 0.6f + 0.4f,
                        green = Random.nextFloat() * 0.8f + 0.2f,
                        blue = Random.nextFloat() * 0.9f + 0.1f,
                        alpha = 1f
                    )
                )
                delay(Random.nextLong(20, 150)) // 随机间隔，模拟真实用户行为
            }

            delay(5000)

            // 第五阶段：轨道释放与复用演示
            manager.addDanmaku("🔄 轨道释放与复用演示", color = Color(0xFF795548))
            delay(1000)

            // 添加一些快速通过的短弹幕
            repeat(12) { i ->
                manager.addDanmaku(
                    text = "快速通过 $i",
                    color = Color(0xFF009688)
                )
                delay(200)
            }

            delay(2000)

            // 然后添加一些慢速长弹幕，测试轨道复用
            repeat(6) { i ->
                manager.addDanmaku(
                    text = "慢速长弹幕复用轨道测试 #$i - 观察轨道如何被重新分配和使用",
                    color = Color(0xFF8BC34A)
                )
                delay(1000)
            }

            delay(3000)

            // 最终演示
            manager.addDanmaku("✨ 第二阶段轨道管理演示完成!", color = Color(0xFFFFD700))
            delay(1500)
            manager.addDanmaku("🎉 弹幕系统运行正常", color = Color(0xFF32CD32))
        }
    }
}
