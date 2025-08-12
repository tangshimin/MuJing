package util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDragEvent
import java.io.File


val shouldStartDragAndDrop: (startEvent: DragAndDropEvent) -> Boolean = { event ->
    shouldStartDragAndDrop(event)
}

@OptIn(ExperimentalComposeUiApi::class)
fun shouldStartDragAndDrop(event: DragAndDropEvent):Boolean{
    var isSupport: Boolean
    val dropEvent = event.nativeEvent as DropTargetDragEvent
    isSupport = dropEvent.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
    return isSupport
}

@OptIn(ExperimentalComposeUiApi::class)
fun createDragAndDropTarget(
    onFilesDropped: (List<File>) -> Unit
): DragAndDropTarget {
    return object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferable = event.awtTransferable
            val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                ?.filterIsInstance<File>() ?: emptyList()
            onFilesDropped(files)
            return true
        }
    }
}