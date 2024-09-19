package player

import kotlinx.serialization.Serializable
import org.mozilla.universalchardet.UniversalDetector
import subtitleFile.FormatSRT
import java.io.File
import java.nio.charset.Charset

@Serializable
data class NewCaption(var start: Int, var end: Int, var content: String) {
    override fun toString(): String {
        return content
    }
}


class NewTimedCaption(subtitleFile: File) {
    private var captionList: List<NewCaption> = emptyList()
    private var currentIndex = 0

    init {
        require(subtitleFile.exists()) { "Subtitle file does not exist" }

        val encoding = UniversalDetector.detectCharset(subtitleFile)
        val charset =  if(encoding != null){
            Charset.forName(encoding)
        }else{
            Charset.defaultCharset()
        }
        val formatSRT = subtitleFile.inputStream().use {
            FormatSRT().parseFile(subtitleFile.name, it, charset)
        }


        captionList= formatSRT.captions.values.map {
            NewCaption(
                start = it.start.mseconds,
                end = it.end.mseconds,
                content = it.content
            )
        }

    }

    fun update(currentTime: Long): NewCaption? {
        if (currentIndex >= captionList.size) return null
        val currentCaption = captionList[currentIndex]
        if (currentTime in currentCaption.start..currentCaption.end) {
            return currentCaption
        }
        if (currentTime > currentCaption.end) {
            currentIndex++
            return update(currentTime)
        }
        return null
    }

    fun seekTo(time: Long) {
        currentIndex = captionList.indexOfFirst { it.start >= time }
    }


    fun getCurrentIndex(): Int {
        return currentIndex
    }

    fun isEmpty(): Boolean {
        return captionList.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return captionList.isNotEmpty()
    }

    fun clear(){
        captionList = emptyList()
        currentIndex = 0
    }

}