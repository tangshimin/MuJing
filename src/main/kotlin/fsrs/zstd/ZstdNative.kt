package fsrs.zstd

class ZstdNative {
    external fun compress(input: ByteArray, compressionLevel: Int): ByteArray
    external fun decompress(input: ByteArray): ByteArray
    external fun compressStream(input: ByteArray, compressionLevel: Int): ByteArray
    external fun getVersion(): String
    
    companion object {
        init {
            System.loadLibrary("rust_zstd_jni")
        }
    }
}