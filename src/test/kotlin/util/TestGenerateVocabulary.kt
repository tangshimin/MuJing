/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package util

import androidx.compose.ui.ExperimentalComposeUiApi
import opennlp.tools.chunker.ChunkerME
import opennlp.tools.chunker.ChunkerModel
import opennlp.tools.postag.POSModel
import opennlp.tools.postag.POSTaggerME
import opennlp.tools.tokenize.TokenizerME
import opennlp.tools.tokenize.TokenizerModel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TestGenerateVocabulary {
    @Test
    fun `Test Parse Document To Vocabulary`(){
        val result =  parseDocument(
            pathName = "src/test/resources/Alice's Adventures in Wonderland by Lewis Carroll.txt",
            setProgressText = { println(it) },
            enablePhrases = true
        )
        assert(result.isNotEmpty())
        assert(result.size == 436)
        println(result[0])
        assertEquals("adventures", result[0].value)
        for (word in result) {
            println("${word.value} sentences:")
            println(word.pos)
            println()
        }
        for(i in result.indices){
           if(result[i].value =="end"){
               println("end index: $i")
           }
        }
        assertEquals("end",result[272].value)
        assertEquals("Would the fall _never_ come to an end?",result[272].pos)

        assertEquals("all the",result[435].value)
        val expectedText = "hall, but they were all locked; and when Alice had been all the way down one side and up the other, trying every"
        assertEquals(expectedText, result[435].pos)

    }

    @Test
    fun `Test Sentence Detect 1`(){
        val text = "This is a sample text. It contains multiple sentences. Let's see how it works!"
        val result = sentenceDetect(text)
        assert(result.isNotEmpty())
        assertEquals(3, result.size)
        assertEquals("This is a sample text.", result[0])
        assertEquals("It contains multiple sentences.", result[1])
        assertEquals("Let's see how it works!", result[2])
    }

    @Test
    fun `Test Sentence Detect 2`(){
        val file = File("src/test/resources/Alice's Adventures in Wonderland by Lewis Carroll.txt")
        val longText = file.readText()
        val result = sentenceDetect(longText)
        result.forEach { sentence ->
            println(sentence)
            println()
        }
        assert(result.isNotEmpty()){"The sentences should not be empty"}
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `Test Segmenting Words and Phrases`(){
        // 加载分词模型
        val tokenModel = loadModelResource("opennlp/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin").use { inputStream ->
            TokenizerModel(inputStream)
        }
        val tokenizer = TokenizerME(tokenModel)
        // 加载词性标注模型
        val posModel = loadModelResource("opennlp/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin").use { inputStream ->
            POSModel(inputStream)
        }
        val posTagger = POSTaggerME(posModel)
        // 加载分块模型
        val chunkerModel = loadModelResource("opennlp/en-chunker.bin").use { inputStream ->
            ChunkerModel(inputStream)
        }
        val chunker = ChunkerME(chunkerModel)
        val text = """
           The quick brown fox, which was very fast and agile,
           jumped over the lazy dog, who was sleeping under the tree,
           while the birds were chirping loudly in the background.
        """.trimIndent()
        val result = tokenizeAndChunkText(text, tokenizer, posTagger, chunker)

        println("result size: ${result.size}")
        result.forEach { token ->
            println(token)
        }
        assert(result.isNotEmpty()){"The tokens should not be empty"}
        assert(result.size == 34){"The tokens size should be 34"}
        assertEquals("The first token should be 'The'","The", result.first())
        assertEquals("The first token should be 'the background'","the background", result.last())

    }


    @Test
    fun `Test Parse SRT To Vocabulary`(){
        val result = parseSRT(
            pathName = "src/test/resources/Sintel.2010.480.srt",
            setProgressText = { println(it) },
            enablePhrases = true
        )
        assert(result.isNotEmpty())
        assert(result.size == 30)
        assertEquals("you're",result[0].value)
        assertEquals("spirit",result[28].value)
        assertEquals("dragon",result[29].value)
    }

    @Test
    fun `Test Parse ASS To Vocabulary`(){
        val result = parseASS(
            pathName = "src/test/resources/ted-2022-bill-gates-en.ass",
            setProgressText = { println(it) },
            enablePhrases = true
        )
        assert(result.isNotEmpty())
        assertEquals("list size should be 664",664,result.size)
        assertEquals("in",result[0].value)
        assertEquals("who",result[28].value)
        assertEquals("invention",result[629].value)
        assertEquals("thank you",result[663].value)
        result.forEach { println(it.value) }
    }

    @Test
    fun `Test Parse Empty SRT`() {
        val path = File("src/test/resources/empty.srt").absolutePath
        val result = parseSRT(pathName = path, setProgressText = {}, enablePhrases = true)
        assert(result.isEmpty()) { "The result should be empty" }
    }

    @Test
    fun `Test Number Not Start From One`() {
        val path = File("src/test/resources/number_not_start_from_one.srt").absolutePath
        val result = parseSRT(pathName = path, setProgressText = {}, enablePhrases = true)
        assert(result.isNotEmpty()) { "The result should not be empty" }
    }

    @Test
    fun `Test Parse MP4 Subtitles To Vocabulary`(){
        val result = parseVideo(
            pathName = "src/test/resources/Sintel.2010.480.mp4",
            trackId = 1,
            setProgressText = { println(it) },
            enablePhrases = true
        )
        assert(result.isNotEmpty())
        assert(result.size == 30)
        assertEquals("you're",result[0].value)
        assertEquals("spirit",result[28].value)
        assertEquals("dragon",result[29].value)
    }

    @Test
    fun `Test Parse MKV Subtitles To Vocabulary`(){
        val result = parseVideo(
            pathName = "src/test/resources/Sintel.2010.480.mkv",
            trackId = 1,
            setProgressText = { println(it) },
            enablePhrases = true
        )
        assert(result.isNotEmpty())
        assert(result.size == 30)
        assertEquals("you're",result[0].value)
        assertEquals("spirit",result[28].value)
        assertEquals("dragon",result[29].value)
    }

}