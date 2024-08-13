package data


import org.junit.Assert.*
import org.junit.Test

class DictionaryTest {

    @Test
    fun query() {
        val apple = Dictionary.query("apple")
        assertNotNull(apple)
        if (apple != null) {
            assertEquals("apple",apple.value)
        }


        val cancel = Dictionary.query("cancel")
        assertNotNull(cancel)
        if(cancel != null){
            assertEquals("cancel",cancel.value)
        }

    }

    @Test
    fun queryList() {
        val list = listOf("apple","cancel","abandon","design","book","dictionary","explosive")
        val result = Dictionary.queryList(list)
        assertEquals(7,result.size)
        assertEquals("第 1 个单词应该是 apple","apple",result[0].value)
        assertEquals("第 2 个单词应该是 cancel","cancel",result[1].value)
        assertEquals("第 3 个单词应该是 abandon","abandon",result[2].value)
        assertEquals("第 4 个单词应该是 design","design",result[3].value)
        assertEquals("第 5 个单词应该是 book","book",result[4].value)
        assertEquals("第 6 个单词应该是 dictionary","dictionary",result[5].value)
        assertEquals("第 7 个单词应该是 explosive","explosive",result[6].value)
    }

    @Test
    fun queryByBncLessThan() {
        val result = Dictionary.queryByBncLessThan(1000)
        assertNotNull(result)
        assertEquals("列表不为空",true, result.isNotEmpty(),)
        assertEquals("单词的总数不对",988,result.size,)
        assertEquals("单词的数量超过了1000",true,result.size<1001,)
        assertEquals("第一个单词不是 the ","the",result[0].value)
        assertEquals("第二个单词不是 be ","be",result[1].value)
        assertEquals("第三个单词不是 of ","of",result[2].value)

    }

    @Test
    fun queryByFrqLessThan() {
        val result = Dictionary.queryByFrqLessThan(1000)
        assertNotNull(result)
        assertEquals("列表不为空",true, result.isNotEmpty())
        assertEquals("单词的总数不对",902,result.size)
        assertEquals("单词的数量超过了1000",true,result.size<1001)
        assertEquals("第一个单词不是 the ","the",result[0].value)
        assertEquals("第二个单词不是 be ","be",result[1].value)
        assertEquals("第三个单词不是 and ","and",result[2].value)
    }

    @Test
    fun queryBncMax() {
        val result = Dictionary.queryBncMax()
        assertNotEquals("BNC 词频的最大值不应该为 0",0,result)
        assertEquals("BNC 词频的最大值应该为 50000",50000,result,)
    }

    @Test
    fun queryFrqMax() {
        val result = Dictionary.queryFrqMax()
        assertNotEquals("COCA 词频的最大值不应该为 0",0,result)
        assertEquals("BNC 词频的最大值应该为 47062",47062,result)
    }

    @Test
    fun wordCount() {
        val result = Dictionary.wordCount()
        assertNotEquals("词典的单词总数不应该为 0",0,result)
        assertEquals("词典的单词总数应该为 740977",740977,result)
    }

    @Test
    fun queryByBncRange(){
        val result = Dictionary.queryByBncRange(1,100)
        assertEquals("列表不为空",true, result.isNotEmpty())
        assertEquals("单词的总数应该为",99,result.size)
        assertEquals("第一个单词是 the ","the",result[0].value)
        assertEquals("第二个单词是 be ","be",result[1].value)
        assertEquals("第四个单词是 and ","and",result[3].value)
        assertEquals("最后一个单词是 those ","those",result[98].value)
    }

    @Test
    fun queryByFrqRange(){
        val result = Dictionary.queryByFrqRange(1,100)
        assertEquals("列表不为空",true, result.isNotEmpty())
        assertEquals("单词的总数应该为",96,result.size)
        assertEquals("第一个单词是 the ","the",result[0].value)
        assertEquals("第二个单词是 be ","be",result[1].value)
        assertEquals("第四个单词是 of ","of",result[3].value)
        assertEquals("最后一个单词是 well ","well",result[95].value)
    }

}