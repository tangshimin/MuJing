package util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DropTargetDragEvent
import java.io.File
import java.io.IOException
import javax.swing.TransferHandler

/** 创建拖放处理器
 * @param singleFile 是否只接收单个文件
 * @param parseImportFile 处理导入的文件的函数
 * @param showWrongMessage 显示提示信息的函数
 */
fun createTransferHandler(
    singleFile: Boolean = true,
    parseImportFile: (List<File>) -> Unit,
    showWrongMessage: (String) -> Unit,
): TransferHandler {
    return object : TransferHandler() {
        override fun canImport(support: TransferSupport): Boolean {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false
            }
            return true
        }

        override fun importData(support: TransferSupport): Boolean {
            if (!canImport(support)) {
                return false
            }
            val transferable = support.transferable
            try {
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                if (singleFile) {
                    if (files.size == 1) {
                        parseImportFile(files)
                    } else {
                        showWrongMessage("一次只能读取一个文件")
                    }
                } else {
                    parseImportFile(files)
                }


            } catch (exception: UnsupportedFlavorException) {
                return false
            } catch (exception: IOException) {
                return false
            }
            return true
        }
    }
}



@OptIn(ExperimentalComposeUiApi::class)
fun shouldStartDragAndDrop(event: DragAndDropEvent):Boolean{
    var isSupport: Boolean
    val dropEvent = event.nativeEvent as DropTargetDragEvent
    isSupport = dropEvent.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    return isSupport
}