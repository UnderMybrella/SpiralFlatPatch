package info.spiralframework.flatpatch

import info.spiralframework.formats.game.DRGame
import java.io.File

class ExtractWorkspaceArgs {
//    data class Immutable(val extractPath: File?, val filter: Regex?, val destDir: File?, val leaveCompressed: Boolean?)
//    var extractPath: File? = null
//    var filter: Regex? = null
//    var destDir: File? = null
//    var leaveCompressed: Boolean? = null
//    var builder: Boolean = false
//
//    fun makeImmutable(
//        defaultExtractPath: File? = null,
//        defaultFilter: Regex? = null,
//        defaultDestDir: File? = null,
//        defaultLeaveCompressed: Boolean? = null
//    ): ExtractArgs.Immutable =
//        Immutable(
//            extractPath ?: defaultExtractPath,
//            filter ?: defaultFilter,
//            destDir ?: defaultDestDir,
//            leaveCompressed ?: defaultLeaveCompressed
//        )
    data class Immutable(val workplacePath: File?, val game: DRGame?)

    var workplacePath: File? = null
    var game: DRGame? = null
    var builder: Boolean = false

    fun makeImmutable(
        defaultWorkplacePath: File? = null,
        defaultGame: DRGame? = null
    ): Immutable =
            Immutable(
                workplacePath ?: defaultWorkplacePath,
                game ?: defaultGame
            )
}