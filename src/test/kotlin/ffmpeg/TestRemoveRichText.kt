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

package ffmpeg

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class TestRemoveRichText {
    private val subtitlesFolder = File("build/test-results/subtitles")
    @Before
    fun setup() {
        if (!subtitlesFolder.exists()) {
            subtitlesFolder.mkdirs()
        }
    }

    @Test
    fun testRemoveRichText() {
        val richText = "<font face=\"Serif\" size=\"18\">Hello World</font>"
        val result = removeRichText(richText)
        assertEquals("Hello World", result)
    }

    @Test
    fun testRemoveRichTextFromFile() {
        val richTextSrt = File("src/test/resources/Sintel.2010.480.RichText.srt")
        val content = richTextSrt.readText()
        val result = removeRichText(content)

        val temp = File("$subtitlesFolder/temp.srt")
        temp.writeText(result)
        val actualLines = temp.readLines()
        val expectedSrt = File("src/test/resources/Sintel.2010.480.srt")
        val lines = expectedSrt.readLines()
        assertEquals(lines.size, actualLines.size)
        for (i in lines.indices) {
            assertEquals(lines[i], actualLines[i])
        }
    }

    @After
    fun clean() {
        subtitlesFolder.deleteRecursively()
    }
}