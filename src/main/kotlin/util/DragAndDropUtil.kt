/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

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