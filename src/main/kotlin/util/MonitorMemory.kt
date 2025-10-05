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