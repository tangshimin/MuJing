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

import org.junit.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestClassOrder
import org.junit.jupiter.api.ClassOrderer
import subtitleFile.FormatSRT
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
@Order(1) // 确保这个测试类最先执行
class TestSubtitleConverter {

    @Test
    @Order(1)
    fun `test shorthand color style ASS to SRT`(){
        // 这个 ASS 文件的 Style 有点特殊，有两个简写的 &H0,TED 的ASS 字幕会这样写
        // Style: Default,Arial,16,&Hffffff,&Hffffff,&H0,&H0,0,0,0,0,100,100,0,0,1,1,0,2,10,10,10,0
        val assFile = File("src/test/resources/ted-2022-bill-gates-en.ass")
        val srtFile = File("src/test/resources/ted-2022-bill-gates-en.srt")

        SubtitleConverter.formatASStoSRT(assFile, srtFile)
        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.isNotEmpty()){"captions not empty"}
        assert(tto.captions.size == 262){"captions size should be 262"}

        if(srtFile.exists()){
            Files.delete(srtFile.toPath())
        }
    }

    @Test
    @Order(2)
    fun `test ass to srt`(){
        val assFile = File("src/test/resources/ASS Example V4+.ass")
        val srtFile = File("src/test/resources/ASS Example V4+ to SRT.srt")

        SubtitleConverter.formatASStoSRT(assFile, srtFile)
        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.isNotEmpty()){"captions not empty"}
        assert(tto.captions.size == 2){"captions size should be 2"}

        if(srtFile.exists()){
            Files.delete(srtFile.toPath())
        }
    }

    @Test
    @Order(3)
    fun `test ssa to srt`(){
        val assFile = File("src/test/resources/SSA Example V4.ssa")
        val srtFile = File("src/test/resources/SSA Example V4 to SRT.srt")

        SubtitleConverter.formatASStoSRT(assFile, srtFile)
        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.isNotEmpty()){"captions not empty"}
        assert(tto.captions.size == 2){"captions size should be 2"}

        if(srtFile.exists()){
            Files.delete(srtFile.toPath())
        }
    }

    @Test
    @Order(4)
    fun `test ASS bilingual subtitles to srt`() {
        val assFile = File("src/test/resources/Inception.ass")
        val srtFile = File("src/test/resources/Inception to SRT.srt")

        SubtitleConverter.formatASStoSRT(assFile, srtFile)
        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.isNotEmpty()) { "captions not empty" }
        assert(tto.captions.size == 11) { "captions size should be 11" }

        if (srtFile.exists()) {
            Files.delete(srtFile.toPath())
        }

    }
}