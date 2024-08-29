package util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TestGenerateVocabulary {
    @Test
    fun `Test Parse Document To Vocabulary`(){
        val result =  parseDocument(
            pathName = "src/test/resources/Alice's Adventures in Wonderland by Lewis Carroll.txt",
            setProgressText = { println(it) },
        )
        assert(result.isNotEmpty())
        assert(result.size == 416)
        println(result[0])
        assertEquals(result[0].value, "adventures")
    }

    @Test
    fun `Test Parse SRT To Vocabulary`(){
        val result = parseSRT(
            pathName = "src/test/resources/Sintel.2010.480.srt",
            setProgressText = { println(it) },
        )
        assert(result.isNotEmpty())
        assert(result.size == 29)
        assertEquals(result[0].value, "you're")
        assertEquals(result[28].value, "spirit")
    }

    @Test
    fun `Test Parse ASS To Vocabulary`(){
        val result = parseASS(
            pathName = "src/test/resources/ted-2022-bill-gates-en.ass",
            setProgressText = { println(it) },
        )
        assert(result.isNotEmpty())
        assertEquals("list size should be 630",630,result.size)
        assertEquals("in",result[0].value)
        assertEquals("who",result[28].value)
        assertEquals("being",result[629].value)
    }

    @Test
    fun `Test Parse Empty SRT`() {
        val path = File("src/test/resources/empty.srt").absolutePath
        val result = parseSRT(pathName = path, setProgressText = {})
        assert(result.isEmpty()) { "The result should be empty" }
    }

    @Test
    fun `Test Number Not Start From One`() {
        val path = File("src/test/resources/number_not_start_from_one.srt").absolutePath
        val result = parseSRT(pathName = path, setProgressText = {})
        assert(result.isNotEmpty()) { "The result should not be empty" }
    }

    @Test
    fun `Test Parse MP4 Subtitles To Vocabulary`(){
        val result = parseMP4(
            pathName = "src/test/resources/Sintel.2010.480.mp4",
            trackId = 1,
            setProgressText = { println(it) },
        )
        assert(result.isNotEmpty())
        assert(result.size == 30)
        assertEquals(result[0].value, "you're")
        assertEquals(result[28].value, "spirit")
        assertEquals(result[29].value, "dragon")
    }

}