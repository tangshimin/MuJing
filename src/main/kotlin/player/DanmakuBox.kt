package player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.IntOffset
import data.*
import util.rememberMonospace
import java.io.File
import javax.swing.JOptionPane
import kotlin.math.floor


@Composable
fun DanmakuBox(
    vocabulary: MutableVocabulary?,
    vocabularyPath:String,
    playerState: PlayerState,
    showingDanmaku: SnapshotStateMap<Int, DanmakuItem>,
    playEvent: () -> Unit,
    playAudio: (String) -> Unit,
    windowHeight: Int,
    showingDetail:Boolean,
    showingDetailChanged:(Boolean) -> Unit
) {

    /** 删除单词 */
    val deleteWord: (DanmakuItem) -> Unit = { danmakuItem ->
        if (danmakuItem.word != null) {
            val word = danmakuItem.word
            vocabulary!!.wordList.remove(word)
            vocabulary.size = vocabulary.wordList.size
            try{
                saveVocabulary(vocabulary.serializeVocabulary,vocabularyPath)
                showingDanmaku.remove(danmakuItem.sequence)
            }catch (e:Exception){
                // 回滚
                if (word != null) {
                    vocabulary.wordList.add(word)
                    vocabulary.size = vocabulary.wordList.size
                }
                e.printStackTrace()
                JOptionPane.showMessageDialog(null, "保存词库失败,错误信息:\n${e.message}")
            }

        }

        showingDetailChanged(false)
        playEvent()
    }

    /** 把单词加入到熟悉词库 */
    val addToFamiliar: (DanmakuItem) -> Unit = { danmakuItem ->
        val word = danmakuItem.word
        if (word != null) {
            val familiarWord = word.deepCopy()
            val file = getFamiliarVocabularyFile()
            val familiar = loadVocabulary(file.absolutePath)
            // 如果当前词库是 MKV 或 SUBTITLES 类型的词库，需要把内置词库转换成外部词库。
            if (vocabulary!!.type == VocabularyType.MKV ||
                vocabulary.type == VocabularyType.SUBTITLES
            ) {
                familiarWord.captions.forEach { caption ->
                    val externalCaption = ExternalCaption(
                        relateVideoPath = vocabulary.relateVideoPath,
                        subtitlesTrackId = vocabulary.subtitlesTrackId,
                        subtitlesName = vocabulary.name,
                        start = caption.start,
                        end = caption.end,
                        content = caption.content
                    )
                    familiarWord.externalCaptions.add(externalCaption)
                }
                familiarWord.captions.clear()

            }
            if (!familiar.wordList.contains(familiarWord)) {
                familiar.wordList.add(familiarWord)
                familiar.size = familiar.wordList.size
            }
            if(familiar.name.isEmpty()){
                familiar.name = "FamiliarVocabulary"
            }
            try{
                saveVocabulary(familiar, file.absolutePath)
                deleteWord(danmakuItem)
            }catch (e:Exception){
                // 回滚
                familiar.wordList.remove(familiarWord)
                familiar.size = familiar.wordList.size
                e.printStackTrace()
                JOptionPane.showMessageDialog(null, "保存熟悉词库失败,错误信息:\n${e.message}")
            }

        }

    }

    /** 等宽字体*/
    val monospace  = rememberMonospace()

    // 在这个 Box 使用 Modifier.fillMaxSize() 可能会导致 DropdownMenu 显示的位置不准。
    Box {
        showingDanmaku.forEach { (_, danmakuItem) ->
            Danmaku(
                playerState,
                danmakuItem,
                playEvent,
                playAudio,
                monospace,
                windowHeight,
                deleteWord,
                addToFamiliar,
                showingDetail,
                showingDetailChanged
            )
        }
    }
}

@Composable
fun rememberDanmakuMap(
    videoPath: String,
    vocabularyPath: String,
    vocabulary: MutableVocabulary?
) = remember(videoPath, vocabulary){
    derivedStateOf{
        // Key 为秒 > 这一秒出现的单词列表
        val timeMap = mutableMapOf<Int, MutableList<DanmakuItem>>()
        val vocabularyDir = File(vocabularyPath).parentFile
        if (vocabulary != null) {
            // 使用字幕和MKV 生成的词库
            if (vocabulary.type == VocabularyType.MKV || vocabulary.type == VocabularyType.SUBTITLES) {
                val absVideoFile = File(videoPath)
                val relVideoFile = File(vocabularyDir, absVideoFile.name)
                // absVideoFile.exists() 为真 视频文件没有移动，还是词库里保存的地址
                //  relVideoFile.exists() 为真 视频文件移动了，词库里保存的地址是旧地址
                if ((absVideoFile.exists() && absVideoFile.absolutePath ==  vocabulary.relateVideoPath) ||
                    (relVideoFile.exists() && relVideoFile.name == File(vocabulary.relateVideoPath).name)
                ) {
                    vocabulary.wordList.forEach { word ->
                        if (word.captions.isNotEmpty()) {
                            word.captions.forEach { caption ->
                                val startTime = floor(convertTimeToSeconds(caption.start)).toInt()
                                addDanmakuToMap(timeMap, startTime, word)
                            }
                        }
                    }
                }

                // 文档词库，或混合词库
            } else {
                vocabulary.wordList.forEach { word ->
                    word.externalCaptions.forEach { externalCaption ->
                        val absVideoFile = File(videoPath)
                        val relVideoFile = File(vocabularyDir, absVideoFile.name)
                        if ((absVideoFile.exists() && absVideoFile.absolutePath == externalCaption.relateVideoPath) ||
                            (relVideoFile.exists() && relVideoFile.name == File(externalCaption.relateVideoPath).name)
                        ) {
                            val startTime = floor(convertTimeToSeconds(externalCaption.start)).toInt()
                            addDanmakuToMap(timeMap, startTime, word)
                        }
                    }
                }
            }
        }
        timeMap
    }
}

private fun addDanmakuToMap(
    timeMap: MutableMap<Int, MutableList<DanmakuItem>>,
    startTime: Int,
    word: Word
) {
    val dList = timeMap[startTime]
    val item = DanmakuItem(word.value, true, startTime, 0, false, IntOffset(0, 0), word)
    if (dList == null) {
        val newList = mutableListOf(item)
        timeMap[startTime] = newList
    } else {
        dList.add(item)
    }
}