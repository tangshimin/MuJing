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
    onDanmakuManagerCreated: (DanmakuStateManager) -> Unit = {}
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

    // 向外暴露弹幕管理器
    LaunchedEffect(danmakuManager) {
        onDanmakuManagerCreated(danmakuManager)
    }

    // 更新管理器配置
    LaunchedEffect(isEnabled, speed, maxDanmakuCount) {
        danmakuManager.isEnabled = isEnabled
        danmakuManager.speed = speed
        danmakuManager.maxDanmakuCount = maxDanmakuCount
    }

    // 定期清理不活跃的弹幕
    LaunchedEffect(danmakuManager) {
        while (true) {
            delay(1000) // 每秒清理一次
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
 * 演示用的弹幕容器，包含一些测试弹幕
 */
@Composable
fun DemoDanmakuContainer(
    modifier: Modifier = Modifier
) {
    var danmakuManager by remember { mutableStateOf<DanmakuStateManager?>(null) }

    CanvasDanmakuContainer(
        modifier = modifier,
        fontSize = 20,
        speed = 2f,
        onDanmakuManagerCreated = { manager ->
            danmakuManager = manager
        }
    )

    // 添加演示弹幕
    LaunchedEffect(danmakuManager) {
        danmakuManager?.let { manager ->
            delay(1000)
            manager.addDanmaku("Hello World!", color = Color.White)
            delay(2000)
            manager.addDanmaku("这是一条测试弹幕", color = Color.Yellow)
            delay(1500)
            manager.addDanmaku("Canvas Danmaku System", color = Color.Cyan)
            delay(3000)
            manager.addDanmaku("第一阶段基础渲染完成", color = Color.Green)
        }
    }
}
