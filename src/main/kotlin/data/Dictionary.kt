package data

import player.isMacOS
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

/**
 * 把结果集映射成单词
 */
fun mapToWord(result: ResultSet): Word {
    var value = result.getString("word")
    var uKphone = result.getString("british_phonetic")
    var usphone = result.getString("american_phonetic")
    var definition = result.getString("definition")
    var translation = result.getString("translation")
    var pos = result.getString("pos")
    val collins = result.getInt("collins")
    val oxford = result.getBoolean("oxford")
    var tag = result.getString("tag")
    val bnc = result.getInt("bnc")
    val frq = result.getInt("frq")
    var exchange = result.getString("exchange")

    if (value == null) value = ""
    if (uKphone == null) uKphone = ""
    if (usphone == null) usphone = ""
    if (definition == null) definition = ""
    if (translation == null) translation = ""
    if (pos == null) pos = ""
    if (tag == null) tag = ""
    if (exchange == null) exchange = ""

    definition = definition.replace("\\n", "\n")
    translation = translation.replace("\\n", "\n")
    return Word(
        value,
        usphone,
        uKphone,
        definition,
        translation,
        pos,
        collins,
        oxford,
        tag,
        bnc,
        frq,
        exchange,
        mutableListOf(),
        mutableListOf()
    )
}
object Dictionary{
    private fun getSQLiteURL(fileName:String): String {
        val property = "compose.application.resources.dir"
        val dir = System.getProperty(property)
        return if (dir != null && !dir.endsWith("prepareAppResources")) {
            // 打包之后的环境
            if(isMacOS()){
                "jdbc:sqlite:file:/Applications/幕境.app/Contents/app/resources/dictionary/$fileName"
            }else{
                "jdbc:sqlite:app/resources/dictionary/$fileName"
            }
        } else {
            "jdbc:sqlite:resources/common/dictionary/$fileName"
        }
    }

    /** 查询一个单词 */
    fun query(word: String): Word? {
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT * from ecdict WHERE word = ?"
                conn.prepareStatement(sql).use { statement ->

                    try {
                        statement.setString(1, word)
                        val result = statement.executeQuery()
                        while (result.next()) {
                            return mapToWord(result)
                        }
                    } catch (se: SQLException) {
                        //Handle errors for JDBC
                        se.printStackTrace()
                    }
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
        return null
    }

    /** 查询一个列表 */
    fun queryList(words: List<String>): MutableList<Word> {
        val results = mutableListOf<Word>()
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT * from ecdict WHERE word = ?"
                conn.prepareStatement(sql).use { statement ->
                    words.forEach { word ->
                        try {
                            statement.setString(1, word)
                            val result = statement.executeQuery()
                            while (result.next()) {
                                val resultWord = mapToWord(result)
                                results.add(resultWord)
                            }
                        } catch (se: SQLException) {
                            //Handle errors for JDBC
                            se.printStackTrace()
                        }
                    }


                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
        return results
    }


    /** 批处理查询一个列表,不保证查询顺序 */
    fun fastQueryList(words: List<String>): MutableList<Word> {
        val results = mutableListOf<Word>()
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT * from ecdict WHERE word IN (${words.joinToString { "?" }})"
                conn.prepareStatement(sql).use { statement ->
                    words.forEachIndexed { index, word ->
                        statement.setString(index + 1, word)
                    }
                    val result = statement.executeQuery()
                    while (result.next()) {
                        val resultWord = mapToWord(result)
                        results.add(resultWord)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    /** 根据 BNC 词频区间查询单词 */
    fun queryByBncRange(start:Int,end:Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE bnc != 0  AND bnc >= $start AND bnc <= $end" +
                " ORDER BY bnc"
        val results = mutableListOf<Word>()
        try{
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 查询所有 BNC 词频小于 num 的单词 */
    fun queryByBncLessThan(num:Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE bnc < $num AND bnc != 0 " +
                "ORDER BY bnc"
        val results = mutableListOf<Word>()
        try{
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 根据 FRQ 词频区间查询单词 */
    fun queryByFrqRange(start:Int,end: Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE frq != 0   AND frq >= $start AND frq <= $end" +
                " ORDER BY frq"
        val results = mutableListOf<Word>()
        try{
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 查询所有 FRQ 词频小于 num 的单词 */
    fun queryByFrqLessThan(num:Int):List<Word>{
        val sql = "SELECT * FROM ecdict WHERE frq < $num AND frq != 0 " +
                "ORDER BY frq"
        val results = mutableListOf<Word>()
        try{
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        results.add(word)
                    }
                }
            }
        }catch (se:SQLException){
            se.printStackTrace()
        }
        return results
    }

    /** 执行更新 */
    fun executeUpdate(sql: String) {
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    statement.executeUpdate(sql)
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }

    /** 执行命令
     *  "VACUUM" 清除数据库中的未使用空间。
     * */
    fun execute(sql: String) {
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                conn.createStatement().use { statement ->
                    statement.execute(sql)
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }

    /** 查询 BNC 词频的最大值 */
    fun queryBncMax():Int{
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT MAX(bnc) as max_bnc from ecdict"
                conn.createStatement().use {statement->
                    val result = statement.executeQuery(sql)
                    if(result.next()){
                        return result.getInt(1)
                    }else return 0
                }

            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

    /** 查询 COCA 词频的最大值 */
    fun queryFrqMax():Int{
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT MAX(frq) as max_frq from ecdict"
                conn.createStatement().use {statement ->
                    val result = statement.executeQuery(sql)
                    if(result.next()){
                        return result.getInt(1)
                    }else return 0
                }

            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

    /** 内置词典单词总数 */
    fun wordCount():Int{
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT COUNT(*) as count from ecdict"
                conn.createStatement().use{ statement ->
                    val result = statement.executeQuery(sql)
                    if(result.next()){
                        return result.getInt(1)
                    }else return 0
                }

            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return 0
        }
    }

    /** 查询所有单词 */
    fun queryAllWords():List<Word>{
        val words = mutableListOf<Word>()
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "SELECT * from ecdict"
                conn.createStatement().use{ statement ->
                    val result = statement.executeQuery(sql)
                    while(result.next()){
                        val word = mapToWord(result)
                        words.add(word)
                    }
                    statement.close()
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
            return words
        }
        return words
    }

    fun insertWords(words:List<Word>){
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "INSERT INTO ecdict(word,british_phonetic,american_phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
                conn.prepareStatement(sql).use { statement ->
                    words.forEach { word ->
                        statement.setString(1,word.value)
                        statement.setString(2,word.ukphone)
                        statement.setString(3,word.usphone)
                        statement.setString(4,word.definition)
                        statement.setString(5,word.translation)
                        statement.setString(6,word.pos)
                        statement.setInt(7,word.collins)
                        statement.setBoolean(8,word.oxford)
                        statement.setString(9,word.tag)
                        statement.setInt(10, word.bnc!!)
                        statement.setInt(11, word.frq!!)
                        statement.setString(12,word.exchange)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }

    fun deleteWord(word:String){
        try {
            val url = getSQLiteURL("ecdict.db")
            DriverManager.getConnection(url).use { conn ->
                val sql = "DELETE FROM ecdict WHERE word = ?"
                conn.prepareStatement(sql).use { statement ->
                    statement.setString(1,word)
                    statement.executeUpdate()
                }
            }

        } catch (e: Exception) {
            //Handle errors for Class.forName
            e.printStackTrace()
        }
    }
}




// 使用 SQLite 数据库
//fun createNewDatabase(fileName:String){
//    val url = getSQLiteURL(fileName)
//
//    DriverManager.getConnection(url).use { conn ->
//        if(conn != null){
//            val meta = conn.metaData
//            println("The driver name is ${meta.driverName}")
//            println("A new database has been created.")
//        }
//    }
//}
//
//
//fun insertWords(words:List<Word>){
//    try {
//        val url = getSQLiteURL("ecdict.db")
//        DriverManager.getConnection(url).use { conn ->
//            val sql = "INSERT INTO ecdict(word,british_phonetic,american_phonetic,definition,translation,pos,collins,oxford,tag,bnc,frq,exchange) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)"
//            conn.prepareStatement(sql).use { statement ->
//                words.forEach { word ->
//                    statement.setString(1,word.value)
//                    statement.setString(2,word.ukphone)
//                    statement.setString(3,word.usphone)
//                    statement.setString(4,word.definition)
//                    statement.setString(5,word.translation)
//                    statement.setString(6,word.pos)
//                    statement.setInt(7,word.collins)
//                    statement.setBoolean(8,word.oxford)
//                    statement.setString(9,word.tag)
//                    statement.setInt(10, word.bnc!!)
//                    statement.setInt(11, word.frq!!)
//                    statement.setString(12,word.exchange)
//                    statement.addBatch()
//                }
//                statement.executeBatch()
//            }
//        }
//
//    } catch (e: Exception) {
//        //Handle errors for Class.forName
//        e.printStackTrace()
//    }
//}
//
//fun createNewTable(){
//    val url = getSQLiteURL("ecdict.db")
//    try{
//        DriverManager.getConnection(url).use { conn ->
//            val sql = "CREATE TABLE IF NOT EXISTS ecdict" +
//                    "(word VARCHAR(64) NOT NULL UNIQUE , " +
//                    " british_phonetic VARCHAR(64), " +
//                    " american_phonetic VARCHAR(64), " +
//                    " definition TEXT, " +
//                    " translation TEXT, " +
//                    " pos VARCHAR(16), " +
//                    " collins INTEGER DEFAULT (0), " +
//                    " oxford BOOLEAN DEFAULT (0), " +
//                    " tag VARCHAR(64), " +
//                    " bnc INTEGER DEFAULT (0), " +
//                    " frq INTEGER DEFAULT (0), " +
//                    " exchange VARCHAR(256), " +
//                    " detail VARCHAR(64), " +
//                    " audio VARCHAR(64), " +
//                    " PRIMARY KEY ( word ))"
//            conn.createStatement().use { statement ->
//                statement.execute(sql)
//            }
//        }
//    }catch (e:SQLException){
//        e.printStackTrace()
//    }
//}

