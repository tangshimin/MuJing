package ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Pagination(totalSize: Int) {
    var startPage by remember { mutableStateOf(1) }
    var currentPage by remember { mutableStateOf(1) }
    val totalPage by remember { mutableStateOf(20) }
    var endPage by remember { mutableStateOf(if (totalPage > 10) 10 else totalPage) }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
//            .width(900.dp)
            .height(60.dp)
    ) {
        Text("共 $totalSize 词")
        Spacer(Modifier.width(16.dp))

        if (startPage > 1) {
            IconButton(
                enabled = currentPage != 1,
                onClick = {
                    startPage = 1
                    currentPage = 1
                    endPage = 10
                }) {
                Text("1", color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }

        IconButton(
            enabled = currentPage != 1,
            onClick = {
                if (currentPage > 0)
                    currentPage -= 1
                if (currentPage < startPage) {
                    startPage = currentPage
                    endPage -= 1
                }

            }) {
            val firstPageColor by animateColorAsState(if (currentPage == 1) Color(0xFFB0BEC5) else MaterialTheme.colors.primary)
            Icon(
                Icons.Filled.NavigateBefore,
                contentDescription = "Localized description",
                tint = firstPageColor
            )
        }


        Pages(
            startPage = startPage,
            currentPage = currentPage,
            setCurrentPage = { currentPage = it },
            endPage = endPage,
        )


        IconButton(
            enabled = currentPage != totalPage,
            onClick = {
                if (currentPage < totalPage) {
                    currentPage += 1
                    if (currentPage > endPage) {
                        endPage = currentPage
                        startPage = currentPage - 9
                    }
                }

            }) {
            val nextPageColor by animateColorAsState(if (currentPage == totalPage) Color(0xFFB0BEC5) else MaterialTheme.colors.primary)
            Icon(
                Icons.Filled.NavigateNext,
                contentDescription = "Localized description",
                tint = nextPageColor
            )
        }
        if (totalPage > 10 && currentPage != totalPage && endPage != totalPage) {
            IconButton(
                enabled = currentPage != totalPage,
                onClick = {
                    startPage = totalPage - 9
                    currentPage = totalPage
                    endPage = totalPage
                }) {
                val lastPageColor by animateColorAsState(if (currentPage == totalPage) Color(0xFFB0BEC5) else MaterialTheme.colors.primary)
                Text("$totalPage", color = lastPageColor, fontWeight = FontWeight.Bold)
            }
        }


    }

}

@Composable
private fun Pages(
    startPage: Int,
    currentPage: Int,
    setCurrentPage: (Int) -> Unit,
    endPage: Int,
) {

    Row {
        for (i in startPage..endPage) {
            var selected = currentPage == i
            IconToggleButton(
                checked = selected,
                onCheckedChange = {
                    setCurrentPage(i)
                }
            ) {
                val color by animateColorAsState(if (selected) Color(0xFFB0BEC5) else MaterialTheme.colors.primary)
                Text("$i", color = color, fontWeight = FontWeight.Bold)
            }
        }
    }

}