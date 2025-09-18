package fsrs.zstd

import java.io.OutputStream
import com.github.luben.zstd.Zstd

object ZstdHelper {
    
    fun compress(data: ByteArray, level: Int = 0): ByteArray {
        // Use Java zstd-jni library for compatibility
        return Zstd.compress(data, level)
    }
    
    fun decompress(data: ByteArray): ByteArray {
        return Zstd.decompress(data, 10 * 1024 * 1024) // 10MB max decompression size
    }
    
    fun getVersion(): String {
        return "1.5.6-10" // zstd-jni version used in build.gradle.kts
    }
    
    fun compressStream(outputStream: OutputStream, level: Int = 0): ZstdOutputStream {
        return ZstdOutputStream(outputStream, level)
    }
}