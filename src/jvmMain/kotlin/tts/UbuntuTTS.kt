package tts

import java.io.IOException
import javax.swing.JOptionPane

class UbuntuTTS {
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
            process = Runtime.getRuntime().exec("espeak \"$text\"")
            if (process != null) {
                // consume the output stream
                ProcessReader(process!!, false)
                // consume the error stream
                ProcessReader(process!!, true)

            }
        } catch (exception: IOException) {
            exception.printStackTrace()
            if(exception.message?.endsWith("No such file or directory") == true) {
                JOptionPane.showMessageDialog(null, "请安装 espeak", "错误", JOptionPane.ERROR_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(null, "${exception.message}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }

        return process
    }
}
