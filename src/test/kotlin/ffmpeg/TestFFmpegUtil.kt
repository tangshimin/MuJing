package ffmpeg

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import subtitleFile.FormatSRT
import java.io.File
import java.io.FileInputStream

class TestFFmpegUtil {

    private val subtitlesFolder = File("build/test-results/subtitles")

    @Before
    fun setup() {
        if (!subtitlesFolder.exists()) {
            subtitlesFolder.mkdirs()
        }
    }

    @Test
    fun `Test FFmpeg Executable Diagnosis`() {
        val ffmpegPath = findFFmpegPath()
        println("=== FFmpeg 诊断信息 ===")
        println("FFmpeg path: $ffmpegPath")

        val ffmpegFile = File(ffmpegPath)
        println("文件存在: ${ffmpegFile.exists()}")
        println("文件大小: ${ffmpegFile.length()} bytes")
        println("可执行权限: ${ffmpegFile.canExecute()}")
        println("可读权限: ${ffmpegFile.canRead()}")

        // 检查文件类型和架构
        try {
            val fileProcess = ProcessBuilder("file", ffmpegPath).start()
            val fileOutput = fileProcess.inputStream.bufferedReader().readText()
            println("文件类型: $fileOutput")
        } catch (e: Exception) {
            println("无法检查文件类型: ${e.message}")
        }

        // 检查动态库依赖
        try {
            val otoolProcess = ProcessBuilder("otool", "-L", ffmpegPath).start()
            val otoolOutput = otoolProcess.inputStream.bufferedReader().readText()
            println("动态库依赖:")
            println(otoolOutput)
        } catch (e: Exception) {
            println("无法检查动态库依赖: ${e.message}")
        }

        // 直接执行测试
        try {
            println("=== 直接执行测试 ===")
            val processBuilder = ProcessBuilder(ffmpegPath, "-version")
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            println("退出代码: $exitCode")
            println("输出内容:")
            println(output)

            if (exitCode == 0) {
                println("✅ FFmpeg 可以正常执行")
            } else {
                println("❌ FFmpeg 执行失败")
            }

        } catch (e: Exception) {
            println("❌ 执行异常: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `Test Extract Subtitles From MP4`() {
        val input = "src/test/resources/Sintel.2010.480.mp4"
        val subtitleId = 1
        val result = extractSubtitles(input, subtitleId,"$subtitlesFolder/Sintel.2010.480.srt")
        assert(result == "finished")
        val actualFile = File("$subtitlesFolder/Sintel.2010.480.srt")
        assert(actualFile.exists())
        val inputStream = FileInputStream(actualFile)
        val formatSRT = FormatSRT()
        val tto = formatSRT.parseFile(actualFile.name, inputStream)
        assert(tto.captions.size == 8){"captions size should be 8"}
        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","You're a fool for traveling alone,<br />so completely unprepared.<br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","A dragon.<br />", lastCaption!!.content )
    }

    @Test
    fun `Test Extract Subtitles From MKV`() {
        val input = "src/test/resources/Sintel.2010.480.mkv"
        val subtitleId = 1
        val result = extractSubtitles(input, subtitleId,"$subtitlesFolder/Sintel.2010.480.srt")
        assert(result == "finished")

        val actualFile = File("$subtitlesFolder/Sintel.2010.480.srt")
        assert(actualFile.exists())

        val inputStream = FileInputStream(actualFile)
        val formatSRT = FormatSRT()
        val tto = formatSRT.parseFile(actualFile.name, inputStream)
        assert(tto.captions.size == 8){"captions size should be 8"}
        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","You're a fool for traveling alone,<br />so completely unprepared.<br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","A dragon.<br />", lastCaption!!.content )
    }


    @Test
    fun `Test Shorthand color style ASS to SRT`(){
        // 这个 ASS 文件的 Style 有点特殊，有两个简写的 &H0,TED 的ASS 字幕会这样写
        // Style: Default,Arial,16,&Hffffff,&Hffffff,&H0,&H0,0,0,0,0,100,100,0,0,1,1,0,2,10,10,10,0
        val assFile = File("src/test/resources/ted-2022-bill-gates-en.ass")
        val srtFile = File("$subtitlesFolder/ted-2022-bill-gates-en.srt")
        val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
        assert(result == "finished")

        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.size == 262){"captions size should be 262"}



        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","In the year 6 CE,<br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","(Applause)<br />", lastCaption!!.content )
    }

    @Test
    fun `Test ASS to SRT`(){
        val assFile = File("src/test/resources/ASS Example V4+.ass")
        val srtFile = File("$subtitlesFolder/ASS Example V4+.srt")

        val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
        assert(result == "finished")

        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.size == 2){"captions size should be 2"}

        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","<font face=\"Tahoma\" color=\"#000000\"><b>Le rugissement des larmes !<br />Tu es mon ami.</b></font><br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","<font face=\"Tahoma\" color=\"#000000\"><b>Est-ce vraiment Naruto ?</b></font><br />", lastCaption!!.content )
    }

    @Test
    fun `Test SSA to SRT`(){
        val assFile = File("src/test/resources/SSA Example V4.ssa")
        val srtFile = File("$subtitlesFolder/SSA Example V4.srt")

        val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
        assert(result == "finished")

        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.size == 2){"captions size should be 2"}

        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","<font face=\"Gill Sans Condensed\" size=\"30\" color=\"#8080ff\"><b>{\\an5}See you again... Best wishes</b></font><br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","<font face=\"Gill Sans Condensed\" size=\"36\"><b>{\\an2}Story, Script & Direction - MIYAZAKI Hayao</b></font><br />", lastCaption!!.content )
    }

    @Test
    fun `Test ASS bilingual subtitles to SRT`() {
        val assFile = File("src/test/resources/Inception.ass")
        val srtFile = File("$subtitlesFolder/Inception to SRT.srt")

        val result = convertToSrt(assFile.absolutePath, srtFile.absolutePath)
        assert(result == "finished")

        val formatSRT = FormatSRT()
        val inputStream = FileInputStream(srtFile)
        val tto = formatSRT.parseFile(srtFile.name, inputStream)
        assert(tto.captions.size == 11) { "captions size should be 11" }
        val firstCaption = tto.captions.values.first()
        assertEquals("first caption content should be equal","快过来<br /><font face=\"Tahoma\"><font size=\"14\"><font color=\"#ffff00\">Hey! Come here!</font></font></font><br />", firstCaption!!.content )
        val lastCaption = tto.captions.values.last()
        assertEquals("last caption content should be equal","我们在一个梦中相遇<br /><font face=\"Tahoma\"><font size=\"14\"><font color=\"#ffff00\">It belonged to a man I met</font></font></font><br />", lastCaption!!.content )

    }



    @After
    fun clean() {
        subtitlesFolder.deleteRecursively()
    }
}