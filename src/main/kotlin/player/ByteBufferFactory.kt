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

package player

import sun.misc.Unsafe
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Copied from vlcj  src/main/java/uk/co/caprica/vlcj/player/embedded/videosurface/ByteBufferFactory.java

/**
 * Factory for creating property aligned native byte buffers.
 *
 *
 * This class uses "unsafe" API which might be restricted/removed in future JDKs.
 *
 *
 * Original credit: http://psy-lob-saw.blogspot.co.uk/2013/01/direct-memory-alignment-in-java.html
 */
internal object ByteBufferFactory {
    private val addressOffset = getAddressOffset()

    /**
     * Alignment suitable for use by LibVLC video callbacks.
     */
    private const val LIBVLC_ALIGNMENT = 32

    /**
     * Allocate a properly aligned native byte buffer, suitable for use by the LibVLC video callbacks.
     *
     * @param capacity required size for the buffer
     * @return aligned byte buffer
     */
    fun allocateAlignedBuffer(capacity: Int): ByteBuffer {
        return allocateAlignedBuffer(capacity, LIBVLC_ALIGNMENT)
    }

    /**
     * Get the value of the native address field from the buffer.
     *
     * @param buffer buffer
     * @return native address pointer
     */
    fun getAddress(buffer: ByteBuffer?): Long {
        return UnsafeAccess.UNSAFE.getLong(buffer, addressOffset)
    }

    private fun getAddressOffset(): Long {
        try {
            return UnsafeAccess.UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("address"))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Allocate a property aligned native byte buffer.
     *
     * @param capacity required size for the buffer
     * @param alignment alignment alignment
     * @return aligned byte buffer
     */
    private fun allocateAlignedBuffer(capacity: Int, alignment: Int): ByteBuffer {
        val result: ByteBuffer
        val buffer = ByteBuffer.allocateDirect(capacity + alignment)
        val address = getAddress(buffer)
        if ((address and (alignment - 1).toLong()) == 0L) {
            // Stupid cast required see #829
            (buffer as Buffer).limit(capacity)
            result = buffer.slice().order(ByteOrder.nativeOrder())
        } else {
            val newPosition = (alignment - (address and (alignment - 1).toLong())).toInt()
            // Stupid casts required see #829
            (buffer as Buffer).position(newPosition)
            (buffer as Buffer).limit(newPosition + capacity)
            result = buffer.slice().order(ByteOrder.nativeOrder())
        }
        return result
    }

    private object UnsafeAccess {
        val UNSAFE: Unsafe

        init {
            try {
                val field = Unsafe::class.java.getDeclaredField("theUnsafe")
                field.setAccessible(true)
                UNSAFE = field.get(null) as Unsafe
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}