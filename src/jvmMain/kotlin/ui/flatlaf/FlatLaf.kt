package ui.flatlaf

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import java.awt.Color
import java.awt.Font
import java.awt.Insets
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.border.LineBorder


/**
 * 初始化 FlatLaf 和文件选择器
 */
fun initializeFileChooser(darkTheme: Boolean): FutureTask<JFileChooser> {
    initializeFlatLaf(darkTheme)
    return setupFileChooser()
}

/**
 * 设置文件选择器
 * 相关链接：https://stackoverflow.com/questions/49792375/jfilechooser-is-very-slow-when-using-windows-look-and-feel
 */
fun setupFileChooser(): FutureTask<JFileChooser> {
    val futureFileChooser = FutureTask { JFileChooser() }
    val executor = Executors.newSingleThreadExecutor()
    executor.execute(futureFileChooser)
    return futureFileChooser
}

/**
 * 初始化 FlatLaf
 */
fun initializeFlatLaf(darkTheme: Boolean) {
    if (darkTheme) FlatDarkLaf.setup()
    else FlatLightLaf.setup()
    UIManager.put("TitlePane.showIcon", false)
    UIManager.put("TitlePane.menuBarEmbedded", true)
    UIManager.put("TitlePane.centerTitle", true)
    UIManager.put("TitlePane.unifiedBackground", false)
    UIManager.put("TitlePane.TitlePane.noIconLeftGap", 0)
    UIManager.put("TitlePane.iconMargins", Insets(0, 0, 0, 0))
    UIManager.put("defaultFont", Font("Default", Font.PLAIN, 16))
    UIManager.put("SplitPaneDivider.gripDotCount", 0)
    UIManager.put("SplitPane.dividerSize", 1)
}

/**
 * 更新 FlatLaf
 */
fun updateFlatLaf(
    darkTheme: Boolean,
    background: Color,
    onBackground: Color,
) {
    if (darkTheme) {
        FlatDarkLaf.setup()

        // Panel
        UIManager.put("Panel.background", Color(18, 18, 18))

        // TitlePane
        UIManager.put("TitlePane.background", Color(18, 18, 18))
        UIManager.put("TitlePane.foreground", Color(133, 144, 151))
        UIManager.put("TitlePane.focusColor", Color(30, 30, 30))

        // ScrollPane
        UIManager.put("ScrollPane.background", Color(62, 66, 68))
        UIManager.put("ScrollPane.foreground", Color(187, 187, 187))
        UIManager.put("ScrollPane.border", LineBorder(Color(55, 55, 55), 1))
        //MenuBar
        UIManager.put("MenuBar.borderColor", Color(55, 55, 55))
        //SplitPane
        UIManager.put("SplitPane.background", Color(30, 30, 30))
        //Tree
        UIManager.put("Tree.background", Color(18, 18, 18))
        UIManager.put("Tree.foreground", Color(133, 144, 151))
        UIManager.put("Tree.icon.closedColor", Color(133, 144, 151))
        UIManager.put("Tree.icon.collapsedColor", Color(133, 144, 151))
        UIManager.put("Tree.icon.leafColor", Color(133, 144, 151))
        UIManager.put("Tree.icon.openColor", Color(133, 144, 151))

        //Table
        UIManager.put("Table.background", Color(18, 18, 18))
        UIManager.put("Table.foreground", Color(133, 144, 151))
        UIManager.put("Table.selectionBackground", Color(30, 30, 30))
        UIManager.put("TableHeader.background", Color(35, 35, 35))
        UIManager.put("Table.gridColor", Color(29, 29, 29))
        UIManager.put("Table.cellFocusColor", Color(13, 152, 6))
        UIManager.put("Table.selectionInactiveForeground", Color(13, 152, 6))
        UIManager.put("TableHeader.bottomSeparatorColor", Color(50, 50, 50))
        UIManager.put("TableHeader.separatorColor", Color(50, 50, 50))

        // TextField
        UIManager.put("TextField.background", Color(18, 18, 18))

        // Highlighter
        UIManager.put("textHighlightText", Color(210, 210, 210))

        // Button
        UIManager.put("Button.toolbar.hoverBackground", Color(31, 31, 31))
        UIManager.put("Button.focusedBorderColor", Color(18, 18, 18))
        UIManager.put("Button.default.focusedBorderColor", Color(18, 18, 18))
        UIManager.put("Button.default.focusColor", Color(18, 18, 18))
        UIManager.put("Button.pressedBackground", Color(18, 18, 18))

    } else {
        FlatLightLaf.setup()
        // Panel
        UIManager.put("Panel.background", background)

        // TitlePane
        val border = Color(224, 224, 224)
        UIManager.put("TitlePane.background", background)
        UIManager.put("TitlePane.foreground", onBackground)
        UIManager.put("TitlePane.focusColor", border)

        // ScrollPane
        UIManager.put("ScrollPane.background", Color(245, 245, 245))
        UIManager.put("ScrollPane.foreground", Color(0, 0, 0))
        UIManager.put("ScrollPane.border", LineBorder(border, 1))

        //MenuBar
        UIManager.put("MenuBar.borderColor", border)

        //SplitPane
        UIManager.put("SplitPane.background", background)
        UIManager.put("Tree.background", background)
        UIManager.put("Tree.foreground", onBackground)

        //Table
        UIManager.put("Table.background", Color(255, 255, 255))
        UIManager.put("Table.foreground", Color(0, 0, 0))
        UIManager.put("Table.selectionBackground", Color(38, 117, 191))
        UIManager.put("TableHeader.background", Color(239, 239, 239))
        UIManager.put("Table.gridColor", Color(235, 235, 235))
        UIManager.put("Table.cellFocusColor", Color(0, 0, 0))
        UIManager.put("Table.selectionInactiveForeground", Color(0, 0, 0))
        UIManager.put("TableHeader.bottomSeparatorColor", Color(230, 230, 230))
        UIManager.put("TableHeader.separatorColor", Color(230, 230, 230))

        // TextField
        UIManager.put("TextField.background", Color(255, 255, 255))

        // Highlighter
        UIManager.put("textHighlightText", background)

        // Button
        UIManager.put("Button.toolbar.hoverBackground", Color(245, 245, 245))
        UIManager.put("Button.focusedBorderColor", background)
        UIManager.put("Button.default.focusedBorderColor", background)
        UIManager.put("Button.default.pressedBackground", background)
        UIManager.put("Button.pressedBackground", background)
    }

    FlatLaf.updateUI()
}

