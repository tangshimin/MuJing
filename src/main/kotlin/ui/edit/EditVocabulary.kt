package ui.edit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon
import data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getResourcesFile
import state.rememberAppState
import ui.dialog.editWordSwing
import util.loadSvgResource
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.event.*
import javax.swing.table.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter

/**
 * 这个版本的编辑词库不好，等compose desktop 有Table 组件之后再重新写一个版本
 *
 */
@Composable
fun EditVocabulary(
    close: () -> Unit,
    vocabularyPath: String,
    isDarkTheme:Boolean,
    updateFlatLaf:() -> Unit
) {
    val vocabulary by remember { mutableStateOf(loadVocabulary(vocabularyPath)) }
    val fileName by remember{ mutableStateOf(File(vocabularyPath).nameWithoutExtension)}
    //v2.1.6和之前的版本，熟悉词库和困难词库 name 属性可能为空，所以这里需要判断一下
    val title by remember { mutableStateOf(
        when (fileName) {
            "FamiliarVocabulary" -> {
                "编辑词库 - 熟悉词库"
            }
            "HardVocabulary" -> {
                "编辑词库 - 困难词库"
            }
            else -> {
                "编辑词库 - "+vocabulary.name
            }
        }

    ) }

    /** 窗口的大小和位置 */
    val windowState = rememberWindowState(
        size = DpSize(1289.dp, 854.dp),
        position = WindowPosition(Alignment.Center)
    )

    Window(
        title = title,
        state = windowState,
        icon = painterResource("logo/logo.png"),
        resizable = true,
        onCloseRequest = { close() },
    ) {
        Table(
            vocabulary = vocabulary,
            vocabularyPath = vocabularyPath,
        )
    }
    LaunchedEffect(isDarkTheme){
        updateFlatLaf()
    }
}
// 提前检查一遍，查看文件是否正常
fun checkVocabulary(vocabularyPath: String):Boolean{
    var valid = true
    try{
        val file = getResourcesFile(vocabularyPath)
        if (file.exists()) {
            Json.decodeFromString<Vocabulary>(file.readText())
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
        JOptionPane.showMessageDialog(null, "词库解析错误,\n地址：$vocabularyPath\n错误信息" + exception.message)
        valid = false
    }
    return valid
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Table(
    vocabulary: Vocabulary,
    vocabularyPath: String,
) {
    val wordList = vocabulary.wordList
    val cellVisible = loadCellVisibleSwingState()
    val searchState = loadSearchState()
    val appState = rememberAppState()
    var removedColumnSet = mutableSetOf<Pair<String, TableColumn>>()
    val columnNames = arrayOf(
        "  ",
        "单词",
        "中文释义",
        "英文释义",
        "美国音标",
        "英国音标",
        "词形变化",
        "例句",
        "字幕",
    )

    val model: DefaultTableModel = object : DefaultTableModel(columnNames, 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }
    }
    wordList.forEachIndexed { index, word ->
        val captions = displayCaptions(word, vocabulary.type)
        val exchange = displayExchange(word.exchange)
        model.addRow(
            arrayOf(
                index + 1,
                word.value,
                word.translation,
                word.definition,
                word.usphone,
                word.ukphone,
                exchange,
                word.pos,
                captions,
            )
        )
    }
    val table = JTable(model)
    table.setShowGrid(true)
    table.rowSelectionAllowed = false
    table.cellSelectionEnabled = true
    table.autoCreateRowSorter = true

    val sorter = TableRowSorter(model)
    sorter.setComparator(0) { o1, o2 ->
        val i1 = o1.toString().toInt()
        val i2 = o2.toString().toInt()
        i1 - i2
    }
    sorter.setSortable(1, true)
    sorter.setSortable(2, false)
    sorter.setSortable(3, false)
    sorter.setSortable(4, false)
    sorter.setSortable(5, false)
    sorter.setSortable(6, false)
    sorter.setSortable(7, false)
    sorter.setSortable(8, false)
    table.rowSorter = sorter

    // 搜索结果的高亮
    val resultRectangleList = mutableListOf<Pair<Cell, Rectangle>>()
    var resultIndex = 0

    // 保存词库
    var showSaveWindow = false
    val saveVocabulary :(String) -> Unit = {text ->
        if (!showSaveWindow) {
            showSaveWindow = true

            runBlocking {
                val encodeBuilder = Json {
                    prettyPrint = true
                    encodeDefaults = true
                }
                launch {
                    val json = encodeBuilder.encodeToString(vocabulary)
                    val file = getResourcesFile(vocabularyPath)
                    file.writeText(json)
                    notification(
                        text = text,
                        close = { showSaveWindow = false },
                        colors = appState.colors
                    )
                }
            }
        }
    }

    var dialogOpen by remember { mutableStateOf(false) }
    var editRow by remember { mutableStateOf(-1) }
    table.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount > 1) {
                if (!dialogOpen) {
                    dialogOpen = true
                    editRow = table.selectedRow
                    editWordSwing(
                        word = wordList[table.selectedRow].deepCopy(),
                        title = "编辑单词",
                        appState = appState,
                        vocabulary = vocabulary,
                        vocabularyDir = File(vocabularyPath).parentFile!!,
                        save = {
                            // 更新表格
                            val captions = displayCaptions(it, vocabulary.type)
                            val exchange = displayExchange(it.exchange)
                            model.setValueAt(it.value, editRow, 1)
                            model.setValueAt(it.translation, editRow, 2)
                            model.setValueAt(it.definition, editRow, 3)
                            model.setValueAt(it.usphone, editRow, 4)
                            model.setValueAt(it.ukphone, editRow, 5)
                            model.setValueAt(exchange, editRow, 6)
                            model.setValueAt(it.pos, editRow, 7)
                            model.setValueAt(captions, editRow, 8)
                            // 保存词库
                            wordList[table.selectedRow] = it
                            saveVocabulary("保存成功")
                            // 关闭编辑单词窗口
                            dialogOpen = false
                        },
                        close = { dialogOpen = false },
                    )

                }

            }
        }
    })

    val textField = JTextField()
    val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    val outsideBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 175, 0))
    textField.border = CompoundBorder(outsideBorder, insideBorder)
    if (FlatLaf.isLafDark()) {
        textField.selectionColor = Color(33, 66, 131)
        textField.background = Color(30, 30, 30)
    }

    val indexColumn = table.columnModel.getColumn(0)
    val valueColumn = table.columnModel.getColumn(1)
    val translationColumn = table.columnModel.getColumn(2)
    val definitionColumn = table.columnModel.getColumn(3)
    val usPhoneColumn = table.columnModel.getColumn(4)
    val ukPhoneColumn = table.columnModel.getColumn(5)
    val exchangeColumn = table.columnModel.getColumn(6)
    val sentencesColumn = table.columnModel.getColumn(7)
    val captionsColumn = table.columnModel.getColumn(8)


    val textFieldCellEditor = DefaultCellEditor(textField)
    valueColumn.cellEditor = textFieldCellEditor
    usPhoneColumn.cellEditor = textFieldCellEditor
    ukPhoneColumn.cellEditor = textFieldCellEditor

    val textAreaCellEditor = TextAreaCellEditor(FlatLaf.isLafDark())
    translationColumn.cellEditor = textAreaCellEditor
    definitionColumn.cellEditor = textAreaCellEditor
    exchangeColumn.cellEditor = textAreaCellEditor


    indexColumn.cellRenderer = FirstCellRenderer(FlatLaf.isLafDark())
    val customCellRenderer = CustomCellRenderer()
    val rowHeightCellRenderer = RowHeightCellRenderer()

    valueColumn.cellRenderer = customCellRenderer
    usPhoneColumn.cellRenderer = customCellRenderer
    ukPhoneColumn.cellRenderer = customCellRenderer


    translationColumn.cellRenderer = rowHeightCellRenderer
    definitionColumn.cellRenderer = rowHeightCellRenderer
    exchangeColumn.cellRenderer = rowHeightCellRenderer
    sentencesColumn.cellRenderer = rowHeightCellRenderer
    captionsColumn.cellRenderer = rowHeightCellRenderer

    indexColumn.minWidth = 33
    indexColumn.preferredWidth = 45
    indexColumn.maxWidth = 50
    valueColumn.preferredWidth = 150
    translationColumn.preferredWidth = 300
    definitionColumn.preferredWidth = 500
    usPhoneColumn.preferredWidth = 95
    ukPhoneColumn.preferredWidth = 95
    exchangeColumn.preferredWidth = 420
    sentencesColumn.preferredWidth = 350
    captionsColumn.preferredWidth = 350
    table.autoResizeMode = JTable.AUTO_RESIZE_OFF

    val scrollPane =
        JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)


    val onBackgroundColor = if (FlatLaf.isLafDark()) Color(137, 148, 155) else Color.darkGray

    val settings = FlatButton()
    settings.toolTipText = "设置"
    val exportButton = FlatButton()
    exportButton.toolTipText = "导出词库"
    val infoButton = FlatButton()
    infoButton.toolTipText = "词库信息"
    val addButton = FlatButton()
    addButton.toolTipText = "添加单词"
    val removeButton = FlatButton()
    removeButton.toolTipText = "删除单词"
    var isSettingOpen = false
    settings.addActionListener {
        if (!isSettingOpen) {
            isSettingOpen = true
            settingWindow(
                close = { isSettingOpen = false },
                displayColumn = {
                    for (pair in removedColumnSet) {
                        if(pair.first ==it){
                            val column = pair.second
                            table.addColumn(column)
                            break
                        }
                    }
                },
                hideColumn = {columnName ->
                    for( i in 0 until table.columnCount ){
                        if(table.getColumnName(i) == columnName){
                            val column = table.columnModel.getColumn(i)
                            table.removeColumn(column)
                            removedColumnSet.add(columnName to column)
                            break
                        }
                    }
                }
            )
        }
    }

    var showExportScreen = false
    exportButton.addActionListener {
        if(!showExportScreen){
            showExportScreen = true
            exportVocabulary(
                vocabulary = vocabulary,
                vocabularyPath = vocabularyPath,
                futureFileChooser = appState.futureFileChooser,
                close = { showExportScreen = false },
                colors = appState.colors
            )
        }

    }

    var showVocabularyInfo = false
    infoButton.addActionListener {
        if (!showVocabularyInfo) {
            showVocabularyInfo = true
            vocabularyInfoWindow(
                vocabulary = vocabulary,
                vocabularyPath = vocabularyPath,
                close = { showVocabularyInfo = false },
                saveVideoPath = {
                    vocabulary.relateVideoPath = it
                    saveVocabulary("保存成功")
                },
                colors = appState.colors
            )
        }
    }

    settings.preferredSize = Dimension(48, 48)
    exportButton.preferredSize = Dimension(48, 48)
    infoButton.preferredSize = Dimension(48, 48)
    addButton.preferredSize = Dimension(48, 48)
    removeButton.preferredSize = Dimension(48, 48)

    settings.margin = Insets(10, 10, 10, 10)
    exportButton.margin = Insets(10, 10, 10, 10)
    infoButton.margin = Insets(10, 10, 10, 10)
    addButton.margin = Insets(10, 10, 10, 10)
    removeButton.margin = Insets(10, 10, 10, 10)

    settings.buttonType = FlatButton.ButtonType.borderless
    exportButton.buttonType = FlatButton.ButtonType.borderless
    infoButton.buttonType = FlatButton.ButtonType.borderless
    addButton.buttonType = FlatButton.ButtonType.borderless
    removeButton.buttonType = FlatButton.ButtonType.borderless


    val exportIcon = FlatSVGIcon(loadSvgResource("svg/export.svg"))
    exportIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    exportButton.icon = exportIcon

    val settingsIcon = FlatSVGIcon(loadSvgResource("svg/tune.svg"))
    settingsIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    settings.icon = settingsIcon

    val openFileIcon = FlatSVGIcon(loadSvgResource("svg/tune.svg"))
    openFileIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }

    val infoButtonIcon = FlatSVGIcon(loadSvgResource("svg/info.svg"))
    infoButtonIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    infoButton.icon = infoButtonIcon


    val addIcon = FlatSVGIcon(loadSvgResource("svg/add.svg"))
    addIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    addButton.icon = addIcon

    val removeIcon = FlatSVGIcon(loadSvgResource("svg/remove.svg"))
    removeIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    removeButton.icon = removeIcon

    val compsTextField = JTextField(50)
    compsTextField.preferredSize = Dimension(650, 48)
    compsTextField.isFocusable = true

    // search history button
    val searchHistoryButton = JButton(FlatSearchWithHistoryIcon(true))
    searchHistoryButton.preferredSize = Dimension(48, 48)
    searchHistoryButton.toolTipText = "搜索历史记录"
    val searchHistoryList = ArrayList<String>()

    val addKeywordToHistory: () -> Unit = {
        val keyword = compsTextField.text
        if (!searchHistoryList.contains(keyword)) {
            searchHistoryList.add(keyword)
        } else if (searchHistoryList.first() != keyword) {
            searchHistoryList.remove(keyword)
            searchHistoryList.add(keyword)

        }
    }

    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, searchHistoryButton)

    // match case button
    val matchCaseButton = JToggleButton(FlatSVGIcon(loadSvgResource("svg/matchCase.svg")))
    matchCaseButton.rolloverIcon = FlatSVGIcon(loadSvgResource("svg/matchCaseHovered.svg"))
    matchCaseButton.selectedIcon = FlatSVGIcon(loadSvgResource("svg/matchCaseSelected.svg"))
    matchCaseButton.toolTipText = "区分大小写"
    matchCaseButton.isSelected = searchState.matchCaseIsSelected
    // whole words button
    val wordsButton = JToggleButton(FlatSVGIcon(loadSvgResource("svg/words.svg")))
    wordsButton.rolloverIcon = FlatSVGIcon(loadSvgResource("svg/wordsHovered.svg"))
    wordsButton.selectedIcon = FlatSVGIcon(loadSvgResource("svg/wordsSelected.svg"))
    wordsButton.toolTipText = "单词"
    wordsButton.isSelected = searchState.wordsIsSelected
    // regex button
    val regexButton = JToggleButton(FlatSVGIcon(loadSvgResource("svg/regex.svg")))
    regexButton.rolloverIcon = FlatSVGIcon(loadSvgResource("svg/regexHovered.svg"))
    regexButton.selectedIcon = FlatSVGIcon(loadSvgResource("svg/regexSelected.svg"))
    regexButton.toolTipText = "正则表达式"
    regexButton.isSelected = searchState.regexIsSelected
    // index button
    val numberButton = JToggleButton(FlatSVGIcon(loadSvgResource("svg/number.svg")))
    numberButton.rolloverIcon = FlatSVGIcon(loadSvgResource("svg/numberHovered.svg"))
    numberButton.selectedIcon = FlatSVGIcon(loadSvgResource("svg/numberSelected.svg"))
    numberButton.toolTipText = "索引"
    numberButton.isSelected = searchState.numberSelected


    val resultCounter = JLabel("")

    val upButton = FlatButton()
    upButton.toolTipText = "向上搜索"
    val downButton = FlatButton()
    downButton.toolTipText = "向下搜索"
    upButton.isVisible = false
    downButton.isVisible = false

    val upButtonIcon = FlatSVGIcon(loadSvgResource("svg/north.svg"))
    upButtonIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    upButton.icon = upButtonIcon

    val downButtonIcon = FlatSVGIcon(loadSvgResource("svg/south.svg"))
    downButtonIcon.colorFilter = FlatSVGIcon.ColorFilter { onBackgroundColor }
    downButton.icon = downButtonIcon

    val searchUp = {
        if (resultRectangleList.size > 0 && resultIndex >= 0) {
            resultIndex--
            if (resultIndex == -1) resultIndex = resultRectangleList.size - 1
            val cell = resultRectangleList[resultIndex].first
            val rectangle = resultRectangleList[resultIndex].second
            resultCounter.text = "${resultIndex + 1}/${resultRectangleList.size}"
            scrollToCenter(table, cell.row, cell.column)
            table.changeSelection(cell.row, cell.column, false, false)
            table.repaint(rectangle)
            addKeywordToHistory()
        }
    }
    val searchDown = {
        if (resultRectangleList.size > 0 && resultIndex >= 0) {
            resultIndex++
            if (resultIndex == resultRectangleList.size) resultIndex = 0
            val cell = resultRectangleList[resultIndex].first
            val rectangle = resultRectangleList[resultIndex].second

            resultCounter.text = "${resultIndex + 1}/${resultRectangleList.size}"

            scrollToCenter(table, cell.row, cell.column)
            table.changeSelection(cell.row, cell.column, false, false)
            table.repaint(rectangle)
            addKeywordToHistory()
        }
    }

    upButton.addActionListener { searchUp() }
    downButton.addActionListener { searchDown() }

    // search toolbar
    val searchToolbar = JToolBar()
    searchToolbar.isFocusable = true
    searchToolbar.focusTraversalKeysEnabled = true
    searchToolbar.add(matchCaseButton)
    searchToolbar.add(wordsButton)
    searchToolbar.addSeparator()
    searchToolbar.add(regexButton)
    searchToolbar.add(numberButton)

    searchToolbar.add(upButton)
    searchToolbar.add(downButton)

    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, searchToolbar)
    compsTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
    compsTextField.putClientProperty(FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, true)


    val cleanHighLight = {
        resultCounter.text = ""
        upButton.isVisible = false
        downButton.isVisible = false
        sorter.rowFilter = null
        val columns = if (numberButton.isSelected) 0 else table.columnCount - 1
        val i = if (numberButton.isSelected) 0 else 1
        for (col in i..columns) {
            val renderer = table.columnModel.getColumn(col).cellRenderer as HighLightCell
            renderer.clear()
        }
        table.repaint()
    }

    val search = {
        val length = compsTextField.text.length
        val searchTime = System.currentTimeMillis()
        resultRectangleList.clear()
        resultIndex = 0

        if (length != 0) {
            val keyword = compsTextField.text
            var filterRegex = keyword
            val pattern: Pattern
            try {

                if (wordsButton.isSelected) {
                    // 匹配单词
                    filterRegex = "\\b$filterRegex\\b"
                }
                if (!regexButton.isSelected && !wordsButton.isSelected) {
                    filterRegex = Pattern.quote(filterRegex)
                }
                if (!matchCaseButton.isSelected) {
                    // 不区分大小写
                    filterRegex = "(?i)$filterRegex"
                }
                // 匹配索引，启用这个选项后，其他选项都会被禁用
                if (numberButton.isSelected) {
                    filterRegex = "\\b$filterRegex\\b"
                }
                pattern = Pattern.compile(filterRegex)
                val rows = table.rowCount - 1
                val columns = if (numberButton.isSelected) 0 else table.columnCount - 1
                val i = if (numberButton.isSelected) 0 else 1
                for (row in 0..rows) {
                    for (column in i..columns) {
                        val value = table.getValueAt(row, column).toString()
                        val matcher = pattern.matcher(value)
                        var finded = false
                        val highlightSpans = mutableMapOf<Int, Int>()
                        while (matcher.find()) {
                            finded = true
                            val start = matcher.start()
                            val end = matcher.end()
                            highlightSpans[start] = end
                        }

                        if (finded) {
                            val renderer = table.getCellRenderer(row, column) as HighLightCell
                            renderer.setSearchTime(searchTime)
                            val cell = Cell(row, column)
                            renderer.addHighlightCell(cell, highlightSpans)
                            renderer.setKeyword(keyword)
                            val rectangle = table.getCellRect(row, column, true)
                            val pair = cell to rectangle
                            resultRectangleList.add(pair)
                            table.repaint(rectangle)
                        }
                    }
                }

            } catch (syntaxException: PatternSyntaxException) {
                println(syntaxException.description)
            }

            resultCounter.text =
                if (resultRectangleList.size > 0) "${resultIndex + 1}/${resultRectangleList.size}" else "0"
            if (resultRectangleList.isNotEmpty()) {
                upButton.isVisible = true
                downButton.isVisible = true
            }
            if (resultRectangleList.size > 0) {
                val cell = resultRectangleList[resultIndex].first
                table.changeSelection(cell.row, cell.column, false, false)
                val rectangle = resultRectangleList[resultIndex].second
                table.scrollRectToVisible(rectangle)
            }

        } else {
            cleanHighLight()
        }

    }

    val setPlaceholder = {
        var placeholderText = ""
        if (matchCaseButton.isSelected && !wordsButton.isSelected && !regexButton.isSelected) {
            placeholderText += "区分大小写"
        } else if (matchCaseButton.isSelected && (wordsButton.isSelected || regexButton.isSelected)) {
            placeholderText += "区分大小写和"
        }

        if (wordsButton.isSelected && !regexButton.isSelected) {
            placeholderText += "单词"
        }else if(regexButton.isSelected){
            placeholderText += "单词和"
        }
        if (regexButton.isSelected) {
            placeholderText += "正则表达式"
        }
        // 最后一个具有排他性
        if (numberButton.isSelected) {
            placeholderText = "索引"
        }
        compsTextField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholderText)
    }
    setPlaceholder()
    searchHistoryButton.addActionListener {
        val popupMenu = JPopupMenu()
        if (searchHistoryList.isEmpty()) {
            popupMenu.add("(empty)")
        } else {
            for (history in searchHistoryList.reversed()) {
                val menuItem = JMenuItem(history)
                menuItem.addActionListener {
                    compsTextField.text = history
                    search()
                }
                popupMenu.add(menuItem)
            }
        }

        popupMenu.show(searchHistoryButton, 0, searchHistoryButton.height)
    }
    matchCaseButton.addActionListener {
        searchState.matchCaseIsSelected = matchCaseButton.isSelected
        searchState.saveSearchState()
        if(matchCaseButton.isSelected){
            numberButton.isSelected = false
            searchState.numberSelected = false
        }

        setPlaceholder()
        cleanHighLight()
        search()
    }
    wordsButton.addActionListener {
        searchState.wordsIsSelected = wordsButton.isSelected
        searchState.saveSearchState()
        if(wordsButton.isSelected){
            numberButton.isSelected = false
            searchState.numberSelected = false
        }

        setPlaceholder()
        cleanHighLight()
        search()
    }
    regexButton.addActionListener {
        searchState.regexIsSelected = regexButton.isSelected
        searchState.saveSearchState()

        if(regexButton.isSelected){
            searchState.numberSelected = false
            numberButton.isSelected = false
        }


        setPlaceholder()
        cleanHighLight()
        search()
    }
    numberButton.addActionListener {
        searchState.numberSelected = numberButton.isSelected
        searchState.saveSearchState()

        if(numberButton.isSelected){
            matchCaseButton.isSelected = false
            wordsButton.isSelected = false
            regexButton.isSelected = false

            searchState.matchCaseIsSelected = false
            searchState.wordsIsSelected = false
            searchState.regexIsSelected = false
        }
        setPlaceholder()
        cleanHighLight()
        search()
    }
    compsTextField.document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(documentEvent: DocumentEvent?) {
            search()
        }

        override fun changedUpdate(documentEvent: DocumentEvent?) {
            search()
        }

        override fun removeUpdate(documentEvent: DocumentEvent?) {
            search()
        }

    })
    compsTextField.addKeyListener(object : KeyListener {
        override fun keyTyped(keyEvent: KeyEvent) {}
        override fun keyReleased(keyEvent: KeyEvent) {}
        override fun keyPressed(keyEvent: KeyEvent) {
            when (keyEvent.keyCode) {
                10 -> {
                    searchDown()
                }

                38 -> {
                    searchUp()
                }

                40 -> {
                    searchDown()
                }
            }
        }

    })

    val addRow = {
        if (!dialogOpen) {
            dialogOpen = true
            val index = model.rowCount + 1
            editWordSwing(
                word = Word(value = ""),
                title = "添加单词",
                appState = appState,
                vocabulary = vocabulary,
                vocabularyDir = File(vocabularyPath).parentFile!!,
                save = {
//                    dialogOpen = false
                    val captions = displayCaptions(it, vocabulary.type)
                    val exchange = displayExchange(it.exchange)
                    val row = arrayOf(
                        index,
                        it.value,
                        it.translation,
                        it.definition,
                        it.usphone,
                        it.ukphone,
                        captions,
                        it.pos,
                        exchange
                    )
                    model.addRow(row)
                    wordList.add(it)
                    vocabulary.size = wordList.size
                    saveVocabulary("添加成功")
                    // 滚动到最后以后,还差一点，第一次不会滚动到最底部，后面几次可以看到最后一行，但是没有完全显示。
                    table.changeSelection(table.getRowCount()-1, 1,false,false)
                },
                close = { dialogOpen = false },
            )
        }

    }

    val removeRow = {
        val rows = table.selectedRows
        if (rows.isNotEmpty()) {
            for (row in rows.reversed()) {
                model.removeRow(row)
                vocabulary.wordList.removeAt(row)
            }
            vocabulary.size = wordList.size
            saveVocabulary("保存成功")
        }
        // 如果搜索框里还有单词，再重新搜索一次，重建单词高亮。
        search()
    }

    addButton.addActionListener { addRow() }
    removeButton.addActionListener { removeRow() }

    val toolPanel = JPanel()
    toolPanel.layout = BoxLayout(toolPanel, BoxLayout.X_AXIS)
    toolPanel.border = BorderFactory.createMatteBorder(1, 1, 0, 0, table.gridColor)
    compsTextField.border = BorderFactory.createMatteBorder(0, 1, 0, 1, table.gridColor)
    scrollPane.border = BorderFactory.createMatteBorder(1, 1, 1, 1, table.gridColor)
    toolPanel.add(settings)
    toolPanel.add(exportButton)
    toolPanel.add(infoButton)
    toolPanel.add(addButton)
    toolPanel.add(removeButton)
    toolPanel.add(compsTextField)
    toolPanel.add(resultCounter)

    val topPanel = JPanel()
    topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
    topPanel.add(toolPanel)

    val popupMenu = JPopupMenu()
    val removeSelected = JMenuItem("删除单词", removeIcon)
    removeSelected.addActionListener { removeRow() }
    // 暂时不添加增加行的功能
    popupMenu.add(removeSelected)
    table.componentPopupMenu = popupMenu

    removedColumnSet = displayOrHideColumn(cellVisible, table)
    scrollPane.requestFocusInWindow()
    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            JPanel().apply {
                layout = BorderLayout()
                add(topPanel, BorderLayout.NORTH)
                add(scrollPane, BorderLayout.CENTER)
            }
        },
    )
}


fun displayExchange(exchangeStr: String): String {
    var str = ""
    val exchanges = exchangeStr.split("/")
    var preterite = ""
    var pastParticiple = ""
    var presentParticiple = ""
    var third = ""
    var er = ""
    var est = ""
    var plural = ""
    var lemma = ""

    exchanges.forEach { exchange ->
        val pair = exchange.split(":")
        when (pair[0]) {
            "p" -> {
                preterite = pair[1]
            }

            "d" -> {
                pastParticiple = pair[1]
            }

            "i" -> {
                presentParticiple = pair[1]
            }

            "3" -> {
                third = pair[1]
            }

            "r" -> {
                er = pair[1]
            }

            "t" -> {
                est = pair[1]
            }

            "s" -> {
                plural = pair[1]
            }

            "0" -> {
                lemma = pair[1]
            }

        }
    }
    if (lemma.isNotEmpty()) {
        str += "原型 $lemma；"
    }
    if (preterite.isNotEmpty()) {
        str += "过去式 $preterite；"
    }
    if (pastParticiple.isNotEmpty()) {
        str += "过去分词 $pastParticiple；"
    }
    if (presentParticiple.isNotEmpty()) {
        str += "现在分词 $presentParticiple；"
    }
    if (third.isNotEmpty()) {
        str += "第三人称单数 $third；"
    }
    if (er.isNotEmpty()) {
        str += "比较级 $er；"
    }
    if (est.isNotEmpty()) {
        str += "最高级 $est；"
    }
    if (plural.isNotEmpty()) {
        str += "复数 $plural；"
    }
    return str
}

fun displayCaptions(word: Word, vocabularyType: VocabularyType): String {
    var captions = ""
    if (vocabularyType == VocabularyType.DOCUMENT) {
        word.externalCaptions.forEachIndexed { i, caption ->
            val num = "\n" + (i + 1).toString() + ". "
            captions += num + caption.content
            val isNewline = i + 1 == word.captions.size
            if (!isNewline) captions += "\r\n"
        }


    } else {
        word.captions.forEachIndexed { index, caption ->
            val num = "\n" + (index + 1).toString() + ". "
            captions += num + caption.content
            val isNewline = index + 1 == word.captions.size
            if (!isNewline) captions += "\r\n"
        }

    }
    return captions
}

fun scrollToCenter(table: JTable, rowIndex: Int, vColIndex: Int) {
    if (table.parent !is JViewport) {
        return
    }
    val viewport = table.parent as JViewport
    val rect = table.getCellRect(rowIndex, vColIndex, true)
    val viewRect = viewport.viewRect
    rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y)
    var centerX = (viewRect.width - rect.width) / 2
    var centerY = (viewRect.height - rect.height) / 2
    if (rect.x < centerX) {
        centerX = -centerX
    }
    if (rect.y < centerY) {
        centerY = -centerY
    }
    rect.translate(centerX, centerY)
    viewport.scrollRectToVisible(rect)
}

// 隐藏和显示列
fun displayOrHideColumn(cellVisibleState: CellVisibleSwingState, table: JTable): MutableSet<Pair<String, TableColumn>> {
    val hideColumnList: MutableList<Pair<String,Int>> = mutableListOf()
    val removedColumnList = mutableSetOf<Pair<String,TableColumn>>()
    if (!cellVisibleState.translationVisible) {
        hideColumnList.add("中文释义" to 2)
    }

    if (!cellVisibleState.definitionVisible) {
        hideColumnList.add("英文释义" to 3)
    }
    if (!cellVisibleState.uKPhoneVisible) {
        hideColumnList.add("美国音标" to 4)
    }
    if (!cellVisibleState.usPhoneVisible) {
        hideColumnList.add("英国英标" to 5)
    }
    if (!cellVisibleState.exchangeVisible) {
        hideColumnList.add("词形变化" to 6)
    }
    if (!cellVisibleState.sentencesVisible) {
        hideColumnList.add("例句" to 7)
    }
    if (!cellVisibleState.captionsVisible) {
        hideColumnList.add("字幕" to 8)
    }
    hideColumnList.reverse()
    hideColumnList.forEach { (columnName,columnIndex) ->
        val column = table.columnModel.getColumn(columnIndex)
        removedColumnList.add(columnName to column)
        table.removeColumn(column)
    }
    return removedColumnList
}

data class Cell(val row: Int, val column: Int)
class CustomCellRenderer : JTextField(), TableCellRenderer, HighLightCell {
    private var highlightCells: HashMap<Cell, MutableMap<Int, Int>> = HashMap()
    private var lastTime: Long = -1L
    private var keyword: String = ""

    override fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>) {
        highlightCells[cell] = highlightSpans
    }

    override fun clear() {
        highlightCells.clear()

    }

    override fun setSearchTime(lastTime: Long) {
        if (lastTime != this.lastTime) {
            highlightCells.clear()
        }
        this.lastTime = lastTime
    }

    override fun setKeyword(keyword: String) {
        this.keyword = keyword
    }


    override fun getTableCellRendererComponent(
        table: JTable, value: Any,
        isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        text = value.toString()
        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        val outsideBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, table.gridColor)
        val focusBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 161, 0))
        border = CompoundBorder(outsideBorder, insideBorder)

        if (isSelected) {
            background = table.selectionBackground
            foreground = table.selectionForeground
        } else {
            background = table.background
            foreground = table.foreground
        }


        val cell = Cell(row, column)
        val spans = highlightCells[cell]
        if (!spans.isNullOrEmpty()) {
            spans.forEach { (start, end) ->
                highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
            }
        }

        if (highlightCells.contains(Cell(row, column)) && isSelected) {
            background = table.background
            foreground = table.foreground
            border = CompoundBorder(focusBorder, insideBorder)
        }


        return this
    }

}

internal class FirstCellRenderer(private val darkTheme: Boolean) : JTextField(), TableCellRenderer, HighLightCell {
    private var highlightCells: HashMap<Cell, MutableMap<Int, Int>> = HashMap()
    private var lastTime: Long = -1L
    private var keyword: String = ""
    override fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>) {
        highlightCells[cell] = highlightSpans
    }


    override fun setSearchTime(lastTime: Long) {
        if (lastTime != this.lastTime) {
            highlightCells.clear()
        }
        this.lastTime = lastTime
    }

    override fun setKeyword(keyword: String) {
        this.keyword = keyword
    }

    override fun clear() {
        highlightCells.clear()
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        text = value.toString()
        if (darkTheme) {
            background = Color(59, 59, 59)
            background = Color(35, 35, 35)
            border = (BorderFactory.createMatteBorder(0, 0, 1, 1, Color(45, 45, 45)))
        } else {
            background = Color(239, 239, 239)
            border = (BorderFactory.createMatteBorder(0, 0, 1, 1, Color(219, 219, 219)))
        }
        horizontalAlignment = JLabel.CENTER

        if (isSelected) {
            table.addColumnSelectionInterval(0, table.columnCount - 1)
            if (darkTheme) {
                background = table.selectionBackground
            }
        }

        val cell = Cell(row, column)
        val spans = highlightCells[cell]
        if (!spans.isNullOrEmpty()) {
            spans.forEach { (start, end) ->
                highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
            }
        }
        if (highlightCells.contains(Cell(row, column)) && isSelected) {
            background = table.background
            foreground = table.foreground
            border = (BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 161, 0)))
        }

        return this
    }

}


internal open class RowHeightCellRenderer : JTextArea(), TableCellRenderer, HighLightCell {
    private var highlightCells: HashMap<Cell, MutableMap<Int, Int>> = HashMap()
    private var lastTime: Long = -1L
    private var keyword: String = ""

    override fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>) {
        highlightCells[cell] = highlightSpans
    }

    override fun setSearchTime(lastTime: Long) {
        if (lastTime != this.lastTime) {
            highlightCells.clear()
        }
        this.lastTime = lastTime
    }

    override fun setKeyword(keyword: String) {
        this.keyword = keyword
    }

    override fun clear() {
        highlightCells.clear()
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        text = value as String?
        lineWrap = true
        wrapStyleWord = true
        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        val outsideBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, table.gridColor)
        val focusBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 161, 0))
        border = CompoundBorder(outsideBorder, insideBorder)

        if (isSelected) {
            background = table.selectionBackground
            foreground = table.selectionForeground

        } else {
            background = table.background
            foreground = table.foreground
        }

        val cell = Cell(row, column)
        val spans = highlightCells[cell]
        if (!spans.isNullOrEmpty()) {
            spans.forEach { (start, end) ->
                highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter)
            }
        }

        if (highlightCells.contains(Cell(row, column)) && isSelected) {
            background = table.background
            foreground = table.foreground
            border = CompoundBorder(focusBorder, insideBorder)
        }


        // Set the component width to match the width of its table cell
        // and make the height arbitrarily large to accomodate all the contents
        setSize(table.columnModel.getColumn(column).width, Short.MAX_VALUE.toInt())

        // Now get the fitted height for the given width
        val rowHeight = this.preferredSize.height


        // Get the current table row height
        val actualRowHeight = table.getRowHeight(row)

        // Set table row height to fitted height.
        // Important to check if this has been done already
        // to prevent a never-ending loop.
        if (rowHeight > actualRowHeight) {
            table.setRowHeight(row, rowHeight)
        }

        validate()
        return this
    }

    override fun getPreferredSize(): Dimension {
        try {
            // Get Rectangle for position after last text-character
            val rectangle: Rectangle? = modelToView(document.length)
            if (rectangle != null) {
                return Dimension(
                    width,
                    this.insets.top + rectangle.y + rectangle.height +
                            this.insets.bottom
                )
            }
        } catch (e: BadLocationException) {
            e.printStackTrace()
        }
        return super.getPreferredSize()
    }

}


internal class TextAreaCellEditor(private val darkTheme: Boolean) : TableCellEditor {
    private var listenerList = EventListenerList()
    private val textArea = JTextArea()
    override fun getCellEditorValue(): Any {
        return textArea.text
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
    ): Component {
        textArea.margin = Insets(5, 5, 5, 5)
        textArea.lineWrap = true
        textArea.font = table.font
        textArea.text = value.toString()
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val insideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        val outsideBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, Color(9, 175, 0))
        textArea.border = CompoundBorder(outsideBorder, insideBorder)
        if (darkTheme) {
            textArea.selectionColor = Color(33, 66, 131)
            textArea.background = Color(30, 30, 30)
        }

        return textArea
    }

    override fun isCellEditable(event: EventObject): Boolean {
        return if (event is MouseEvent) {
            event.clickCount > 1
        } else {
            false
        }

    }

    override fun shouldSelectCell(e: EventObject): Boolean {
        return true
    }

    override fun stopCellEditing(): Boolean {
        fireEditingStopped()
        return true
    }

    override fun cancelCellEditing() {
        fireEditingCanceled()
    }

    override fun addCellEditorListener(l: CellEditorListener) {
        listenerList.add(CellEditorListener::class.java, l)
    }

    override fun removeCellEditorListener(l: CellEditorListener) {
        listenerList.remove(CellEditorListener::class.java, l)
    }

    private fun fireEditingStopped() {
        val listeners = listenerList.listenerList
        for (i in listeners.indices) {
            if (listeners[i] is CellEditorListener) {
                (listeners[i] as CellEditorListener).editingStopped(ChangeEvent(this))
            }
        }
    }

    private fun fireEditingCanceled() {
        val listeners = listenerList.listenerList
        for (i in listeners.indices) {
            if (listeners[i] is CellEditorListener) {
                (listeners[i] as CellEditorListener).editingCanceled(ChangeEvent(this))
            }
        }
    }

}





