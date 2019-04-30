package info.spiralframework.flatpatch

data class SpiralDR1ExecutableMap(
    val archiveLocations: Map<Long, String>,
    val sfxFormatLocation: Pair<Long, String>?
)