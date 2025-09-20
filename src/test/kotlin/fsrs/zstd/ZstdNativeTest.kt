package fsrs.zstd

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZstdNativeTest {
    @Test
    fun compress_decompress_roundtrip_and_magic() {
        val native = ZstdNative()
        val version = native.getVersion()
        println("zstd version: $version")

        val input = ByteArray(64 * 1024) { (it % 251).toByte() }
        val compressed = native.compress(input, 0)
        assertTrue(compressed.size >= 5, "compressed too short")
        // magic: 28 B5 2F FD
        assertTrue(compressed[0] == 0x28.toByte())
        assertTrue(compressed[1] == 0xB5.toByte())
        assertTrue(compressed[2] == 0x2F.toByte())
        assertTrue(compressed[3] == 0xFD.toByte())
        val fd = compressed[4].toInt() and 0xFF
        assertTrue(fd == 0x24 || fd == 0x04, "unexpected frame descriptor: 0x${fd.toString(16)}")

        val decompressed = native.decompress(compressed)
        assertArrayEquals(input, decompressed)
    }
}
