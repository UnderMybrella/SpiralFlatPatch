package info.spiralframework.flatpatch

import java.io.File

fun File.normaliseForDataFiles(): File = if (this.name.endsWith(".app") && this.isDirectory) File(this, "Contents") else this
fun File.normaliseForExecutable(): File = if (this.name.endsWith(".app") && this.isDirectory) File(this, "Contents") else this

fun String.normalisePath(): String = this.replace("/", File.separator).replace("\\", File.separator)