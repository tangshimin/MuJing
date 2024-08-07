package ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.Word
import kotlinx.serialization.ExperimentalSerializationApi
import player.AudioButton
import state.AppState
import state.WordScreenState
import tts.AzureTTS
import ui.word.Morphology

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
    Divider()
    Morphology(
        word = word,
        isPlaying = false,
        searching = true,
        morphologyVisible = true,
        fontSize = appState.global.detailFontSize
    )
    Spacer(Modifier.height(8.dp))
    Divider()
    SelectionContainer {
        Text(text = word.definition,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
    SelectionContainer {
        Text(word.translation,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(top = 8.dp,bottom = 8.dp))
    }
    Divider()
}