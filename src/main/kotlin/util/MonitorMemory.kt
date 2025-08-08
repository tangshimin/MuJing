package util

import java.lang.management.ManagementFactory

fun monitorMemory() {
    val memoryBean = ManagementFactory.getMemoryMXBean()
    val heapUsage = memoryBean.heapMemoryUsage
    val nonHeapUsage = memoryBean.nonHeapMemoryUsage

    println("JVM堆内存: ${heapUsage.used / 1024 / 1024}MB / ${heapUsage.max / 1024 / 1024}MB")
    println("JVM非堆内存: ${nonHeapUsage.used / 1024 / 1024}MB")
    println()
    println()

}