package player

import kotlinx.serialization.Serializable
import subtitleFile.FormatSRT
import java.io.File

@Serializable
data class NewCaption(var start: Int, var end: Int, var content: String) {
    override fun toString(): String {
        return content
    }
}


class NewTimedCaption(subtitleFile: File) {
    private var captionList: List<NewCaption>
    private var captionMap: Map<Int,NewCaption> = emptyMap()
    private var currentIndex = 0

    init {
        require(subtitleFile.exists()) { "Subtitle file does not exist" }
        val formatSRT = subtitleFile.inputStream().use {
            FormatSRT().parseFile(subtitleFile.name, it, Charsets.UTF_8)
        }

        captionMap = formatSRT.captions.map {
            it.key to NewCaption(
                start = it.value.start.mseconds,
                end = it.value.end.mseconds,
                content = it.value.content
            )
        }.toMap()

        captionList= captionMap.values.toList()

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

}


