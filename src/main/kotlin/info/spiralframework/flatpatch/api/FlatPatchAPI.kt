package info.spiralframework.flatpatch.api

import info.spiralframework.base.util.SemVer
import java.util.*

interface FlatPatchAPI {
    companion object {
        val loader = ServiceLoader.load(FlatPatchAPI::class.java)
        val instance: FlatPatchAPI? by lazy { loader.firstOrNull() }
    }

    val version: SemVer
}