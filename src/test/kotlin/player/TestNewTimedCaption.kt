package player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TestNewTimedCaption {
    @Test
    fun `Test From Start To End`() {
        val subtitleFile = File("src/test/resources/Sintel.2010.480.srt")
        val timedCaption = NewTimedCaption(subtitleFile)

        // 模拟播放器每秒调用 onTimeChange 10次
        var currentTime: Long = 0
        val interval = 100L // 100ms
        var currentContent = ""
        while (true) {

            val currentCaption =  timedCaption.update(currentTime)
            if (currentCaption != null && currentCaption.content != currentContent) {
                println(currentCaption.content)
                currentContent = currentCaption.content
            }

            if(currentContent == "A dragon.<br />") break

            Thread.sleep(interval)
            currentTime += interval
        }
    }

    @Test
    fun `Test Seek To Time`() {
        val subtitleFile = File("src/test/resources/Sintel.2010.480.srt")
        val timedCaption = NewTimedCaption(subtitleFile)
        var seeked = false
        // 模拟播放器每秒调用 onTimeChange 10次
        var currentTime: Long = 0
        val interval = 100L // 100ms
        var currentContent = ""
        while (true) {
            timedCaption.update(currentTime)
            val currentCaption =  timedCaption.update(currentTime)
            val currentIndex = timedCaption.getCurrentIndex()
            if (currentCaption != null && currentCaption.content != currentContent) {
                println(currentCaption.content)
                currentContent = currentCaption.content
            }

            if(currentCaption != null &&  seeked) {
                assertEquals("So...<br />", currentCaption.content)
                assertEquals(3, currentIndex)
                break
            }

            if(currentCaption != null &&  currentContent == "A dragon.<br />") {
                timedCaption.seekTo(9000)
                currentTime = 9000
                seeked = true
            }

            Thread.sleep(interval)
            currentTime += interval
        }
    }

}