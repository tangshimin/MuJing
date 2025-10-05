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

package ui.wordscreen

enum class MemoryStrategy {
    /** 正常的记忆单词，可以多次拼写单词，播放视频，抄写字幕，可以显示所有的信息。 */
    Normal,

    /** 正常记忆单词时，记忆完一个单元的词之后的听写测试。*/
    Dictation,

    /** 正常的记忆单词时的复习听写错误的单词。*/
    NormalReviewWrong ,

    /** 独立的听写测试，可以选择多个章节。从侧边栏打开 */
    DictationTest,

    /** 复习独立的听写测试后的错误单词 */
    DictationTestReviewWrong
}