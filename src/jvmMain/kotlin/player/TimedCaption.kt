package player

import androidx.compose.runtime.*
import data.Caption


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
                    nextCaptionStart = parseTime(captionList[index + 1].start).times(1000).toLong()
                    nextCaptionEnd = parseTime(captionList[index + 1].end).times(1000).toLong()
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
                            nextCaptionStart = parseTime(captionList[index + 1].start).times(1000).toLong()
                            nextCaptionEnd = parseTime(captionList[index + 1].end).times(1000).toLong()
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