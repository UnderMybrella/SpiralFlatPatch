package info.spiralframework.flatpatch

open class BitStateMachineWriter {
    private var currentInt = 0
    private var currentPos = 0
    private val builder = StringBuilder()

    fun Int.toBitHex(): String = toString(16).padStart(2, '0')

    open infix fun write(bool: Boolean) = encodeData { bit(if (bool) 1 else 0) }
    open infix fun writeByte(byte: Number) = byte(byte)
    open infix fun writeShort(short: Number) = short(short)
    open infix fun writeInt(int: Number) = int(int)
    open infix fun writeLong(long: Number) = long(long)
    open infix fun writeFloat(float: Float) = int(float.toBits())

    open fun <T> encodeData(needed: Int = 1, op: () -> T): T {
        checkBefore(needed)
        try {
            return op()
        } finally {
            checkAfter()
        }
    }

    open fun bit(bit: Int) {
        currentInt = currentInt or (bit shl currentPos++)
    }
    open fun byte(num: Number) = number(num.toLong(), 8)
    open fun short(num: Number) = number(num.toLong(), 16)
    open fun int(num: Number) = number(num.toLong(), 32)
    open fun long(num: Number) = number(num.toLong(), 64)

    open fun number(num: Long, bits: Int) {
        val availableBits = 8 - currentPos
        checkBefore(availableBits)
        for (i in 0 until availableBits)
            bit(((num shr i) and 1).toInt())
        checkAfter()
        var offset = availableBits
        for (i in 0 until (bits / 8) - 1) {
            encode(((num shr offset) and 0xFF).toInt())
            offset += 8
        }
        checkBefore(8 - availableBits)
        for (i in offset until bits)
            bit(((num shr i) and 1).toInt())
        checkAfter()
    }

    protected fun checkBefore(needed: Int = 1) {
        if (currentPos > (8 - needed) && currentPos > 0) { //hardcoded check just to make sure we don't write a 0 byte
            encodeByte()
            currentInt = 0
            currentPos = 0
        }
    }
    protected fun checkAfter() {
        if (currentPos >= 8) {
            encodeByte()
            currentInt = 0
            currentPos = 0
        }
    }
    protected fun encodeByte() {
        builder.append(currentInt.toBitHex())
    }
    protected fun encode(byte: Int) {
        builder.append(byte.toBitHex())
    }

    open fun build(): String {
        if (currentPos > 0)
            encodeByte()

        try {
            return builder.toString()
        } finally {
            currentInt = 0
            currentPos = 0
            builder.clear()
        }
    }
}