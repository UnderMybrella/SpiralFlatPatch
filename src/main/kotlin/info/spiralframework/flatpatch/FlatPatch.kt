package info.spiralframework.flatpatch

import info.spiralframework.base.util.SemVer
import info.spiralframework.flatpatch.api.FlatPatchAPI

class FlatPatch: FlatPatchAPI {
    override val version: SemVer = FlatPatchPlugin.version
}