package ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TestRemoveRichText {
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
        val expectedSrt = File("src/test/resources/Sintel.2010.480.srt")
        val text = expectedSrt.readText()
        assertEquals(text, result)
    }
}