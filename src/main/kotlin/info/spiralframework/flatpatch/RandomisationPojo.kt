package info.spiralframework.flatpatch

interface RandomisationPojo {
    interface Immutable {
        val seed: Long

        fun encode(): String

        data class DEFAULT(override val seed: Long): Immutable {
            override fun encode(): String = seed.toString(16).padStart(16, '0')
        }
    }

    data class DEFAULT(override var seed: Long = 0L): RandomisationPojo {
        override fun makeImmutable(): Immutable = Immutable.DEFAULT(seed)
    }

    var seed: Long

    fun makeImmutable(): Immutable
}