package info.spiralframework.flatpatch

import info.spiralframework.base.SpiralLocale
import info.spiralframework.console.Cockpit
import info.spiralframework.console.data.ParameterParser
import info.spiralframework.console.eventbus.ParboiledCommand
import info.spiralframework.console.registerCommandClassViaRequest
import info.spiralframework.console.unregisterCommandClassViaRequest
import info.spiralframework.core.SpiralCoreData
import info.spiralframework.core.plugins.BaseSpiralPlugin
import info.spiralframework.formats.customCPK
import org.greenrobot.eventbus.EventBus
import org.slf4j.Logger
import java.io.FileOutputStream

object FlatPatchPlugin: BaseSpiralPlugin(FlatPatchPlugin::class.java, "spiralframework_flatpatch_plugin.yaml") {
    val LOGGER: Logger
        get() = SpiralCoreData.LOGGER //TODO: Get our own logger

    @JvmStatic
    fun main(args: Array<String>) {
        val empty = customCPK {
            add("[empty]", 1024, ByteArray(1024)::inputStream)
        }

        FileOutputStream("empty.cpk").use(empty::compile)

        val cockpit = Cockpit(args)
        load()
        cockpit.start()
    }

    val thisReallySucksButWhatever = run {
        //TODO: This isn't great, should be in load
        SpiralLocale.addBundle("SpiralFlatPatch")
    }

    val parameterParser = ParameterParser()
    val gurrenFlatPatch = GurrenFlatPatch(parameterParser)

    override fun load() {
        EventBus.getDefault().registerCommandClassViaRequest<ParboiledCommand>(gurrenFlatPatch)
    }

    override fun unload() {
        EventBus.getDefault().unregisterCommandClassViaRequest<ParboiledCommand>(gurrenFlatPatch)
    }
}