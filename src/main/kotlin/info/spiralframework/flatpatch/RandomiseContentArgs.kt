package info.spiralframework.flatpatch

import info.spiralframework.formats.game.DRGame
import java.io.File

class RandomiseContentArgs {
    data class Immutable(val workspacePath: File?, val baseGamePath: File?, val game: DRGame?, val filter: Regex?)

    var workspacePath: File? = null
    var baseGamePath: File? = null
    var game: DRGame? = null
    var filter: Regex? = null
    var builder: Boolean = false

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
            filter ?: defaultFilter
        )
}