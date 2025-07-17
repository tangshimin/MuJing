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