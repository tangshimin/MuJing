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

package theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import org.slf4j.LoggerFactory
import state.GlobalState


val IDEADarkThemeOnBackground = Color(133, 144, 151)

fun createColors(
    isDarkTheme: Boolean,
    isFollowSystemTheme:Boolean = true,
    primary: Color,
    background:Color,
    onBackground:Color
): Colors {
    val isDark = if (isFollowSystemTheme) {
        isSystemDarkMode()
    } else isDarkTheme

    return if (isDark) {
        darkColors(
            primary = primary,
            onBackground = IDEADarkThemeOnBackground
        )
    } else {
        lightColors(
            primary = primary,
            background = background,
            surface = background,
            onBackground = onBackground
        )
    }
}


fun createColors(
    global: GlobalState
): Colors {
    val isDark = if (global.isFollowSystemTheme) {
        isSystemDarkMode()
    } else global.isDarkTheme

    return if (isDark) {
        darkColors(
            primary = global.primaryColor,
            onBackground = IDEADarkThemeOnBackground
        )
    } else {
        lightColors(
            primary = global.primaryColor,
            background = global.backgroundColor,
            surface = global.backgroundColor,
            onBackground = global.onBackgroundColor
        )
    }
}

fun java.awt.Color.toCompose(): Color {
    return Color(red, green, blue)
}

fun Color.toAwt(): java.awt.Color {
    return java.awt.Color(red, green, blue)
}

fun isSystemDarkMode(): Boolean {
    val logger = LoggerFactory.getLogger("isSystemDarkMode")
    return when {
        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> {
            val command = arrayOf("/usr/bin/defaults", "read", "-g", "AppleInterfaceStyle")
            try {
                val process = Runtime.getRuntime().exec(command)
                process.inputStream.bufferedReader().use { it.readText().trim() == "Dark" }
            } catch (e: Exception) {
                logError(e, logger)
                false
            }
        }
        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> {
            val command = "reg query HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme"
            try {
                val process = Runtime.getRuntime().exec(command)
                process.inputStream.bufferedReader().use { reader ->
                    val output = reader.readText()
                    !output.contains("0x1")
                }
            } catch (e: Exception) {
                logError(e, logger)
                false
            }
        }
        System.getProperty("os.name").contains("Linux", ignoreCase = true) -> {
            // 还没有在Linux上测试
            val command = arrayOf("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
            try {
                val process = Runtime.getRuntime().exec(command)
                process.inputStream.bufferedReader().use { reader ->
                    val output = reader.readText().trim()
                    output.contains("dark", ignoreCase = true)
                }
            } catch (e: Exception) {
                logError(e, logger)
                false
            }
        }
        else -> false
    }
}

fun logError(e: Exception, logger: org.slf4j.Logger) {
    logger.error("Error StackTrace: ${e.stackTraceToString()}\n")
}