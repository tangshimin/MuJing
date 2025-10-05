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

package tts

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MacTTS {
    private var process: Process? = null

    fun speakAndWait(text:String) {
        process = speak(text)
        if (process != null) {
            try {
                process!!.waitFor()
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
            }
        }
    }

    private fun speak(text: String): Process? {
        process = null
        try {
            process = Runtime.getRuntime().exec("say \"$text\"")
            if (process != null) {
                // consume the output stream
                ProcessReader(process!!, false)
                // consume the error stream
                ProcessReader(process!!, true)

            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }

        return process
    }
}

internal class ProcessReader(process: Process, errorStream: Boolean) {
    init {
        val processStream = if (errorStream) process.errorStream else process.inputStream
            try {
                val br = BufferedReader(InputStreamReader(processStream))
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    if (errorStream) {
                        System.err.println(line)
                    } else {
                        println(line)
                    }
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
    }
}

