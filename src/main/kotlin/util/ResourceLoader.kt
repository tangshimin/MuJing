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

import java.io.IOException
import java.io.InputStream


// 加载模型资源文件，只能用于 JVM 上，因为 contextClassLoader 没有在非 JVM 创建的线程中定义。
// Resource loader based on JVM current context class loader.
fun loadModelResource(path: String): InputStream {
    return loadResource(path)
}

fun loadSvgResource(path: String): InputStream {
    return loadResource(path)
}

private fun loadResource(path: String): InputStream {
    val contextClassLoader = Thread.currentThread().contextClassLoader!!
    return contextClassLoader.getResourceAsStream(path)
        ?: throw IOException("无法加载资源文件: $path")
}