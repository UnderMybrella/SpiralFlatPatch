package info.spiralframework.flatpatch

import java.io.File

fun File.normaliseForDataFiles(): File {
    if (this.name.endsWith(".app") && this.isDirectory)
        return File(this, "Contents")
    else if (this.name.matches(GurrenFlatPatch.DRV3_STEAM_FOLDER_REGEX))
        return File(this, "data${File.separator}win").takeIf(File::exists)
            ?: File(this, "data${File.separator}win_demo")
    else
        return this
}

fun File.normaliseForExecutable(): File =
    if (this.name.endsWith(".app") && this.isDirectory) File(this, "Contents") else this

fun String.normalisePath(): String = this.replace("/", File.separator).replace("\\", File.separator)