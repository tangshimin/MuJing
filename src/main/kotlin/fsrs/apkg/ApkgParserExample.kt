package fsrs.apkg

import java.io.File

/**
 * APKG 解析器使用示例
 * 演示如何解析 APKG 文件并提取信息
 */
object ApkgParserExample {

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ApkgParser()

        // 示例文件路径（可以根据实际情况修改）
        val exampleFiles = listOf(
            "src/test/resources/apkg/basic_word.apkg",
            "src/test/resources/apkg/primary_school_words.apkg",
            "src/test/resources/apkg/English_Roots.apkg"
        )

        exampleFiles.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                println("=== 解析文件: ${file.name} ===")
                
                try {
                    // 快速获取文件信息
                    val quickInfo = parser.getApkgInfo(file.absolutePath)
                    println("快速信息:")
                    println("  - 笔记数量: ${quickInfo["noteCount"]}")
                    println("  - 卡片数量: ${quickInfo["cardCount"]}")
                    println("  - 牌组数量: ${quickInfo["deckCount"]}")
                    println("  - 数据库版本: ${quickInfo["databaseVersion"]}")
                    
                    // 完整解析文件
                    val parsedData = parser.parseApkg(file.absolutePath)
                    
                    println("\n详细解析结果:")
                    println("  - 解析出 ${parsedData.notes.size} 个笔记")
                    println("  - 解析出 ${parsedData.cards.size} 个卡片")
                    println("  - 解析出 ${parsedData.decks.size} 个牌组")
                    println("  - 解析出 ${parsedData.models.size} 个模型")
                    println("  - 解析出 ${parsedData.mediaFiles.size} 个媒体文件")
                    
                    // 显示第一个笔记的内容
                    if (parsedData.notes.isNotEmpty()) {
                        val firstNote = parsedData.notes.first()
                        println("\n第一个笔记:")
                        println("  - ID: ${firstNote.id}")
                        println("  - GUID: ${firstNote.guid}")
                        println("  - 模型 ID: ${firstNote.modelId}")
                        println("  - 字段数量: ${firstNote.fields.size}")
                        firstNote.fields.forEachIndexed { index, field ->
                            println("    [${index}]: ${if (field.length > 50) field.substring(0, 50) + "..." else field}")
                        }
                    }
                    
                    // 显示第一个牌组的信息
                    if (parsedData.decks.isNotEmpty()) {
                        val firstDeck = parsedData.decks.first()
                        println("\n第一个牌组:")
                        println("  - ID: ${firstDeck.id}")
                        println("  - 名称: ${firstDeck.name}")
                        println("  - 描述: ${if (firstDeck.description.isNotBlank()) firstDeck.description else "无"}")
                    }
                    
                    // 显示第一个模型的信息
                    if (parsedData.models.isNotEmpty()) {
                        val firstModel = parsedData.models.first()
                        println("\n第一个模型:")
                        println("  - ID: ${firstModel.id}")
                        println("  - 名称: ${firstModel.name}")
                        println("  - 类型: ${firstModel.type}")
                        println("  - 模板数量: ${firstModel.templates.size}")
                        println("  - 字段数量: ${firstModel.fields.size}")
                    }
                    
                    println("\n" + "=".repeat(50))
                    
                } catch (e: Exception) {
                    println("解析文件失败: ${e.message}")
                }
            } else {
                println("文件不存在: $filePath")
            }
        }
    }

    /**
     * 验证 APKG 文件是否有效
     */
    fun validateApkgFile(filePath: String): Boolean {
        val parser = ApkgParser()
        return parser.isValidApkg(filePath)
    }

    /**
     * 提取 APKG 文件中的所有文本内容
     */
    fun extractAllTextContent(filePath: String): List<String> {
        val parser = ApkgParser()
        val parsedData = parser.parseApkg(filePath)
        
        val allText = mutableListOf<String>()
        
        // 提取所有笔记字段内容
        parsedData.notes.forEach { note ->
            note.fields.forEach { field ->
                if (field.isNotBlank()) {
                    allText.add(field)
                }
            }
        }
        
        // 提取牌组名称和描述
        parsedData.decks.forEach { deck ->
            if (deck.name.isNotBlank()) allText.add(deck.name)
            if (deck.description.isNotBlank()) allText.add(deck.description)
        }
        
        // 提取模型名称和字段名称
        parsedData.models.forEach { model ->
            if (model.name.isNotBlank()) allText.add(model.name)
            model.fields.forEach { field ->
                if (field.name.isNotBlank()) allText.add(field.name)
            }
            model.templates.forEach { template ->
                if (template.name.isNotBlank()) allText.add(template.name)
            }
        }
        
        return allText
    }
}