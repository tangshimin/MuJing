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

package util

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

// 定义 User32 库的接口
interface User32Ext : StdCallLibrary {

    companion object {
        // 加载 user32.dll 库
        val INSTANCE: User32Ext = Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)

        // 定义窗口样式常量
        const val GWL_STYLE = -16
        const val WS_OVERLAPPEDWINDOW = 0x00CF0000
        const val WS_POPUP =   0x80000000

        // SetWindowPos 常量
        val HWND_TOP: HWND = HWND(Pointer.createConstant(0))
        const val SWP_NOSIZE = 0x0001
        const val SWP_NOMOVE = 0x0002
        const val SWP_NOZORDER = 0x0004
        const val SWP_FRAMECHANGED = 0x0020
    }

    // 获取窗口信息 (64位兼容)
    fun GetWindowLongPtr(hWnd: HWND, nIndex: Int): LONG_PTR

    // 设置窗口信息 (64位兼容)
    fun SetWindowLongPtr(hWnd: HWND, nIndex: Int, dwNewLong: LONG_PTR): LONG_PTR

    // 设置窗口位置
    fun SetWindowPos(hWnd: HWND, hWndInsertAfter: HWND?, X: Int, Y: Int, cx: Int, cy: Int, uFlags: Int): Boolean

    // 获取屏幕尺寸
    fun GetSystemMetrics(nIndex: Int): Int
}

// 定义 GetSystemMetrics 的常量
object SM {
    const val CXSCREEN = 0
    const val CYSCREEN = 1
}

// 用于保存窗口状态的简单数据类
data class WindowState(
    var style: LONG_PTR = LONG_PTR(0),
    var rect: RECT = RECT()
)