package info.spiralframework.flatpatch

import kotlin.random.Random

data class DR1RandomisationPojo(
    override var seed: Long,
    var randomiseBustSprites: Boolean = false,
    var randomiseStandTextures: Boolean = false,
    var randomiseBGM: Boolean = false,
    var randomiseBackgroundSprites: Boolean = false,
    var randomisePresentIcons: Boolean = false,
    var randomiseFlashEvents: Boolean = false,
    var randomiseRoomComponents: Boolean = false,
    var randomiseSoundEffects: Boolean = false,
    var randomiseCutIns: Boolean = false,
    var randomiseEvidenceIcons: Boolean = false,
    var randomiseMovies: Boolean = false,
    var randomiseVoiceLines: Boolean = false,

    var deepRandomiseTextures: Boolean = false,

    var textureAnarchy: Boolean = false,
    var audioAnarchy: Boolean = false,
    var modelAnarchy: Boolean = false,

    var equivalentExchangeRate: Float = 1.0f
): RandomisationPojo {
    companion object {
        fun decode(state: BitStateMachineReader): DR1RandomisationPojo =
            DR1RandomisationPojo(
                state.readLong().takeIf { seed -> seed != -1L } ?: Random.nextLong(),

                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),

                state.readBoolean(),

                state.readBoolean(),
                state.readBoolean(),
                state.readBoolean(),

                state.readFloat()
            )
    }

    data class Immutable(
        override val seed: Long,
        val randomiseBustSprites: Boolean,
        val randomiseStandTextures: Boolean,
        val randomiseBGM: Boolean,
        val randomiseBackgroundSprites: Boolean,
        val randomisePresentIcons: Boolean,
        val randomiseFlashEvents: Boolean,
        val randomiseRoomComponents: Boolean,
        val randomiseSoundEffects: Boolean,
        val randomiseCutIns: Boolean,
        val randomiseEvidenceIcons: Boolean,
        val randomiseMovies: Boolean,
        val randomiseVoiceLines: Boolean,

        val deepRandomiseTextures: Boolean,

        val textureAnarchy: Boolean,
        val audioAnarchy: Boolean,
        val modelAnarchy: Boolean,

        val equivalentExchangeRate: Float
    ): RandomisationPojo.Immutable {
        override fun encode(): String {
            val state = BitStateMachineWriter()

            state.writeLong(seed)

            state.write(randomiseBustSprites)
            state.write(randomiseStandTextures)
            state.write(randomiseBGM)
            state.write(randomiseBackgroundSprites)
            state.write(randomisePresentIcons)
            state.write(randomiseFlashEvents)
            state.write(randomiseRoomComponents)
            state.write(randomiseSoundEffects)
            state.write(randomiseCutIns)
            state.write(randomiseEvidenceIcons)
            state.write(randomiseMovies)
            state.write(randomiseVoiceLines)

            state.write(deepRandomiseTextures)

            state.write(textureAnarchy)
            state.write(audioAnarchy)
            state.write(modelAnarchy)

            state.writeFloat(equivalentExchangeRate)

            return state.build()
        }
    }

    override fun makeImmutable(): Immutable = Immutable(
        seed,
        randomiseBustSprites,
        randomiseStandTextures,
        randomiseBGM,
        randomiseBackgroundSprites,
        randomisePresentIcons,
        randomiseFlashEvents,
        randomiseRoomComponents,
        randomiseSoundEffects,
        randomiseCutIns,
        randomiseEvidenceIcons,
        randomiseMovies,
        randomiseVoiceLines,
        deepRandomiseTextures,
        textureAnarchy,
        audioAnarchy,
        modelAnarchy,
        equivalentExchangeRate
    )
}