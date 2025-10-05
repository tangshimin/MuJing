/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package player.danmaku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 视频标注演示容器
 * 展示在任意位置添加标注弹幕的功能
 */
@Composable
fun VideoAnnotationDemoContainer(
    modifier: Modifier = Modifier
) {
    var danmakuManager by remember { mutableStateOf<DanmakuStateManager?>(null) }
    var isAnnotationMode by remember { mutableStateOf(false) }
    var currentAnnotationText by remember { mutableStateOf("标注文字") }

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
                    text = "🎯 视频标注演示",
                    style = MaterialTheme.typography.h6
                )

                Text(
                    text = if (isAnnotationMode) "点击视频区域添加标注" else "选择功能或开启标注模式",
                    style = MaterialTheme.typography.body2,
                    color = if (isAnnotationMode) Color.Red else Color.Gray
                )

                // 标注文字输入
                OutlinedTextField(
                    value = currentAnnotationText,
                    onValueChange = { currentAnnotationText = it },
                    label = { Text("标注文字") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isAnnotationMode = !isAnnotationMode },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isAnnotationMode) Color.Red else MaterialTheme.colors.primary
                        )
                    ) {
                        Text(if (isAnnotationMode) "退出标注" else "标注模式")
                    }

                    Button(
                        onClick = {
                            // 添加随机位置的标注
                            danmakuManager?.addAnnotationDanmakuRelative(
                                text = "随机标注 ${Random.nextInt(100)}",
                                xPercent = Random.nextFloat(),
                                yPercent = Random.nextFloat(),
                                color = Color(
                                    red = Random.nextFloat(),
                                    green = Random.nextFloat(),
                                    blue = Random.nextFloat(),
                                    alpha = 1f
                                ),
                                durationMs = 4000L
                            )
                        }
                    ) {
                        Text("随机标注")
                    }

                    Button(
                        onClick = {
                            // 预设标注演示
                            danmakuManager?.let { manager ->
                                // 模拟标注视频中的不同对象
                                manager.addAnnotationDanmakuRelative("人物A", 0.2f, 0.3f, color = Color.Yellow, durationMs = 5000L)
                                manager.addAnnotationDanmakuRelative("建筑物", 0.7f, 0.2f, color = Color.Blue, durationMs = 5000L)
                                manager.addAnnotationDanmakuRelative("车辆", 0.5f, 0.8f, color = Color.Green, durationMs = 5000L)
                                manager.addAnnotationDanmakuRelative("重要场景", 0.5f, 0.1f, color = Color.Red, durationMs = 6000L)
                            }
                        }
                    ) {
                        Text("场景演示")
                    }

                    Button(
                        onClick = {
                            // 清除所有标注（通过添加普通弹幕来模拟清除效果）
                            danmakuManager?.addTopDanmaku("已清除所有标注", color = Color.Gray, durationMs = 2000L)
                        }
                    ) {
                        Text("清除")
                    }
                }
            }
        }

        // 可点击的视频区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isAnnotationMode) {
                    if (isAnnotationMode) {
                        detectTapGestures { offset ->
                            // 在点击位置添加标注
                            danmakuManager?.addAnnotationDanmaku(
                                text = currentAnnotationText,
                                x = offset.x,
                                y = offset.y,
                                color = Color(
                                    red = Random.nextFloat() * 0.8f + 0.2f,
                                    green = Random.nextFloat() * 0.8f + 0.2f,
                                    blue = Random.nextFloat() * 0.8f + 0.2f,
                                    alpha = 1f
                                ),
                                durationMs = 5000L
                            )
                        }
                    }
                }
        ) {
            // 背景模拟视频画面
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // 绘制模拟的视频背景
                drawRect(
                    color = Color(0xFF2C2C2C),
                    size = size
                )

                // 绘制一些模拟的视频元素
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 40f,
                    center = Offset(size.width * 0.2f, size.height * 0.3f)
                )

                drawRect(
                    color = Color(0xFF2196F3),
                    topLeft = Offset(size.width * 0.6f, size.height * 0.1f),
                    size = androidx.compose.ui.geometry.Size(120f, 80f)
                )

                drawCircle(
                    color = Color(0xFFFF9800),
                    radius = 30f,
                    center = Offset(size.width * 0.5f, size.height * 0.8f)
                )
            }

//            // 弹幕显示层
//            CanvasDanmakuContainer(
//                modifier = Modifier.fillMaxSize(),
//                fontSize = 16,
//                speed = 2f,
//                maxDanmakuCount = 20,
//                onDanmakuManagerCreated = { manager ->
//                    danmakuManager = manager
//                }
//            )

            // 标注模式的视觉提示
            if (isAnnotationMode) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 绘制网格线以帮助定位
                    val gridSpacing = 100f
                    for (x in 0 until (size.width / gridSpacing).toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(x * gridSpacing, 0f),
                            end = Offset(x * gridSpacing, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0 until (size.height / gridSpacing).toInt()) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(0f, y * gridSpacing),
                            end = Offset(size.width, y * gridSpacing),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }

    // 自动演示
    LaunchedEffect(danmakuManager) {
        danmakuManager?.let { manager ->
            delay(2000)

            manager.addTopDanmaku("🎯 视频标注功能演示", null, Color(0xFF4CAF50), 3000L)
            delay(2000)

            // 逐个标注不同的视频元素
            manager.addAnnotationDanmakuRelative("绿色圆圈", 0.2f, 0.25f, null, Color.Yellow, 4000L)
            delay(1500)

            manager.addAnnotationDanmakuRelative("蓝色矩形", 0.7f, 0.08f, null, Color.Cyan, 4000L)
            delay(1500)

            manager.addAnnotationDanmakuRelative("橙色圆点", 0.5f, 0.75f, null, Color.Magenta, 4000L)
            delay(2000)

            manager.addBottomDanmaku("💡 点击\"标注模式\"后可在任意位置添加标注", null, Color.White, 5000L)
        }
    }
}
