package fsrs.zstd

import java.io.IOException
import java.io.OutputStream

class ZstdOutputStream(
    private val outputStream: OutputStream,
    private val compressionLevel: Int = 0
) : OutputStream() {
    
    private val buffer = mutableListOf<ByteArray>()
    private var closed = false
    
    override fun write(b: Int) {
        if (closed) throw IOException("Stream closed")
        buffer.add(byteArrayOf(b.toByte()))
    }
    
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (closed) throw IOException("Stream closed")
        buffer.add(b.copyOfRange(off, off + len))
    }
    
    override fun flush() {
        // No-op for now
    }
    
    override fun close() {
        if (closed) return
        closed = true
        
        val allData = buffer.flatMap { it.toList() }.toByteArray()
        val compressed = ZstdHelper.compress(allData, compressionLevel)
        outputStream.write(compressed)
        outputStream.close()
    }
}