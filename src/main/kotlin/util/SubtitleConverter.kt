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

import org.mozilla.universalchardet.UniversalDetector
import subtitleFile.FormatASS
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.Charset


object SubtitleConverter{

    fun formatASStoSRT(assFile:File, srtFile:File){
        val encoding = UniversalDetector.detectCharset(assFile)
        val charset = if (encoding != null) {
            Charset.forName(encoding)
        } else {
            Charset.defaultCharset()
        }
        val inputStream = FileInputStream(assFile)
        val formatASS = FormatASS()
        val timedTextObject = formatASS.parseFile(assFile.name, inputStream, charset)
        writeFileTxt(srtFile.absolutePath, timedTextObject.toSRT())
    }
}

private fun writeFileTxt(outPath: String, totalFile: Array<String?>) {
    PrintWriter(FileWriter(outPath)).use { pw ->
        totalFile.forEach { line ->
            pw.println(line)
        }
    }
}