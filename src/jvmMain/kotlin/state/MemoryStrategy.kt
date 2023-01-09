package state

enum class MemoryStrategy {
    /** 正常的记忆单词，可以多次拼写单词，播放视频，抄写字幕，可以显示所有的信息。 */
    Normal,
    /** 正常记忆单词时，记忆完一个单元的词之后的听写测试。*/
    Dictation,
    /** 听写复习，可以选择多个单元的单词一起复习，先测试，测试完，再复习听写错误的单词。*/
    Review,
    /** 正常的记忆单词时的复习听写错误的单词。*/
    NormalReviewWrong ,
    /** 听写复习时的复习听写错误的单词。*/
    DictationReviewWrong
}