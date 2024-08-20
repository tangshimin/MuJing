package ui

import org.junit.Test
import util.readSRT
import java.io.File

class GenerateVocabularyTest {
    @Test
    fun testReadEmptySRT() {
        val path = File("src/test/resources/empty.srt").absolutePath
        val result = readSRT(pathName = path, setProgressText = {})
        assert(result.isEmpty()) { "The result should be empty" }
    }

    @Test
    fun testNumberNotStartFromOne() {
        val path = File("src/test/resources/number_not_start_from_one.srt").absolutePath
        val result = readSRT(pathName = path, setProgressText = {})
        assert(result.isNotEmpty()) { "The result should not be empty" }
    }
}