package player

import com.sun.jna.platform.win32.Kernel32
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


// 不依赖鼠标移动的保持唤醒函数
fun keepScreenAwake(scope: CoroutineScope): Job {
    return scope.launch {
        try {
            when {
                isWindows() -> {
                    WindowsKeepAwake.preventSleep()
                    // Windows 需要定期刷新
                    while (isActive) {
                        delay(30000)
                        WindowsKeepAwake.preventSleep()
                    }
                }
                isMacOS() -> {
                    // macOS 只需要启动一次进程
                    MacOSKeepAwake.preventSleep()
                    // 保持协程活跃但不需要重复调用
                    while (isActive) {
                        delay(30000)
                    }
                }
                else -> {
                    // Linux 系统
                    while (isActive) {
                        try {
                            val process = ProcessBuilder("xset", "s", "reset").start()
                            process.waitFor(1000, TimeUnit.MILLISECONDS)
                            process.destroyForcibly()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(30000)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun stopKeepingScreenAwake(job: Job?) {
    job?.cancel()
    when {
        isWindows() -> WindowsKeepAwake.allowSleep()
        isMacOS() -> MacOSKeepAwake.allowSleep()
    }
}

/**
 * Windows 系统防止屏幕休眠和系统睡眠的工具对象
 * 
 * 使用 Windows API 的 SetThreadExecutionState 函数来:
 * - 防止屏幕关闭 (ES_DISPLAY_REQUIRED)
 * - 防止系统进入睡眠状态 (ES_SYSTEM_REQUIRED) 
 * - 持续生效直到明确取消 (ES_CONTINUOUS)
 * 
 * 原理: 通过设置线程执行状态来告诉系统当前有需要保持唤醒的活动
 * 这种方法比模拟鼠标移动更高效且不会干扰用户操作
 */
object WindowsKeepAwake {
    /** 持续生效标志 - 设置后直到调用 allowSleep() 才会取消 */
    private const val ES_CONTINUOUS = 0x80000000L
    
    /** 保持显示器开启标志 - 防止屏幕关闭 */
    private const val ES_DISPLAY_REQUIRED = 0x00000002L
    
    /** 保持系统唤醒标志 - 防止系统进入睡眠状态 */
    private const val ES_SYSTEM_REQUIRED = 0x00000001L

    /**
     * 阻止 Windows 系统睡眠和屏幕关闭
     * 
     * 组合使用三个标志:
     * - ES_CONTINUOUS: 持续生效
     * - ES_DISPLAY_REQUIRED: 保持显示器开启
     * - ES_SYSTEM_REQUIRED: 保持系统唤醒
     * 
     * 调用此函数后，系统将不会自动睡眠或关闭屏幕，
     * 直到调用 allowSleep() 方法取消
     */
    fun preventSleep() {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            Kernel32.INSTANCE.SetThreadExecutionState(
                (ES_CONTINUOUS or ES_DISPLAY_REQUIRED or ES_SYSTEM_REQUIRED).toInt()
            )
        }
    }

    /**
     * 允许 Windows 系统恢复正常睡眠行为
     * 
     * 通过设置仅 ES_CONTINUOUS 标志来取消之前的防睡眠设置，
     * 让系统恢复正常的电源管理行为
     */
    fun allowSleep() {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS.toInt())
        }
    }
}



/**
 * macOS 系统防止屏幕休眠和系统睡眠的工具对象
 * 
 * 使用 macOS 的 caffeinate 命令行工具来:
 * - 防止显示器进入睡眠状态 (-d 标志)
 * - 保持系统唤醒直到进程被终止
 * 
 * 原理: 启动一个 caffeinate 子进程来阻止系统睡眠，
 * 通过销毁该进程来恢复正常的电源管理行为
 */
object MacOSKeepAwake {
    private var process: Process? = null
    private var isPreventingSleep = false

    /**
     * 阻止 macOS 系统睡眠和屏幕关闭
     * 
     * 使用 caffeinate 工具的 -d 参数:
     * - -d: 防止显示器进入睡眠状态
     * 
     * 启动一个后台 caffeinate 进程，该进程会持续运行
     * 直到调用 allowSleep() 方法显式终止它
     * 
     * @throws Exception 如果 caffeinate 命令执行失败
     */
    fun preventSleep() {
        if (isMacOS() && !isPreventingSleep) {
            try {
                // 只在首次调用时启动进程
                if (process == null || !process!!.isAlive) {
                    process?.destroy() // 清理旧进程
                    process = ProcessBuilder("caffeinate", "-d").start()
                    isPreventingSleep = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 允许 macOS 系统恢复正常睡眠行为
     * 
     * 终止之前启动的 caffeinate 进程，
     * 让系统恢复正常的电源管理行为
     */
    fun allowSleep() {
        process?.destroy()
        process = null
        isPreventingSleep = false
    }
}