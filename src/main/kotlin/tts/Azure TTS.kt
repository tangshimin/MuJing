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

package tts

import androidx.compose.runtime.*
import data.Crypt
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import state.getAudioDirectory
import state.getSettingsDirectory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

@ExperimentalSerializationApi
@Serializable
data class Voice(
    val DisplayName: String,
    val ShortName: String,
    val Locale: String,
    val Gender: String,
)
@ExperimentalSerializationApi
@Serializable
data class AzureTTSData(
    val subscriptionKey: String = "",
    val region: String = "",
    val pronunciationStyle: String = "en-US",
    val shortName: String = "en-US-AvaNeural",
    val displayName: String = "Ava",
    val gender: String = "Female"
)
@OptIn(ExperimentalSerializationApi::class)
class AzureTTS(
    azureTTSData: AzureTTSData
) {
    private var tokenExpiryTime by   mutableStateOf< LocalDateTime?>(null)
    private var token by mutableStateOf<String?>(null)
    var pronunciationStyle by mutableStateOf( azureTTSData.pronunciationStyle)
    var shortName: String by mutableStateOf(azureTTSData.shortName)
    var displayName: String by mutableStateOf(azureTTSData.displayName)
    var gender: String by mutableStateOf(azureTTSData.gender)
    var subscriptionKey: String by mutableStateOf(azureTTSData.subscriptionKey)
    var region: String by mutableStateOf(azureTTSData.region)

    // 支持 Azure Speech 的区域
    private val regionList = listOf("southafricanorth","eastasia","southeastasia","australiaeast","centralindia","japaneast","japanwest","koreacentral","canadacentral","northeurope","westeurope","francecentral","germanywestcentral","norwayeast","swedencentral8","switzerlandnorth","switzerlandwest","uksouth","uaenorth","brazilsouth","qatarcentral3,8","centralus","eastus","eastus2","northcentralus","southcentralus","westcentralus","westus","westus2","westus3","southafricanorth","eastasia","southeastasia","australiaeast","centralindia","japaneast","japanwest","koreacentral","canadacentral","northeurope","westeurope","francecentral","germanywestcentral","norwayeast","swedencentral8","switzerlandnorth","switzerlandwest","uksouth","uaenorth","brazilsouth","qatarcentral3,8","centralus","eastus","eastus2","northcentralus","southcentralus","westcentralus","westus","westus2","westus3")


    fun regionIsValid():Boolean{
        return regionList.contains(region)
    }
    fun subscriptionKeyIsValid():Boolean{
        return subscriptionKey.length == 32
    }
    fun getAccessToken(): String? {
        val fetchTokenUri = "https://${region}.api.cognitive.microsoft.com/sts/v1.0/issueToken"
        if(regionIsValid() && subscriptionKeyIsValid()){
            if (tokenExpiryTime == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
                runBlocking {
                    token = fetchTokenAsync(fetchTokenUri, subscriptionKey)
                    // If token is not null, set the expiry time to 9 minutes from now
                    if(token != null){
                        tokenExpiryTime = LocalDateTime.now().plusMinutes(9)
                    }
                }
            }
        }

        return token
    }

    private suspend fun fetchTokenAsync(fetchUri: String, subscriptionKey: String): String? {
        val client = HttpClient()
        return try {
            val response: HttpResponse = client.post(fetchUri) {
                headers {
                    append("Ocp-Apim-Subscription-Key", subscriptionKey)
                }
            }
            response.bodyAsText()
        }  catch (e: Exception) {
            // Handle any other exceptions
            println("An error occurred: ${e.message}")
            null
        }
    }

    // TODO 方法名不对，可能需要重构
    suspend fun textToSpeech( text: String):String? {
        val accessToken = getAccessToken()
        if(accessToken != null){
            val url = "https://$region.tts.speech.microsoft.com/cognitiveservices/v1"

            val ssml = """
                    <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$pronunciationStyle'>
                        <voice name='$shortName'>$text</voice>
                    </speak>
                  """.trimIndent()

            val client = HttpClient()
            val contentType = ContentType("application", "ssml+xml")

            return try{
                val response: HttpResponse = client.post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $accessToken")
                        append("X-Microsoft-OutputFormat", "audio-48khz-192kbitrate-mono-mp3")
                    }
                    setBody(ssml)
                }
                var path :String? = null
                if (response.status == HttpStatusCode.OK) {
                    withContext(Dispatchers.IO) {
                        val bytes = response.body<ByteArray>()
                        val file = File(getAudioDirectory(), text +"_Azure_${displayName}_${pronunciationStyle}.mp3")
                        Files.write(Paths.get(file.toURI()), bytes)
                        path =  file.absolutePath
                    }
                }
                 path
            }catch (e: Exception) {
                // Handle any other exceptions
                println("An error occurred: ${e.message}")
                null
            }

        }else {
            return null
        }

    }


    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getVoicesList(): List<Voice> {
        val voiceList = mutableListOf<Voice>()
        val client = HttpClient()
        val voiceListUri = "https://${region}.tts.speech.microsoft.com/cognitiveservices/voices/list"

        try{
            val response: HttpResponse = client.get(voiceListUri) {
                headers {
                    append("Ocp-Apim-Subscription-Key", subscriptionKey)
                }
            }
            if(response.status.value == 200){
                val format = Json { ignoreUnknownKeys = true }
                val list = format.decodeFromString<List<Voice>>(response.bodyAsText())
                for (voice in list) {
                    if(voice.Locale == pronunciationStyle){
                        voiceList.add(voice)
                    }
                }
            }
        }catch (e:Exception){
            println("GET request failed. Message: ${e.message}")
        }



        return voiceList
    }

    fun saveAzureState(){
        val azureSetting = getAzureSettingsFile()
        runBlocking {
            launch (Dispatchers.IO){
                val key = Crypt.encrypt(subscriptionKey)
                val azureData = AzureTTSData(
                    subscriptionKey = key,
                    region = region,
                    pronunciationStyle = pronunciationStyle,
                    shortName = shortName,
                    displayName = displayName,
                    gender = gender
                )
                val json = encodeFormat.encodeToString(azureData)
                azureSetting.writeText(json)
            }
        }

    }

}
val encodeFormat = Json {
    prettyPrint = true
    encodeDefaults = true
}
@Composable
fun rememberAzureTTS(): AzureTTS = remember{
    loadAzureState()
}

@OptIn(ExperimentalSerializationApi::class)
fun loadAzureState(): AzureTTS {
    val azureSetting = getAzureSettingsFile()
    return if(azureSetting.exists()){
        try{
            val decodeFormat = Json { ignoreUnknownKeys = true }
            var azureData= decodeFormat.decodeFromString<AzureTTSData>(azureSetting.readText())
            Crypt.decrypt(azureData.subscriptionKey).let {
                azureData = azureData.copy(subscriptionKey = it)
            }

            AzureTTS(azureData)
        }catch (exception:Exception){
            exception.printStackTrace()
            AzureTTS(AzureTTSData())
        }
    }else{
        println("Azure setting file not found")
        val azureData = AzureTTSData()
        val tts = AzureTTS(azureData)
        val json = encodeFormat.encodeToString(azureData)
        azureSetting.writeText(json)
        tts
    }
}

fun getAzureSettingsFile(): File {
    val settingsDir = getSettingsDirectory()
    return File(settingsDir, "AzureSpeechSettings.json")
}
