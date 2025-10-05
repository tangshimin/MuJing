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

package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*


/**
 * 提取 Wikitionary 词条的发音
 * @param rawdata 文件是 raw-wiktextract-data.json，从 https://kaikki.org/dictionary/rawdata.html 下载的。
 */
private fun extractedAudioUrls(rawdata:File) {
    val audios = mutableMapOf<String, MutableSet<Pronunciation>>()
    val tags = mutableSetOf<String>()
    var count = 1
    val format = Json { ignoreUnknownKeys = true }
    rawdata.useLines { lines ->
        lines.forEach { line ->
            try {

                val item = format.decodeFromString<WikitionaryItem>(line)
                if(item.lang == "English"){
                    val word = item.word.lowercase(Locale.getDefault())
                    print("Row ${count++}")
                    println("    $word")
                    val result = Dictionary.query(word)
                    if (result != null) {
                        item.sounds.forEach { sound ->
                            sound.tags.forEach { tag ->
                                tags.add(tag)
                            }
                            if (sound.mp3_url.isNotEmpty()) {
                                val pronunciation = Pronunciation(sound.tags.first(), sound.mp3_url)
                                val pronunciations = audios.get(word)
                                if (pronunciations == null) {
                                    audios.put(word, mutableSetOf(pronunciation))
                                } else {
                                    pronunciations.add(pronunciation)
                                }

                            }else if(sound.ogg_url.isNotEmpty()){
                                val pronunciation = Pronunciation(sound.tags.first(), sound.ogg_url)
                                val pronunciations = audios.get(word)
                                if (pronunciations == null) {
                                    audios.put(word, mutableSetOf(pronunciation))
                                } else {
                                    pronunciations.add(pronunciation)
                                }
                            }


                        }
                    }
                }else{
                    println("lang:${item.lang}")
                }

            } catch (_: Exception) {
            }
        }
    }

    val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    val jsonString = json.encodeToString(audios)
    val audioUrls = File("resources/common/AudioUrls.json")
    audioUrls.writeText(jsonString)
}

/**
 * Wikitionary 的词条,我只关注单词发音，就只映射了两个 Key。
 */
@Serializable
data class WikitionaryItem(
    val word:String,
    val lang:String,
    val sounds:List<AudioItem>
)

/**
 * Wikitionary 词条的发音
 */
@Serializable
data class AudioItem(
    val audio:String,
    val text:String,
    val tags:List<String>,
    val ogg_url:String,
    val mp3_url:String,
)

/**
 * 单词发音
 * @param tag 地区
 * @param url 发音的网址
 */
@Serializable
data class Pronunciation(
    val tag:String,
    val url:String
)
