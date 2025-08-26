package util

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND

object FullScreenManager {
    private var isFullScreen = false
    private val originalState = WindowState()
    private val user32 = User32Ext.INSTANCE
    private val user32Std = User32.INSTANCE


    fun toggle(windowHandle: Long) {
        // 检查操作系统，确保是 Windows
        val os = System.getProperty("os.name").lowercase()
        if (!os.contains("win")) {
            println("This function is for Windows only.")
            return
        }

        if (windowHandle == 0L) {
            println("Invalid window handle.")
            return
        }

        val hwnd = HWND(Pointer(windowHandle))
        isFullScreen = !isFullScreen

        if (isFullScreen) {
            enterFullScreen(hwnd)
        } else {
            exitFullScreen(hwnd)
        }
    }

    private fun enterFullScreen(hwnd: HWND) {
        // 1. 保存当前窗口状态
        originalState.style = user32.GetWindowLongPtr(hwnd, User32Ext.GWL_STYLE)
        user32Std.GetWindowRect(hwnd, originalState.rect)

        // 2. 修改窗口样式为无边框的弹出式窗口
        val newStyle = originalState.style.toLong() and User32Ext.WS_OVERLAPPEDWINDOW.toLong().inv() or User32Ext.WS_POPUP.toLong()
        user32.SetWindowLongPtr(hwnd, User32Ext.GWL_STYLE, LONG_PTR(newStyle))


        // 3. 获取屏幕分辨率并设置窗口位置和大小
        val screenWidth = user32.GetSystemMetrics(SM.CXSCREEN)
        val screenHeight = user32.GetSystemMetrics(SM.CYSCREEN)

        user32.SetWindowPos(
            hwnd,
            User32Ext.HWND_TOP,
            0, 0, screenWidth, screenHeight,
            User32Ext.SWP_FRAMECHANGED
        )
    }

    private fun exitFullScreen(hwnd: HWND) {
        // 1. 恢复窗口样式
        user32.SetWindowLongPtr(hwnd, User32Ext.GWL_STYLE, originalState.style)

        // 2. 恢复窗口位置和大小
        val rect = originalState.rect
        user32.SetWindowPos(
            hwnd,
            null,
            rect.left, rect.top,
            rect.right - rect.left,
            rect.bottom - rect.top,
            User32Ext.SWP_NOZORDER or User32Ext.SWP_FRAMECHANGED
        )
    }
}