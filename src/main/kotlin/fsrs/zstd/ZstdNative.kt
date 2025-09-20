package fsrs.zstd

class ZstdNative {
    external fun compress(input: ByteArray, compressionLevel: Int): ByteArray
    external fun decompress(input: ByteArray): ByteArray
    external fun compressStream(input: ByteArray, compressionLevel: Int): ByteArray
    external fun getVersion(): String
    
    companion object {
        init {
            var loaded = false
            try {
                System.loadLibrary("rust_zstd_jni")
                loaded = true
            } catch (e: Throwable) {
                // 尝试从工程内置路径加载
                val userDir = System.getProperty("user.dir") ?: "."
                val candidates = mutableListOf<String>()
                val base = "$userDir/rust-zstd-jni/target"
                val names = when (detectOs()) {
                    Os.Mac -> listOf("release/librust_zstd_jni.dylib", "debug/librust_zstd_jni.dylib")
                    Os.Linux -> listOf("release/librust_zstd_jni.so", "debug/librust_zstd_jni.so")
                    Os.Windows -> listOf("release/rust_zstd_jni.dll", "debug/rust_zstd_jni.dll")
                }
                names.forEach { candidates.add("$base/$it") }
                var lastError: Throwable = e
                for (p in candidates) {
                    try {
                        java.nio.file.Files.exists(java.nio.file.Paths.get(p)).also {
                            if (it) {
                                System.load(p)
                                loaded = true
                                break
                            }
                        }
                    } catch (t: Throwable) {
                        lastError = t
                    }
                }
                if (!loaded) throw lastError
            }
        }
        private enum class Os { Mac, Linux, Windows }
        private fun detectOs(): Os {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("mac") || os.contains("darwin") -> Os.Mac
                os.contains("win") -> Os.Windows
                else -> Os.Linux
            }
        }
    }
}