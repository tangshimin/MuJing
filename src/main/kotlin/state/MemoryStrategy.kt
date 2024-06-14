package state

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