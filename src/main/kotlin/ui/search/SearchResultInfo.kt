package ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.Word
import kotlinx.serialization.ExperimentalSerializationApi
import player.AudioButton
import player.danmaku.TextBox
import state.AppState
import ui.wordscreen.WordScreenState
import tts.AzureTTS
import ui.wordscreen.Morphology

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun SearchResultInfo(
    word: Word,
    appState: AppState,
    wordScreenState: WordScreenState,
    azureTTS: AzureTTS,
){
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()){
        AudioButton(
            word = word,
            state = appState,
            wordScreenState = wordScreenState,
            volume = appState.global.audioVolume,
            pronunciation = wordScreenState.pronunciation,
            azureTTS = azureTTS
        )
    }

    Morphology(
        word = word,
        isPlaying = false,
        searching = true,
        morphologyVisible = true,
        fontSize = appState.global.detailFontSize
    )
    Spacer(Modifier.height(8.dp))
    Divider()
    var tabState by remember { mutableStateOf(0) }

    TabRow(
        selectedTabIndex = tabState,
        backgroundColor = Color.Transparent
    ) {
        Tab(
            text = { Text("中文") },
            selected = tabState == 0,
            onClick = { tabState = 0 }
        )
        Tab(
            text = { Text("英文") },
            selected = tabState == 1,
            onClick = { tabState = 1 }
        )
    }
    when (tabState) {
        0 -> {
            SelectionContainer {
                Text(word.translation,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
            }
        }

        1 -> {
            SelectionContainer {
                Text(text = word.definition,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
            }
        }
    }

    Divider()
}