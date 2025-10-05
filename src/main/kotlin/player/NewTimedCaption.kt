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

package player

import kotlinx.serialization.Serializable
import org.mozilla.universalchardet.UniversalDetector
import subtitleFile.FormatSRT
import java.io.File
import java.nio.charset.Charset

@Serializable
data class NewCaption(var start: Int, var end: Int, var content: String) {
    override fun toString(): String {
        return content
    }
}


class NewTimedCaption(subtitleFile: File) {
    private var captionList: List<NewCaption> = emptyList()
    private var currentIndex = 0

    init {
        require(subtitleFile.exists()) { "Subtitle file does not exist" }

        val encoding = UniversalDetector.detectCharset(subtitleFile)
        val charset =  if(encoding != null){
            Charset.forName(encoding)
        }else{
            Charset.defaultCharset()
        }
        val formatSRT = subtitleFile.inputStream().use {
            FormatSRT().parseFile(subtitleFile.name, it, charset)
        }


        captionList= formatSRT.captions.values.map {
            NewCaption(
                start = it.start.mseconds,
                end = it.end.mseconds,
                content = it.content
            )
        }

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

    fun isEmpty(): Boolean {
        return captionList.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return captionList.isNotEmpty()
    }

    fun clear(){
        captionList = emptyList()
        currentIndex = 0
    }

}