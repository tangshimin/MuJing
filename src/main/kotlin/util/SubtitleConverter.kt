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