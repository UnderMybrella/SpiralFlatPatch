package info.spiralframework.flatpatch

import info.spiralframework.base.util.locale
import info.spiralframework.base.util.printLocale
import info.spiralframework.base.util.relativePathFrom
import info.spiralframework.console.CommandBuilders
import info.spiralframework.console.data.ParameterParser
import info.spiralframework.console.eventbus.CommandClass
import info.spiralframework.console.eventbus.ParboiledCommand
import info.spiralframework.console.eventbus.ParboiledCommand.Companion.SUCCESS
import info.spiralframework.console.eventbus.ParboiledCommand.Companion.fail
import info.spiralframework.formats.game.hpa.DR1
import info.spiralframework.formats.game.hpa.DR2
import info.spiralframework.formats.game.hpa.UDG
import info.spiralframework.formats.game.v3.V3
import org.parboiled.Action
import java.io.File
import java.nio.file.Files
import kotlin.random.Random

class GurrenRandomisation(override val parameterParser: ParameterParser) : CommandClass {
    companion object {
        val FILE_SEPARATOR_REGEX = "[/\\\\]"
        val LANG_REGEX = "(all|us|jp|ch)"
        val DR1_BUSTUP_SPRITES_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}cg${FILE_SEPARATOR_REGEX}bustup_\\d{2}_\\d{2}(_$LANG_REGEX)?\\.(tga)".toRegex()
        val DR1_STAND_TEXTURE_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}texture${FILE_SEPARATOR_REGEX}stand_\\d{2}_\\d{2}(_$LANG_REGEX)?\\.(tga)".toRegex()
        val DR1_BGM_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}all${FILE_SEPARATOR_REGEX}bgm${FILE_SEPARATOR_REGEX}dr1_bgm_hca\\.awb\\.\\d{5}\\.ogg".toRegex()
        val DR1_BACKGROUND_SPRITES_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}cg${FILE_SEPARATOR_REGEX}bgd_\\d{3}\\.(tga)".toRegex()
        val DR1_PRESENT_ICONS_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}cg${FILE_SEPARATOR_REGEX}present_icn_\\d{3}\\.(tga)".toRegex()
        val DR1_FLASH_EVENTS_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}flash${FILE_SEPARATOR_REGEX}fla_\\d{3}\\.pak".toRegex()
        val DR1_SOUND_EFFECTS_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}se${FILE_SEPARATOR_REGEX}se\\d_\\d{3}\\.acb\\.files${FILE_SEPARATOR_REGEX}HS_SE_\\d{3}\\.ogg".toRegex()
        val DR1_CUTINS_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}cg${FILE_SEPARATOR_REGEX}cutin_icn_\\d{3}\\.(tga)".toRegex()
        val DR1_EVIDENCE_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}cg${FILE_SEPARATOR_REGEX}kotodama-icn_\\d{3}\\.(tga)".toRegex()
        val DR1_MOVIE_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}movie${FILE_SEPARATOR_REGEX}movie_\\d{2}\\.(ivf)".toRegex()
        val DR1_VOICE_LINE_REGEX =
            "Dr1${FILE_SEPARATOR_REGEX}data${FILE_SEPARATOR_REGEX}${LANG_REGEX}${FILE_SEPARATOR_REGEX}voice${FILE_SEPARATOR_REGEX}dr1_voice_hca(_${LANG_REGEX})\\.awb\\.\\d{5}\\.ogg".toRegex()
    }

    val builders = CommandBuilders(parameterParser)

    val randomiseContentRule = makeRuleWith(::RandomiseContentArgs) { argsVar ->
        Sequence(
            Localised("commands.flatpatch.randomise.base"),
            Action<Any> { pushMarkerSuccessBase() },
            Optional(
                InlineWhitespace(),
                FirstOf(
                    Sequence(
                        Localised("commands.flatpatch.randomise.builder"),
                        Action<Any> { argsVar.get().builder = true; true }
                    ),
                    Sequence(
                        ExistingFilePath(),
                        Action<Any> { argsVar.get().workspacePath = pop() as? File; true }
                    )
                ),
                ZeroOrMore(
                    InlineWhitespace(),
                    FirstOf(
                        Sequence(
                            Localised("commands.flatpatch.randomise.game"),
                            InlineWhitespace(),
                            Parameter(),
                            Action<Any> {
                                val gameStr = pop() as String
                                when {
                                    DR1.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR1
                                    DR2.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR2
                                    V3.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = V3
                                    else -> return@Action pushMarkerFailedLocale(
                                        locale("commands.flatpatch.randomise.err_no_game_for_name", gameStr)
                                    )
                                }

                                return@Action true
                            }
                        ),
                        Sequence(
                            Localised("commands.flatpatch.randomise.presets"),
                            InlineWhitespace(),
                            Parameter(),
                            Action<Any> {
                                argsVar.get().let { args ->
                                    when (args.game) {
                                        DR1 -> argsVar.get().randomisation = DR1RandomisationPojo.decode(
                                            BitStateMachineReader(pop() as String)
                                        )
                                        else -> argsVar.get().randomisation =
                                            RandomisationPojo.DEFAULT((pop() as String).toLongOrNull(16) ?: 0L)
                                    }
                                }

                                return@Action true
                            }
                        ),
                        Sequence(
                            Action<Any> { argsVar.get().game == DR1 },
                            Localised("commands.flatpatch.randomise.dr1.base"),
                            InlineWhitespace(),
                            ZeroOrMore(
                                FirstOf(
                                    Sequence(
                                        Localised("commands.flatpatch.randomise.dr1.bust_sprites"),
                                        Action<Any> {
                                            (argsVar.get().randomisation as DR1RandomisationPojo).randomiseBustSprites =
                                                true; true
                                        }
                                    ),
                                    Sequence(
                                        Localised("commands.flatpatch.randomise.dr1.stand_textures"),
                                        Action<Any> {
                                            (argsVar.get().randomisation as DR1RandomisationPojo).randomiseStandTextures =
                                                true; true
                                        }
                                    ),
                                    Sequence(
                                        Localised("commands.flatpatch.randomise.dr1.bgm"),
                                        Action<Any> {
                                            (argsVar.get().randomisation as DR1RandomisationPojo).randomiseBGM =
                                                true; true
                                        }
                                    ),
                                    Sequence(
                                        Localised("commands.flatpatch.randomise.dr1.background_sprites"),
                                        Action<Any> {
                                            (argsVar.get().randomisation as DR1RandomisationPojo).randomiseBackgroundSprites =
                                                true; true
                                        }
                                    ),
                                    Sequence(
                                        Localised("commands.flatpatch.randomise.dr1.present_icons"),
                                        Action<Any> {
                                            (argsVar.get().randomisation as DR1RandomisationPojo).randomisePresentIcons =
                                                true; true
                                        }
                                    )
                                ),
                                OptionalInlineWhitespace(),
                                Optional(","),
                                OptionalInlineWhitespace()
                            )
                        ),
                        Sequence(
                            Localised("commands.flatpatch.randomise.filter"),
                            InlineWhitespace(),
                            Filter(),
                            Action<Any> { argsVar.get().filter = pop() as? Regex; true }
                        ),
                        Sequence(
                            Localised("commands.flatpatch.randomise.builder"),
                            Action<Any> { argsVar.get().builder = true; true }
                        )
                    )
                ),
                Action<Any> { pushMarkerSuccessCommand() }
            )
        )
    }

    val randomiseContent = ParboiledCommand(randomiseContentRule) { stack ->
        val builder = stack[0] as RandomiseContentArgs

        //Step 1. Check all our data's there

        builder.workspacePath = builder.workspacePath?.normaliseForContentFile()
        if (builder.workspacePath == null || builder.game == null || builder.builder) {
            if (builder.workspacePath == null) {
                printLocale("commands.flatpatch.randomise.builder.workspace")
                builder.workspacePath = builders.filePath()?.normaliseForContentFile()
            }

            if (builder.game == null) {
                builder.workspacePath?.takeIf(File::isDirectory)?.let { path ->
                    builder.game = when {
                        path.name.matches(GurrenFlatPatch.DR1_STEAM_FOLDER_REGEX) -> DR1
                        path.name.matches(GurrenFlatPatch.DR2_STEAM_FOLDER_REGEX) -> DR2
                        path.name.matches(GurrenFlatPatch.DRV3_STEAM_FOLDER_REGEX) -> V3
                        else -> null
                    }
                }
            }

            if (builder.game == null) {
                printLocale("commands.flatpatch.randomise.builder.game")
                builder.game = builders.parameter()?.let { gameStr ->
                    if (DR1.names.any { str -> str.equals(gameStr, true) })
                        return@let DR1
                    if (DR2.names.any { str -> str.equals(gameStr, true) })
                        return@let DR2
                    if (V3.names.any { str -> str.equals(gameStr, true) })
                        return@let V3
                    return@ParboiledCommand fail(
                        "commands.flatpatch.randomise.builder.err_no_game_for_name",
                        gameStr
                    )
                } ?: return@ParboiledCommand fail(
                    "commands.flatpatch.randomise.builder.err_no_game_for_name",
                    ""
                )
            }

            if (builder.filter == null) {
                printLocale("commands.flatpatch.randomise.builder.filter")
                builder.filter = builders.filter()
            }
        }

        builder.workspacePath?.takeIf(File::isDirectory)?.let { workspacePath ->
            builder.game?.let { game ->
                val regex = when (game) {
                    DR1 -> GurrenFlatPatch.DR1_CONTENT_FOLDER_REGEX
                    DR2 -> GurrenFlatPatch.DR2_CONTENT_FOLDER_REGEX
                    UDG -> GurrenFlatPatch.UDG_CONTENT_FOLDER_REGEX
                    V3 -> GurrenFlatPatch.DRV3_CONTENT_FOLDER_REGEX
                    else -> TODO("Make regex for $game content folder")
                }

                builder.workspacePath = workspacePath.listFiles()
                    .firstOrNull { file -> file.name.matches(regex) }
                    ?: return@ParboiledCommand fail("commands.flatpatch.randomise.err_no_content_child")

                builder.baseGamePath = File(workspacePath, "base_game")
            }
        }

        val args = builder.makeImmutable(defaultFilter = ".*".toRegex())

        if (args.workspacePath == null)
            return@ParboiledCommand fail("commands.flatpatch.randomise.err_no_workspace")

        if (!args.workspacePath.exists())
            return@ParboiledCommand fail("commands.flatpatch.randomise.err_workspace_doesnt_exist")

        if (!args.workspacePath.isDirectory)
            return@ParboiledCommand fail("commands.flatpatch.randomise.err_workspace_not_directory")

        if (args.game == null)
            return@ParboiledCommand fail("commands.flatpatch.randomise.err_no_game")

        return@ParboiledCommand when (args.game) {
            DR1 -> randomiseDR1(args)
            else -> TODO("Support ${args.game}")
        }
    }

    fun randomiseDR1(args: RandomiseContentArgs.Immutable): Boolean {
        val random = Random(args.randomisation.seed)
        val randomisation = args.randomisation as DR1RandomisationPojo.Immutable

        val baseGameFolder = args.baseGamePath!!
        val contentFolder = args.workspacePath!!
        val randomisedFolder = File(baseGameFolder.parentFile, randomisation.encode())
        randomisedFolder.mkdir()

        val contents = contentFolder.walk().toList().map { file -> (file relativePathFrom contentFolder) }
            .sorted()
            .filter(args.filter!!::matches)


        if (randomisation.randomiseBustSprites) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_BUSTUP_SPRITES_REGEX
            )
        }

        if (randomisation.randomiseStandTextures) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_STAND_TEXTURE_REGEX
            )
        }

        if (randomisation.randomiseBGM) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_BGM_REGEX,
                "ogg" to "loop"
            )
        }

        if (randomisation.randomiseBackgroundSprites) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_BACKGROUND_SPRITES_REGEX
            )
        }

        if (randomisation.randomisePresentIcons) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_PRESENT_ICONS_REGEX
            )
        }

        if (randomisation.randomiseFlashEvents) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_FLASH_EVENTS_REGEX
            )
        }

        if (randomisation.randomiseRoomComponents) {
            //Later
        }

        if (randomisation.randomiseSoundEffects) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_SOUND_EFFECTS_REGEX
            )
        }

        if (randomisation.randomiseCutIns) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_CUTINS_REGEX
            )
        }

        if (randomisation.randomiseEvidenceIcons) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_EVIDENCE_REGEX
            )
        }

        if (randomisation.randomiseMovies) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_MOVIE_REGEX,
                "ivf" to "ogg"
            )
        }

        if (randomisation.randomiseVoiceLines) {
            randomiseComponentDR1(
                contents,
                contentFolder,
                randomisedFolder,
                random,
                randomisation,
                DR1_VOICE_LINE_REGEX
            )
        }

        randomisedFolder.walk()
            .filter { file ->
                file.isFile && !(file.name.startsWith(".") || file.name.startsWith("_") || file.parentFile.name.startsWith(
                    "."
                ) || file.parentFile.name.startsWith(
                    "_"
                ))
            }
            .map { file -> file relativePathFrom randomisedFolder }
            .forEach { str ->
                val source = File(randomisedFolder, str)
                val dest = File(contentFolder, str)

                if (dest.exists())
                    dest.delete()

                if (!source.exists())
                    FlatPatchPlugin.LOGGER.warn("commands.flatpatch.link_content.warn_missing_source", source)

                Files.createLink(dest.toPath(), source.toPath())
            }

        return SUCCESS
    }

    fun randomiseComponentDR1(
        contents: List<String>,
        contentFolder: File,
        randomFolder: File,
        random: Random,
        args: DR1RandomisationPojo.Immutable,
        regex: Regex,
        vararg replacements: Pair<String, String>
    ) {
        val files = contents.filter { path -> path.matches(regex) }.toMutableList()

        files.toTypedArray().forEach { path ->
            val sourcePath = files.random(random)
            val source = File(contentFolder, sourcePath)
            val dest = File(randomFolder, path)
            dest.parentFile.mkdirs()

            if (dest.exists())
                dest.delete()

            Files.createLink(dest.toPath(), source.toPath())

            replacements.forEach { (start, end) ->
                val sourceReplace = File(contentFolder, sourcePath.replace(start, end))
                val destReplace = File(randomFolder, path.replace(start, end))

                if (sourceReplace.exists()) {
                    if (destReplace.exists())
                        destReplace.delete()

                    Files.createLink(destReplace.toPath(), sourceReplace.toPath())
                }
            }

            val eeRate = random.nextFloat()
            FlatPatchPlugin.LOGGER.trace(
                "commands.flatpatch.randomise.randomising",
                dest,
                source,
                eeRate
            )

            if (eeRate < args.equivalentExchangeRate)
                files.remove(sourcePath)
        }
    }
}