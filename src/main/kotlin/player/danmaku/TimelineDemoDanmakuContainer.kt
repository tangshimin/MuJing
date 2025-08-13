package player.danmaku

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

/**
 * 第三阶段演示容器
 * 展示时间轴同步功能
 */
@Composable
fun TimelineDemoDanmakuContainer(
    modifier: Modifier = Modifier
) {
    var danmakuManager by remember { mutableStateOf<DanmakuStateManager?>(null) }
    var timelineSynchronizer by remember { mutableStateOf<TimelineSynchronizer?>(null) }

    // 模拟媒体播放时间
    var currentTimeMs by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // 创建媒体时间流
    val mediaTimeFlow = remember {
        flow {
            while (true) {
                if (isPlaying) {
                    emit(currentTimeMs)
                    currentTimeMs += 100 // 每100ms更新一次时间
                }
                delay(100)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 控制面板
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🕐 第三阶段：时间轴同步演示",
                    style = MaterialTheme.typography.h6
                )

                Text("当前时间: ${formatTime(currentTimeMs)}")

                // 进度条
                Slider(
                    value = currentTimeMs.toFloat(),
                    onValueChange = { currentTimeMs = it.toLong() },
                    valueRange = 0f..60000f, // 60秒
                    modifier = Modifier.fillMaxWidth()
                )

                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isPlaying = !isPlaying }
                    ) {
                        Text(if (isPlaying) "暂停" else "播放")
                    }

                    Button(
                        onClick = {
                            currentTimeMs = 0L
                            danmakuManager?.resetTimeline()
                        }
                    ) {
                        Text("重置")
                    }

                    Button(
                        onClick = {
                            currentTimeMs = 30000L // 跳转到30秒
                        }
                    ) {
                        Text("跳转30s")
                    }

                    Button(
                        onClick = {
                            // 加载演示弹幕数据
                            loadDemoTimelineDanmakus(timelineSynchronizer)
                        }
                    ) {
                        Text("加载弹幕")
                    }
                }
            }
        }

        // 弹幕显示区域
//        CanvasDanmakuContainer(
//            modifier = Modifier.fillMaxSize(),
//            fontSize = 20,
//            speed = 2f,
//            maxDanmakuCount = 30,
//            mediaTimeFlow = mediaTimeFlow,
//            onDanmakuManagerCreated = { manager ->
//                danmakuManager = manager
//            },
//            onTimelineSynchronizerCreated = { synchronizer ->
//                timelineSynchronizer = synchronizer
//                // 自动加载演示数据
//                loadDemoTimelineDanmakus(synchronizer)
//            }
//        )
    }
}

/**
 * 加载演示用的时间轴弹幕数据
 */
private fun loadDemoTimelineDanmakus(synchronizer: TimelineSynchronizer?) {
    synchronizer?.let { sync ->
        val demoData = listOf(
            // 第一段：开场介绍 (0-10秒)
            TimelineSynchronizer.TimedDanmakuData(1000, "🎬 时间轴同步演示开始", color = Color(0xFF4CAF50)),
            TimelineSynchronizer.TimedDanmakuData(2000, "这些弹幕会在特定时间点出现", color = Color(0xFF2196F3)),
            TimelineSynchronizer.TimedDanmakuData(3500, "观察时间轴同步效果", color = Color(0xFF9C27B0)),
            TimelineSynchronizer.TimedDanmakuData(5000, "可以拖拽进度条测试", color = Color(0xFFFF9800)),
            TimelineSynchronizer.TimedDanmakuData(6500, "也可以暂停和播放", color = Color(0xFF00BCD4)),
            TimelineSynchronizer.TimedDanmakuData(8000, "跳转功能也完全支持", color = Color(0xFFE91E63)),

            // 第二段：功能展示 (10-20秒)
            TimelineSynchronizer.TimedDanmakuData(10000, "⚡ 第二段：快进测试", color = Color(0xFFFF5722)),
            TimelineSynchronizer.TimedDanmakuData(11000, "这里有密集的弹幕", color = Color(0xFF795548)),
            TimelineSynchronizer.TimedDanmakuData(11200, "测试1", color = Color(0xFFE91E63)),
            TimelineSynchronizer.TimedDanmakuData(11400, "测试2", color = Color(0xFF9C27B0)),
            TimelineSynchronizer.TimedDanmakuData(11600, "测试3", color = Color(0xFF3F51B5)),
            TimelineSynchronizer.TimedDanmakuData(11800, "测试4", color = Color(0xFF2196F3)),
            TimelineSynchronizer.TimedDanmakuData(12000, "测试5", color = Color(0xFF00BCD4)),
            TimelineSynchronizer.TimedDanmakuData(12500, "观察快进时的处理", color = Color(0xFF4CAF50)),
            TimelineSynchronizer.TimedDanmakuData(13000, "所有弹幕都按时间排序", color = Color(0xFFFF9800)),
            TimelineSynchronizer.TimedDanmakuData(14000, "支持毫秒级精度", color = Color(0xFF607D8B)),
            TimelineSynchronizer.TimedDanmakuData(15000, "二分查找算法优化", color = Color(0xFF9E9E9E)),

            // 第三段：长文本测试 (20-30秒)
            TimelineSynchronizer.TimedDanmakuData(20000, "📝 第三段：长文本测试", color = Color(0xFF8BC34A)),
            TimelineSynchronizer.TimedDanmakuData(21000, "这是一条很长很长的弹幕，用来测试时间轴同步系统如何处理长文本内容", color = Color(0xFFCDDC39)),
            TimelineSynchronizer.TimedDanmakuData(22500, "中英混合 Mixed Text 测试", color = Color(0xFFFFC107)),
            TimelineSynchronizer.TimedDanmakuData(24000, "Unicode emoji 测试: 🚀⚡🔥💫🎯", color = Color(0xFFFF9800)),
            TimelineSynchronizer.TimedDanmakuData(25500, "Internationalization", color = Color(0xFFFF5722)),
            TimelineSynchronizer.TimedDanmakuData(27000, "时间轴系统性能测试", color = Color(0xFFE91E63)),

            // 第四段：压力测试 (30-40秒)
            TimelineSynchronizer.TimedDanmakuData(30000, "💥 第四段：压力测试", color = Color(0xFFD32F2F)),
            TimelineSynchronizer.TimedDanmakuData(31000, "密集弹幕1", color = Color(0xFFC62828)),
            TimelineSynchronizer.TimedDanmakuData(31100, "密集弹幕2", color = Color(0xFFAD1457)),
            TimelineSynchronizer.TimedDanmakuData(31200, "密集弹幕3", color = Color(0xFF8E24AA)),
            TimelineSynchronizer.TimedDanmakuData(31300, "密集弹幕4", color = Color(0xFF5E35B1)),
            TimelineSynchronizer.TimedDanmakuData(31400, "密集弹幕5", color = Color(0xFF3949AB)),
            TimelineSynchronizer.TimedDanmakuData(31500, "密集弹幕6", color = Color(0xFF1E88E5)),
            TimelineSynchronizer.TimedDanmakuData(31600, "密集弹幕7", color = Color(0xFF039BE5)),
            TimelineSynchronizer.TimedDanmakuData(31700, "密集弹幕8", color = Color(0xFF00ACC1)),
            TimelineSynchronizer.TimedDanmakuData(31800, "密集弹幕9", color = Color(0xFF00897B)),
            TimelineSynchronizer.TimedDanmakuData(31900, "密集弹幕10", color = Color(0xFF43A047)),
            TimelineSynchronizer.TimedDanmakuData(33000, "轨道管理 + 时间同步", color = Color(0xFF689F38)),
            TimelineSynchronizer.TimedDanmakuData(35000, "完美配合工作", color = Color(0xFF7CB342)),
            TimelineSynchronizer.TimedDanmakuData(37000, "性能表现优异", color = Color(0xFF8BC34A)),

            // 第五段：结束 (40-50秒)
            TimelineSynchronizer.TimedDanmakuData(40000, "🎉 第五段：演示结束", color = Color(0xFF9CCC65)),
            TimelineSynchronizer.TimedDanmakuData(42000, "时间轴同步功能完成", color = Color(0xFFAED581)),
            TimelineSynchronizer.TimedDanmakuData(44000, "支持播放控制", color = Color(0xFFC5E1A5)),
            TimelineSynchronizer.TimedDanmakuData(46000, "支持时间跳跃", color = Color(0xFFDCEDC8)),
            TimelineSynchronizer.TimedDanmakuData(48000, "支持进度拖拽", color = Color(0xFFF1F8E9)),
            TimelineSynchronizer.TimedDanmakuData(50000, "✨ 第三阶段完成!", color = Color(0xFF8BC34A))
        )

        sync.loadTimedDanmakus(demoData)
    }
}

/**
 * 格式化时间显示
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val milliseconds = (timeMs % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}
