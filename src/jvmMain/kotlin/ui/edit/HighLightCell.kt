package ui.edit

interface HighLightCell {
    fun addHighlightCell(cell: Cell, highlightSpans: MutableMap<Int, Int>)
    fun clear()
    fun setSearchTime(lastTime: Long)
    fun setKeyword(keyword: String)
}