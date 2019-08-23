package info.spiralframework.flatpatch

import info.spiralframework.formats.game.DRGame
import info.spiralframework.formats.game.hpa.DR1
import java.io.File
import kotlin.properties.Delegates
import kotlin.random.Random

class RandomiseContentArgs {
    data class Immutable(val workspacePath: File?, val baseGamePath: File?, val game: DRGame?, val filter: Regex?, val randomisation: RandomisationPojo.Immutable)

    var workspacePath: File? = null
    var baseGamePath: File? = null
    var game: DRGame? by Delegates.observable<DRGame?>(null) { _, old, new ->
        if (old != new) {
            randomisation = when (new) {
                DR1 -> DR1RandomisationPojo(randomisation.seed)
                else -> RandomisationPojo.DEFAULT(randomisation.seed)
            }
        }
    }
    var filter: Regex? = null
    var builder: Boolean = false
    var randomisation: RandomisationPojo = RandomisationPojo.DEFAULT(Random.nextLong())

    fun makeImmutable(
        defaultWorkspacePath: File? = null,
        defaultBaseGamePath: File? = null,
        defaultGame: DRGame? = null,
        defaultFilter: Regex? = null
    ): Immutable =
        Immutable(
            workspacePath ?: defaultWorkspacePath,
            baseGamePath ?: defaultBaseGamePath,
            game ?: defaultGame,
            filter ?: defaultFilter,
            randomisation.makeImmutable()
        )
}