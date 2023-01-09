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
        Thread {
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
        }.start()
    }
}

