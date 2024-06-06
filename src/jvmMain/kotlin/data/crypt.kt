
package data

import com.sun.jna.platform.win32.Crypt32Util
import player.isMacOS
import player.isWindows
import java.nio.charset.StandardCharsets
import java.util.*

class Crypt{
    companion object{
        fun encrypt(data: String): String {
            if(data.isEmpty()) return data
            return if(isWindows()) {
                DPAPI.encrypt(data)
            }else if(isMacOS()){
                Keychain.addItem(data)
            }else{
                Keyring.addItem(data)
            }
        }

        fun decrypt(data: String): String {
            if (data.isEmpty()) return ""
            return if (data.length == 32) {
                data
            } else if (data.length > 32) {
                DPAPI.decrypt(data)
            } else if (data == "Keychain") {
                Keychain.getItem()
            } else if (data == "Keyring") {
                Keyring.getItem()
            } else data
        }
    }

}
object DPAPI {
    fun encrypt(data: String): String {
        return try {
            val encryptedData = Crypt32Util.cryptProtectData(data.toByteArray(StandardCharsets.UTF_8),"MuJing".toByteArray(),0,"",null)
            Base64.getEncoder().encodeToString(encryptedData)
        } catch (e: Exception) {
            println("Error encrypting data: ${e.message}")
            return data
        }
    }

    fun decrypt(data: String): String {
        return try {
            val decryptedData = Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(data),"MuJing".toByteArray(),0,null)
            String(decryptedData, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            println("Error decrypting data: ${e.message}")
            ""
        }
    }
}


object Keychain{
    fun addItem(password:String):String{
        val username = System.getProperty("user.name")
        val command = listOf(
            "security", "add-generic-password",
            "-a", username,
            "-w", password,
            "-s", "MuJing Service"
        )

        try {
            val process = ProcessBuilder(command).start()
            process.waitFor()
        } catch (e: Exception) {
            println("Error adding item to Keychain: ${e.message}")
            return password
        }
        return "Keychain"
    }
    fun getItem(): String {
        val username = System.getProperty("user.name")
        val command = listOf(
            "security", "find-generic-password",
            "-a", username,
            "-s", "MuJing Service",
            "-w"
        )

        return try {
            val process = ProcessBuilder(command).start()
            val reader = process.inputStream.bufferedReader()
            val password = reader.readLine()
            process.waitFor()
            password
        } catch (e: Exception) {
            println("Error retrieving item from Keychain: ${e.message}")
            ""
        }
    }

}


object Keyring{
    fun addItem(password: String):String {
        val command = listOf(
            "/bin/sh", "-c",
            "echo -n $password | secret-tool store --label='MuJing Password' MuJing-Service AzureKey"
        )

        try {
            val process = ProcessBuilder(command).start()
            process.waitFor()
        } catch (e: Exception) {
            println("Error adding item to Keyring: ${e.message}")
            return password
        }
        return "Keyring"
    }

    fun getItem(): String {
        val command = listOf(
            "secret-tool", "lookup",
            "service:", "MuJing AzureKey Service"
        )

        return try {
            val process = ProcessBuilder(command).start()
            val reader = process.inputStream.bufferedReader()
            val password = reader.readLine()
            process.waitFor()
            password
        } catch (e: Exception) {
            println("Error retrieving item from Keyring: ${e.message}")
            ""
        }
    }
}