package tts

import com.jacob.activeX.ActiveXComponent
import com.jacob.com.Dispatch
import com.jacob.com.Variant
import state.getResourcesFile

class MSTTSpeech {

    /** 音量 1到100 */
    var volume: Int = 100

    /** 频率 -10到10 */
    var rate: Int = 0

    /** 输出设备序号 */
    var audio: Int = 0

    /** 声音对象 */
    var spVoice:Dispatch? = null

    var ax: ActiveXComponent? = null

    init {
        System.setProperty("jacob.dll.path", getResourcesFile("jacob/jacob-1.20-x64.dll").absolutePath ?: "")
        ax = ActiveXComponent("Sapi.SpVoice")
        spVoice = ax!!.`object`
    }


    /**
     * 播放语言
     * @param text 要转换成语言的文本
     */
    fun speak(text: String) {
        try{
            // 设置音量
            Dispatch.put(spVoice,"Volume",Variant(this.volume))
            // 设置速率
            Dispatch.put(spVoice,"Rate",Variant(this.rate))
            // 开始朗读
            Dispatch.call(spVoice,"Speak",Variant(text))
        }catch (exception: Exception) {
            println(exception.message)
            exception.printStackTrace()
        }
    }

}
