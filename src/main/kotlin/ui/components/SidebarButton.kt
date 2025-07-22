package ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import player.isMacOS
import theme.LocalCtrl

/**
 * 打开侧边栏的按钮
 */
@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun SidebarButton(
    isOpen: Boolean,
    setIsOpen: (Boolean) -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        val width by animateDpAsState(targetValue = if (isOpen) 217.dp else 48.dp)
        Column(Modifier.width(width)) {
            if(isMacOS()){
                // SidebarButton的上边框刚好遮挡住MacOS title 标题栏的下面的那个分割线，所以这里添加一个透明效果,如果不加透明效果的话感觉哪里有两根分割线。
                // isOpen 打开之后， SidebarButton 的高度发生了变化，暂时还不知道哪里引起的，反正 isOpen 为 true 的时候，显示这个分割线刚刚好。
                Box(Modifier.fillMaxWidth().height(1.dp)
                    .background(if(isOpen) MaterialTheme.colors.onBackground.copy(alpha = 0.12f) else Color.Transparent)
                )
            }
            TooltipArea(
                tooltip = {
                    Surface(
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                        shape = RectangleShape
                    ) {
                        val ctrl = LocalCtrl.current
                        val text = if (isMacOS()) "$ctrl ⌃ S" else "$ctrl + Alt + S"
                        Row(modifier = Modifier.padding(10.dp)){
                            Text(text = "侧边栏  " )
                            CompositionLocalProvider(LocalContentAlpha provides 0.5f) {
                                Text(text = text)
                            }
                        }
                    }
                },
                delayMillis = 100,
                tooltipPlacement = TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(5.dp,0.dp)
                )
            ) {

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag("SidebarButton")
                        .width(width)
                        .shadow(
                            elevation = 0.dp,
                            shape = if (isOpen) RectangleShape else RoundedCornerShape(50)
                        )
                        .background(MaterialTheme.colors.background)
                        .clickable { setIsOpen(!isOpen) }) {

                    val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                    Icon(
                        if (isOpen) Icons.Filled.ArrowBack else icons.DockToRight,
                        contentDescription = "Localized description",
                        tint = tint,
                        modifier = Modifier.clickable { setIsOpen(!isOpen) }
                            .size(48.dp, 48.dp).padding(13.dp)
                    )
                    if (isOpen) {
                        Divider(Modifier.height(48.dp).width(1.dp))
                    }
                }
            }
            if (isOpen && isMacOS()) Divider(Modifier.fillMaxWidth())
        }
    }
}
