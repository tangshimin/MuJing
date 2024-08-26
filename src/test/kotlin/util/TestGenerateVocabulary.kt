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

}