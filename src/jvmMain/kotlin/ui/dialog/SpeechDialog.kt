package ui.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import tts.AzureTTS
import tts.Voice
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import player.rememberAudioPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun AzureTTSDialog(
    close:() -> Unit,
    azureTTS: AzureTTS
){
    DialogWindow(
        title = "设置 Azure TTS",
        icon = painterResource("logo/logo.png"),
        onCloseRequest = { close() },
        resizable = false,
        state = rememberDialogState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(645.dp, 750.dp)
        ),
    ) {
        Box{
            Column (Modifier.fillMaxSize().background(MaterialTheme.colors.background).padding(start = 10.dp)){
                // 支持 Azure Speech 的区域
                val regionList = listOf("southafricanorth","eastasia","southeastasia","australiaeast","centralindia","japaneast","japanwest","koreacentral","canadacentral","northeurope","westeurope","francecentral","germanywestcentral","norwayeast","swedencentral8","switzerlandnorth","switzerlandwest","uksouth","uaenorth","brazilsouth","qatarcentral3,8","centralus","eastus","eastus2","northcentralus","southcentralus","westcentralus","westus","westus2","westus3","southafricanorth","eastasia","southeastasia","australiaeast","centralindia","japaneast","japanwest","koreacentral","canadacentral","northeurope","westeurope","francecentral","germanywestcentral","norwayeast","swedencentral8","switzerlandnorth","switzerlandwest","uksouth","uaenorth","brazilsouth","qatarcentral3,8","centralus","eastus","eastus2","northcentralus","southcentralus","westcentralus","westus","westus2","westus3")
                val voiceList = remember{ mutableStateListOf<Voice>() }
                val border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                val fontSize = 21.sp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)){
                    Text("SpeechKey      ")
                    BasicTextField(
                        value = azureTTS.subscriptionKey,
                        onValueChange = {
                            azureTTS.subscriptionKey = it
                            azureTTS.saveAzureState()
                        },
                        textStyle = TextStyle(
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        decorationBox = {innerTextField ->
                            Box{
                                Box(Modifier.padding(start = 8.dp,end = 8.dp).align(Alignment.CenterStart)){
                                    innerTextField()
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp).border(border = border)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(48.dp)){
                    Text("SpeechRegion ")
                    BasicTextField(
                        value = azureTTS.region,
                        onValueChange = {
                            azureTTS.region = it
                            azureTTS.saveAzureState()
                        },
                        textStyle = TextStyle(
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onBackground
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        decorationBox = {innerTextField ->
                            Box{
                                Box(Modifier.padding(start = 8.dp,end = 8.dp).align(Alignment.CenterStart)){
                                    innerTextField()
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp).border(border = border)
                    )
                }

                LaunchedEffect(azureTTS.subscriptionKey, azureTTS.region,azureTTS.pronunciationStyle){
                    if(azureTTS.subscriptionKeyIsValid() && azureTTS.regionIsValid()){
                        runBlocking {
                            voiceList.clear()
                            val list = azureTTS.getVoicesList()
                            voiceList.addAll(list)
                        }
                    }
                }

                if(voiceList.isNotEmpty()){
                    var shortName by remember{ mutableStateOf(azureTTS.shortName) }
                    var displayName by remember{ mutableStateOf(azureTTS.displayName ) }
                    var gender by remember{ mutableStateOf(azureTTS.gender ) }

                    LaunchedEffect(azureTTS.pronunciationStyle){
                        var contain = false
                        for (voice in voiceList) {
                            if(voice.ShortName == shortName){
                                contain = true
                                break
                            }
                        }
                        if(!contain) {
                            shortName = voiceList.first().ShortName
                            displayName = voiceList.first().DisplayName
                        }

                    }
                    Spacer(Modifier.height(8.dp))
                    Box{
                        var showStyle by remember{ mutableStateOf(false) }

                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("口音")

                            Spacer(Modifier.width(15.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(270.dp).height(48.dp).padding(start = 16.dp, end = 8.dp)
                                    .border(border = border)
                                    .clickable { showStyle = !showStyle}
                            ) {
                                Text(text = if(azureTTS.pronunciationStyle == "en-GB") "英式发音" else "美式发音",
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = MaterialTheme.colors.onBackground)
                                val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "Localized description",
                                    tint = tint,
                                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                                )
                            }

                        }

                        if(showStyle){
                            DropdownMenu(
                                expanded = showStyle,
                                onDismissRequest = { showStyle = false },
                                offset = DpOffset(64.dp, (-48).dp),
                                modifier = Modifier.width(246.dp).height(100.dp)
                            ) {
                                Column(Modifier.width(246.dp).height(80.dp)){
                                    DropdownMenuItem(
                                        onClick = {
                                            azureTTS.pronunciationStyle = "en-GB"
                                            showStyle = false
                                            azureTTS.saveAzureState()
                                        },
                                        modifier = Modifier.width(246.dp).height(40.dp)
                                    ){

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                            val color = if(azureTTS.pronunciationStyle == "en-GB")  MaterialTheme.colors.primary else  Color.Transparent
                                            Spacer(Modifier
                                                .background(color)
                                                .height(20.dp)
                                                .width(2.dp)
                                            )
                                            Text(
                                                text = "英式发音",
                                                color = if(azureTTS.pronunciationStyle == "en-GB") MaterialTheme.colors.primary else  Color.Unspecified,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }

                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            azureTTS.pronunciationStyle = "en-US"
                                            showStyle = false
                                            azureTTS.saveAzureState()
                                        },
                                        modifier = Modifier.width(246.dp).height(40.dp)
                                    ){
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                            val color = if(azureTTS.pronunciationStyle == "en-US")  MaterialTheme.colors.primary else  Color.Transparent
                                            Spacer(Modifier
                                                .background(color)
                                                .height(20.dp)
                                                .width(2.dp)
                                            )

                                            Text(
                                                text = "美式发音",
                                                color = if(azureTTS.pronunciationStyle == "en-US") MaterialTheme.colors.primary else  Color.Unspecified,
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                    }
                                }




                            }

                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box{
                        var showList by remember{ mutableStateOf(false) }

                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("语音")

                            Spacer(Modifier.width(15.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(270.dp).height(48.dp).padding(start = 16.dp, end = 8.dp)
                                    .border(border = border)
                                    .clickable { showList = !showList}
                            ) {
                                Text(text = "$displayName  $gender",
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = MaterialTheme.colors.onBackground)
                                val tint = if (MaterialTheme.colors.isLight) Color.DarkGray else MaterialTheme.colors.onBackground
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = "Localized description",
                                    tint = tint,
                                    modifier = Modifier.size(48.dp, 48.dp).padding(top = 12.dp, bottom = 12.dp)
                                )
                            }

                        }
                        DropdownMenu(
                            expanded = showList,
                            onDismissRequest = { showList = false },
                            offset = DpOffset(65.dp, (-48).dp),
                            modifier = Modifier.width(246.dp).height(460.dp)
                        ) {
                            Box(Modifier.width(246.dp).height(460.dp)){
                                val scrollState = rememberLazyListState()
                                LazyColumn(Modifier.fillMaxSize(),scrollState){
                                    items(voiceList){voice ->
                                        if(voice.Locale == azureTTS.pronunciationStyle){
                                            DropdownMenuItem(
                                                onClick = {
//                                                    selectedVoiceShortName = voice
                                                    shortName = voice.ShortName
                                                    displayName = voice.DisplayName
                                                    gender = voice.Gender

                                                    azureTTS.shortName = voice.ShortName
                                                    azureTTS.displayName = voice.DisplayName
                                                    azureTTS.gender = voice.Gender
                                                    showList = false
                                                    azureTTS.saveAzureState()
                                                },
                                                modifier = Modifier.width(246.dp).height(40.dp)
                                            ){

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Start,
                                                    modifier = Modifier.fillMaxWidth().height(40.dp)) {
                                                    val color = if(  shortName == voice.ShortName)  MaterialTheme.colors.primary else  Color.Transparent
                                                    Spacer(Modifier
                                                        .background(color)
                                                        .height(20.dp)
                                                        .width(2.dp)
                                                    )
                                                    Text(
                                                        text = voice.DisplayName+" - "+voice.Gender,
                                                        color = if(shortName== voice.ShortName) MaterialTheme.colors.primary else  Color.Unspecified,
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                    }
                                }
                                VerticalScrollbar(
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    adapter = rememberScrollbarAdapter(scrollState = scrollState),
                                )
                            }

                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                           .height(60.dp)
                            .border(border = border)

                    ) {
                        val testSentence = "The quick brown fox jumps over the lazy dog"
                        val audioPlayerComponent = rememberAudioPlayerComponent()
                        var isPlaying by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()
                        Spacer(Modifier.width(8.dp))
                        Text(testSentence)
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                              val path =   azureTTS.textToSpeech(testSentence)
                                if(!path.isNullOrEmpty()){
                                    isPlaying = true
                                    audioPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

                                        override fun finished(mediaPlayer: MediaPlayer) {
                                            isPlaying = false
                                            audioPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(this)
                                        }
                                    })
                                    audioPlayerComponent.mediaPlayer().media().play(path)
                                }

                            }
                        }, enabled = !isPlaying){
                            Text("测试",)
                        }
                        Spacer(Modifier.width(8.dp))
                        DisposableEffect(Unit){
                            onDispose {
                                // TODO 可能要重构保存逻辑
                                azureTTS.saveAzureState()
                                audioPlayerComponent.mediaPlayer().release()
                            }}
                    }



                }

            }

            OutlinedButton(onClick = close,modifier = Modifier.padding(bottom = 10.dp).align(Alignment.BottomCenter)){
                Text("关闭")
            }
        }

    }
}







