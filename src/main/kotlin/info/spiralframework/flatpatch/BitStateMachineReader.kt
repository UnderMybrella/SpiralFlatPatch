package info.spiralframework.flatpatch

import java.io.ByteArrayInputStream

open class BitStateMachineReader(str: String) {
    protected val stream = ByteArrayInputStream(str.chunked(2).map { str -> str.toInt(16).toByte() }.toByteArray())
    var currentInt = stream.read()
    var currentPos = 0

    fun readBoolean(): Boolean = decodeData { bit() == 1 }
    fun readByte(): Byte = readNumber(8).toByte()
    fun readShort(): Short = readNumber(16).toShort()
    fun readInt(): Int = readNumber(32).toInt()
    fun readLong(): Long = readNumber(64)
    fun readFloat(): Float = Float.fromBits(readInt())

    fun readNumber(bits: Int): Long {
        var result = 0L

        //Read first x bits
        val availableBits = 8 - currentPos
        checkBefore(availableBits)
        for (i in 0 until availableBits)
            result = result or (bit().toLong() shl i)

        var offset = availableBits
        for (i in 0 until (bits / 8) - 1) {
            result = result or (stream.read().toLong() shl offset)
            offset += 8
        }

        checkAfter() //This goes after the read calls so that we ensure we don't skip a byte

        //Read last x bits
        checkBefore(8 - availableBits)
        for (i in offset until bits)
            result = result or (bit().toLong() shl i)
        checkAfter()

        return result
    }

    open fun <T> decodeData(needed: Int = 1, op: () -> T): T {
        checkBefore(needed)
        try {
            return op()
        } finally {
            checkAfter()
        }
    }

    fun bit(): Int = ((currentInt and 0xFF) shr currentPos++) and 1

    protected fun checkBefore(needed: Int = 1) {
        if (currentPos > (8 - needed)) { //hardcoded check just to make sure we don't write a 0 byte
            currentInt = stream.read()
            currentPos = 0
        }
    }
    protected fun checkAfter() {
        if (currentPos >= 8) {
            currentInt = stream.read()
            currentPos = 0
        }
    }
}