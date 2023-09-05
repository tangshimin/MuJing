package player

import androidx.compose.runtime.*
import data.Caption
import kotlinx.serialization.Serializable


@Serializable
data class PlayerCaption(var start: Long, var end: Long, var content: String) {
    override fun toString(): String {
        return content
    }
}

class TimedCaption{
    val captionList =  mutableStateListOf<Caption>()
    /** Map 的 Key 为秒，Triple 的第一个参数是开始，第二个参数是结束，第三个参数是字幕索引 */
    private val timeMap =  mutableStateMapOf<Int,MutableList<Triple<Double,Double,Int>>>()

    private var previousCaptionStart: Long by  mutableStateOf(0)
    private var previousCaptionEnd: Long by  mutableStateOf(0)
    private var captionStart: Long by  mutableStateOf(0)
    private var captionEnd: Long by  mutableStateOf(0)
    private var nextCaptionStart: Long by  mutableStateOf(0)
    private var nextCaptionEnd: Long by  mutableStateOf(0)
    private var currentIndex by mutableStateOf(0)

    fun setCaptionList(captions:List<Caption>){
        clear()
        captions.forEachIndexed { index, caption ->
            captionList.add(caption)
            val start = parseTime(caption.start)
            val end = parseTime(caption.end)
            val startInt = start.toInt()
            val list = timeMap[startInt] ?: mutableListOf()
            list.add(Triple(start,end,index))
            timeMap[startInt] = list
        }
    }
    fun getCaption( newTime:Long,currentIndex: Int): Caption?{
        when (newTime) {
            in captionStart..captionEnd -> {
                return captionList[currentIndex]
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                previousCaptionStart = parseTime(captionList[index - 1].start).times(1000).toLong()
                previousCaptionEnd = parseTime(captionList[index - 1].end).times(1000).toLong()
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    nextCaptionStart = parseTime(captionList[index + 1].start).times(1000).toLong()
                    nextCaptionEnd = parseTime(captionList[index + 1].end).times(1000).toLong()
                }

                return captionList[index]
            }
            in previousCaptionStart..previousCaptionEnd -> {
                val index = currentIndex - 1

                captionStart = previousCaptionStart
                captionEnd = previousCaptionEnd

                previousCaptionStart = parseTime(captionList[index - 1].start).times(1000).toLong()
                previousCaptionEnd = parseTime(captionList[index - 1].end).times(1000).toLong()

                if(index + 1 < captionList.size){
                    nextCaptionStart = parseTime(captionList[index + 1].start).times(1000).toLong()
                    nextCaptionEnd = parseTime(captionList[index + 1].end).times(1000).toLong()
                }

                return captionList[index]
            }
            else -> {
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        previousCaptionStart = parseTime(captionList[it.third - 1].start).times(1000).toLong()
                        previousCaptionEnd = parseTime(captionList[it.third - 1].end).times(1000).toLong()
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            nextCaptionStart = parseTime(captionList[it.third + 1].start).times(1000).toLong()
                            nextCaptionEnd = parseTime(captionList[it.third + 1].end).times(1000).toLong()
                        }
                        return captionList[it.third]
                    }
                }
            }
        }
        return null
    }
    fun getCaption( newTime:Long): Caption?{
        // newTime 是毫秒
        when (newTime) {
            in captionStart..captionEnd -> {
                println("current")
                return captionList[currentIndex]
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    val nextCaption = captionList[index + 1]
                    nextCaptionStart = parseTime(nextCaption.start).times(1000).toLong()
                    nextCaptionEnd = parseTime(nextCaption.end).times(1000).toLong()
                }
                currentIndex = index
                println("next")
                return captionList[index]
            }
            else -> {
                // time 是秒
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            val nextCaption = captionList[it.third + 1]
                            nextCaptionStart = parseTime(nextCaption.start).times(1000).toLong()
                            nextCaptionEnd = parseTime(nextCaption.end).times(1000).toLong()
                        }
                        currentIndex = it.third
                        return captionList[it.third]
                    }
                }
            }
        }
        return null
    }
    fun getCaption( newTime:Long,currentCaption: Caption?): Caption?{

        when (newTime) {
            in captionStart..captionEnd -> {
                return currentCaption
            }
            else ->{
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    if(newTime in start..end){
                        previousCaptionStart = parseTime(captionList[it.third - 1].start).times(1000).toLong()
                        previousCaptionEnd = parseTime(captionList[it.third - 1].end).times(1000).toLong()
                        captionStart = start
                        captionEnd = end
                        if(it.third + 1 < captionList.size){
                            nextCaptionStart = parseTime(captionList[it.third + 1].start).times(1000).toLong()
                            nextCaptionEnd = parseTime(captionList[it.third + 1].end).times(1000).toLong()
                        }
                        return captionList[it.third]
                    }
                }
            }
        }

        return null
    }


    fun getCaptionIndex(
        newTime:Long,
        currentIndex: Int,
    ):Int{
        when (newTime) {
            in captionStart..captionEnd -> {
                return currentIndex
            }
            in nextCaptionStart..nextCaptionEnd -> {
                val index = currentIndex + 1
                captionStart = nextCaptionStart
                captionEnd = nextCaptionEnd
                if(index + 1 < captionList.size){
                    val nextCaption = captionList[index + 1]
                    nextCaptionStart = parseTime(nextCaption.start).times(1000).toLong()
                    nextCaptionEnd = parseTime(nextCaption.end).times(1000).toLong()
                }
                return index
            }
            else -> {
                val time = newTime / 1000
                val list = timeMap[time.toInt()]
                list?.forEach {
                    val start = it.first.times(1000).toLong()
                    val end = it.second.times(1000).toLong()
                    val index = it.third
                    if(newTime in start..end){
                        captionStart = start
                        captionEnd = end
                        if(index + 1 < captionList.size){
                            val nextCaption = captionList[index + 1]
                            nextCaptionStart = parseTime(nextCaption.start).times(1000).toLong()
                            nextCaptionEnd = parseTime(nextCaption.end).times(1000).toLong()
                        }
                        return index
                    }
                }
            }
        }

        return currentIndex
    }

    fun isEmpty():Boolean{
        return captionList.isEmpty()
    }
    fun isNotEmpty():Boolean{
        return captionList.isNotEmpty()
    }
    fun clear(){
        timeMap.clear()
        captionList.clear()
        captionStart = 0
        captionEnd = 0
        nextCaptionStart = 0
        nextCaptionEnd = 0
    }
}

@Composable
fun rememberTimedCaption():TimedCaption = remember{
    TimedCaption()
}
class PlayerTimedCaption{
    val captionList =  mutableStateListOf<PlayerCaption>()
    var currentIndex by mutableStateOf(0)
    /** Map 的 Key 为秒，Pair 第一个参数是字幕，第二个参数是字幕索引 */
    private val timeMap =  mutableStateMapOf<Long,MutableList<Pair<PlayerCaption,Int>>>()
    private var currentCaption: PlayerCaption by  mutableStateOf(PlayerCaption(0,0,""))
    private var nextCaption: PlayerCaption by  mutableStateOf(PlayerCaption(0,0,""))


    fun setCaptionList(captions:List<PlayerCaption>){
        clear()
        captions.forEachIndexed { index, caption ->
            captionList.add(caption)
            val start = caption.start.toDouble().div(1000)
            val startInt = start.toLong()
            val list = timeMap[startInt] ?: mutableListOf()
            list.add(Pair(caption,index))
            timeMap[startInt] = list
        }
    }

    fun getCaption( newTime:Long): String{
        // newTime 是毫秒
        when (newTime) {
            // TODO  有 260 毫秒的延迟，还没有找到原因
            in currentCaption.start..currentCaption.end -> {
                println("current")
                println("start:${currentCaption.start} end:${currentCaption.end} content:${currentCaption.content}")
                return currentCaption.content
            }
            in nextCaption.start..nextCaption.end -> {
                currentCaption.start = nextCaption.start
                currentCaption.end = nextCaption.end
                currentCaption.content = nextCaption.content

                currentIndex += 1

                val nextIndex = currentIndex + 1
                if (nextIndex < captionList.size) {
                    nextCaption = captionList[nextIndex]
                }
                println("next")
                return currentCaption.content
            }
            else -> {
                // time 是秒
                val time = newTime / 1000
                val list = timeMap[time]
                list?.forEach {
                    val start = it.first.start
                    val end = it.first.end
                    if(newTime in start..end){
                        currentIndex = it.second
                        currentCaption.start = it.first.start
                        currentCaption.end = it.first.end
                        currentCaption.content = it.first.content

                        if(it.second + 1 < captionList.size){
                            val next = captionList[it.second + 1]
                            nextCaption = next
                        }
                        return currentCaption.content
                    }
                }
            }
        }
        return ""
    }


    fun isEmpty():Boolean{
        return captionList.isEmpty()
    }
    fun isNotEmpty():Boolean{
        return captionList.isNotEmpty()
    }
    fun clear(){
        timeMap.clear()
        captionList.clear()
        currentCaption = PlayerCaption(0,0,"")
        nextCaption = PlayerCaption(0,0,"")
    }
}

@Composable
fun rememberPlayerTimedCaption():PlayerTimedCaption = remember{
    PlayerTimedCaption()
}