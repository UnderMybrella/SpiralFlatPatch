package info.spiralframework.flatpatch

import com.fasterxml.jackson.module.kotlin.readValue
import info.spiralframework.base.util.*
import info.spiralframework.console.CommandBuilders
import info.spiralframework.console.data.ParameterParser
import info.spiralframework.console.eventbus.CommandClass
import info.spiralframework.console.eventbus.ParboiledCommand
import info.spiralframework.console.eventbus.ParboiledCommand.Companion.SUCCESS
import info.spiralframework.console.eventbus.ParboiledCommand.Companion.fail
import info.spiralframework.core.SpiralSerialisation
import info.spiralframework.formats.archives.WAD
import info.spiralframework.formats.archives.WADFileEntry
import info.spiralframework.formats.customWAD
import info.spiralframework.formats.game.hpa.DR1
import info.spiralframework.formats.game.hpa.DR2
import info.spiralframework.formats.game.v3.V3
import info.spiralframework.formats.utils.readZeroString
import org.parboiled.Action
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.measureTimeMillis

class GurrenFlatPatch(override val parameterParser: ParameterParser) : CommandClass {
    companion object {
        val DR1_WAD_REGEX = "dr1_data(_keyboard)?(_[a-z]{2})?\\.wad".toRegex()
        val DR1_WAD_LANG_REGEX = "dr1_data(_keyboard)?(_[a-z]{2})?\\.wad".toRegex()
        val DR1_WAD_KB_REGEX = "dr1_data_keyboard(_[a-z]{2})?\\.wad".toRegex()
        val DR1_FILE_SE_REGEX =
            "Dr(\\d|Common)/data/(all|[a-z]{2})/se/se\\d_\\d{3}\\.acb\\.files/HS_SE_\\d{3}\\.wav".toRegex()

        val DR1_EXECUTABLE_REGEX = "DR1_(us)\\.(exe)".toRegex()
        val DR1_STEAM_FOLDER_REGEX = Regex.fromLiteral("Danganronpa Trigger Happy Havoc")

        val DR2_WAD_REGEX = "dr2_data(_keyboard)?(_[a-z]{2})?\\.wad".toRegex()
        val DR2_WAD_LANG_REGEX = "dr2_data(_keyboard)?(_[a-z]{2})?\\.wad".toRegex()
        val DR2_WAD_KB_REGEX = "dr2_data_keyboard(_[a-z]{2})?\\.wad".toRegex()
        val DR2_FILE_SE_REGEX =
            "Dr(\\d|Common)/data/(all|[a-z]{2})/se/SE\\d_\\d\\.acb\\.files/DR2_SE_\\d{3}\\.wav".toRegex()

        val DR2_EXECUTABLE_REGEX = "DR2_(us)\\.(exe)".toRegex()
        val DR2_STEAM_FOLDER_REGEX = Regex.fromLiteral("Danganronpa 2 Goodbye Despair")

        val DRv3_CPK_REGEX = "partition_(data|resident)_(win)(_[a-z]{2})?\\.cpk".toRegex()
    }

    val builders = CommandBuilders(parameterParser)

    val prepareWorkspaceRule = makeRuleWith(::ExtractWorkspaceArgs) { argsVar ->
        Sequence(
            Localised("commands.flatpatch.prepare_workspace.base"),
            Action<Any> { pushMarkerSuccessBase() },
            Optional(
                InlineWhitespace(),
                FirstOf(
                    Sequence(
                        Localised("commands.flatpatch.prepare_workspace.builder"),
                        Action<Any> { argsVar.get().builder = true; true }
                    ),
                    Sequence(
                        ExistingFilePath(),
                        Action<Any> { argsVar.get().workplacePath = pop() as? File; true }
                    )
                ),
                ZeroOrMore(
                    InlineWhitespace(),
                    Sequence(
                        Localised("commands.flatpatch.prepare_workspace.game"),
                        InlineWhitespace(),
                        Parameter(),
                        Action<Any> {
                            val gameStr = pop() as String
                            when {
                                DR1.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR1
                                DR2.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR2
                                V3.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = V3
                                else -> return@Action pushMarkerFailedLocale(
                                    locale("commands.flatpatch.prepare_workspace.err_no_game_for_name", gameStr)
                                )
                            }

                            return@Action true
                        }
                    )
                ),
                Action<Any> { pushMarkerSuccessCommand() }
            )
        )
    }

    val patchExecutableRule = makeRuleWith(::PatchExecutableArgs) { argsVar ->
        Sequence(
            Localised("commands.flatpatch.patch_executable.base"),
            Action<Any> { pushMarkerSuccessBase() },
            Optional(
                InlineWhitespace(),
                FirstOf(
                    Sequence(
                        Localised("commands.flatpatch.patch_executable.builder"),
                        Action<Any> { argsVar.get().builder = true; true }
                    ),
                    Sequence(
                        ExistingFilePath(),
                        Action<Any> { argsVar.get().executablePath = pop() as? File; true }
                    )
                ),
                ZeroOrMore(
                    InlineWhitespace(),
                    Sequence(
                        Localised("commands.flatpatch.patch_executable.game"),
                        InlineWhitespace(),
                        Parameter(),
                        Action<Any> {
                            val gameStr = pop() as String
                            when {
                                DR1.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR1
                                DR2.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = DR2
                                V3.names.any { str -> str.equals(gameStr, true) } -> argsVar.get().game = V3
                                else -> return@Action pushMarkerFailedLocale(
                                    locale("commands.flatpatch.patch_executable.err_no_game_for_name", gameStr)
                                )
                            }

                            return@Action true
                        }
                    )
                ),
                Action<Any> { pushMarkerSuccessCommand() }
            )
        )
    }

    val prepareWorkspace = ParboiledCommand(prepareWorkspaceRule) { stack ->
        val workspaceBuilder = stack[0] as ExtractWorkspaceArgs

        //Step 1. Check all our data's there

        if (workspaceBuilder.workplacePath == null || workspaceBuilder.game == null || workspaceBuilder.builder) {
            if (workspaceBuilder.workplacePath == null) {
                printLocale("commands.flatpatch.prepare_workspace.builder.workspace")
                workspaceBuilder.workplacePath = builders.filePath()
            }

            if (workspaceBuilder.game == null) {
                printLocale("commands.flatpatch.prepare_workspace.builder.game")
                workspaceBuilder.game = builders.parameter()?.let { gameStr ->
                    if (DR1.names.any { str -> str.equals(gameStr, true) })
                        return@let DR1
                    if (DR2.names.any { str -> str.equals(gameStr, true) })
                        return@let DR2
                    if (V3.names.any { str -> str.equals(gameStr, true) })
                        return@let V3
                    return@ParboiledCommand fail(
                        "commands.flatpatch.prepare_workspace.builder.err_no_game_for_name",
                        gameStr
                    )
                } ?: return@ParboiledCommand fail(
                    "commands.flatpatch.prepare_workspace.builder.err_no_game_for_name",
                    ""
                )
            }
        }

        workspaceBuilder.workplacePath = workspaceBuilder.workplacePath?.normaliseForDataFiles()
        val args = workspaceBuilder.makeImmutable()

        if (args.workplacePath == null)
            return@ParboiledCommand fail("commands.flatpatch.prepare_workspace.err_no_workspace")

        if (!args.workplacePath.exists())
            return@ParboiledCommand fail("commands.flatpatch.prepare_workspace.err_workspace_doesnt_exist")

        if (!args.workplacePath.isDirectory)
            return@ParboiledCommand fail("commands.flatpatch.prepare_workspace.err_workspace_not_directory")

        if (args.game == null)
            return@ParboiledCommand fail("commands.flatpatch.prepare_workspace.err_no_game")

        when (args.game) {
            DR1 -> prepareDR12(args.workplacePath, DR1_WAD_REGEX, DR1_WAD_LANG_REGEX, DR1_WAD_KB_REGEX, DR1_FILE_SE_REGEX)
            DR2 -> prepareDR12(args.workplacePath, DR2_WAD_REGEX, DR2_WAD_LANG_REGEX, DR2_WAD_KB_REGEX, DR2_FILE_SE_REGEX)
        }

        return@ParboiledCommand SUCCESS
    }

    fun prepareDR12(workplacePath: File, wadRegex: Regex, langRegex: Regex, kbRegex: Regex, seRegex: Regex) {
        val wadFiles = workplacePath.listFiles().filter { file -> file.name.matches(wadRegex) }
            .sortedBy { file ->
                var weight = 0
                if (file.name.matches(langRegex))
                    weight = weight or 0b010
                if (file.name.matches(kbRegex))
                    weight = weight or 0b100
                return@sortedBy weight
            }

        //We're gonna try something here:
        //If we extract all the files to a folder - let's call it base_game - and then we create the operational folder called content, we should be able to create symlinks between them

        val baseGamePath = File(workplacePath, "base_game")
        baseGamePath.mkdir()

        val contentPath = File(workplacePath, "content")
        contentPath.mkdir()

        val backupWadPath = File(workplacePath, "backup_wads")
        backupWadPath.mkdir()

        wadFiles.forEach { wadFile ->
            val wad = WAD(wadFile::inputStream)
                ?: return@forEach printlnLocale(
                    "commands.flatpatch.prepare_workspace.err_not_wad",
                    wadFile.name
                )

            if (wad.files.isEmpty())
                return@forEach printlnLocale(
                    "commands.flatpatch.prepare_workspace.err_wad_no_files",
                    wadFile.name
                )

            val extractTime = measureTimeMillis {
                val files = wad.files.sortedBy(WADFileEntry::offset)

                arbitraryProgressBar(
                    loadingText = locale(
                        "commands.flatpatch.prepare_workspace.extracting",
                        wadFile.name
                    ), loadedText = ""
                ) {
                    wad.directories.forEach { entry ->
                        val name = entry.name.normalisePath()
                        val baseGameDir = File(baseGamePath, name)
                        val contentDir = File(contentPath, name)

                        baseGameDir.mkdirs()
                        contentDir.mkdirs()
                    }
                }

//                val totalCount = files.size.toLong()
//                var extracted: Long = 0
                arbitraryProgressBar(
                    loadingText = locale(
                        "commands.flatpatch.prepare_workspace.extracting",
                        wadFile.name
                    ), loadedText = ""
                ) {
                    //trackDownload(0, totalCount)
                    files.forEach { entry ->
                        val file = File(baseGamePath, entry.name.normalisePath())
                        entry.inputStream.use { stream -> FileOutputStream(file).use(stream::copyToStream) }
                        //trackDownload(++extracted, totalCount)
                    }
                }
            }
            val linkTime = measureTimeMillis {
                arbitraryProgressBar(
                    loadingText = locale(
                        "commands.flatpatch.prepare_workspace.linking",
                        wadFile.name
                    ), loadedText = ""
                ) {
                    wad.files.forEach { entry ->
                        val name = entry.name.normalisePath()
                        val contentFile = File(contentPath, name)
                        val baseGameFile = File(baseGamePath, name)

                        if (!contentFile.exists()) {
                            try {
                                Files.createLink(contentFile.toPath(), baseGameFile.toPath())
                            } catch (io: IOException) {
                                io.printStackTrace()
                                return@arbitraryProgressBar
                            }
                        }

                        if (entry.name.matches(seRegex)) {
                            val oggContentFile = File(contentPath, name.replace(".wav", ".ogg"))
                            if (!oggContentFile.exists()) {
                                try {
                                    Files.createLink(oggContentFile.toPath(), contentFile.toPath())
                                } catch (io: IOException) {
                                    io.printStackTrace()
                                    return@arbitraryProgressBar
                                }
                            }
                        }
                    }
                }
            }
            printlnLocale(
                "commands.flatpatch.prepare_workspace.extracted_wad",
                wadFile.name,
                extractTime,
                linkTime
            )

            val empty = customWAD {
                major = wad.major
                minor = wad.minor
            }

            val backupWadDest = File(backupWadPath, wadFile.name)
            if (!backupWadDest.exists())
                wadFile.renameTo(backupWadDest)

            FileOutputStream(wadFile).use(empty::compile)
        }
    }

    val patchExecutable = ParboiledCommand(patchExecutableRule) { stack ->
        val executableBuilder = stack[0] as PatchExecutableArgs

        //Step 1. Check all our data's there

        executableBuilder.executablePath = executableBuilder.executablePath?.normaliseForExecutable()
        if (executableBuilder.executablePath == null || executableBuilder.game == null || executableBuilder.builder) {
            if (executableBuilder.executablePath == null) {
                printLocale("commands.flatpatch.patch_executable.builder.executable")
                executableBuilder.executablePath = builders.filePath()?.normaliseForExecutable()
            }

            if (executableBuilder.game == null) {
                executableBuilder.executablePath?.takeIf(File::isFile)?.let { path ->
                    executableBuilder.game = when {
                        path.name.matches(DR1_EXECUTABLE_REGEX) -> DR1
                        path.name.matches(DR2_EXECUTABLE_REGEX) -> DR2
                        else -> null
                    }
                }

                executableBuilder.executablePath?.takeIf(File::isDirectory)?.let { path ->
                    executableBuilder.game = when {
                        path.name.matches(DR1_STEAM_FOLDER_REGEX) -> DR1
                        path.name.matches(DR2_STEAM_FOLDER_REGEX) -> DR2
                        else -> null
                    }
                }
            }

            if (executableBuilder.game == null) {
                printLocale("commands.flatpatch.patch_executable.builder.game")
                executableBuilder.game = builders.parameter()?.let { gameStr ->
                    if (DR1.names.any { str -> str.equals(gameStr, true) })
                        return@let DR1
                    if (DR2.names.any { str -> str.equals(gameStr, true) })
                        return@let DR2
                    if (V3.names.any { str -> str.equals(gameStr, true) })
                        return@let V3
                    return@ParboiledCommand fail(
                        "commands.flatpatch.patch_executable.builder.err_no_game_for_name",
                        gameStr
                    )
                } ?: return@ParboiledCommand fail(
                    "commands.flatpatch.patch_executable.builder.err_no_game_for_name",
                    ""
                )
            }
        }

        executableBuilder.executablePath?.takeIf(File::isDirectory)?.let { executablePath ->
            executableBuilder.game?.let { game ->
                val regex = when (game) {
                    DR1 -> DR1_EXECUTABLE_REGEX
                    DR2 -> DR2_EXECUTABLE_REGEX
                    else -> TODO("Make regex for $game executable")
                }
                executableBuilder.executablePath = executablePath.listFiles()
                    .firstOrNull { file -> file.name.matches(regex) }
                    ?: return@ParboiledCommand fail("commands.flatpatch.patch_executable.err_no_executable_child")
            }
        }

        val args = executableBuilder.makeImmutable()

        if (args.executablePath == null)
            return@ParboiledCommand fail("commands.flatpatch.patch_executable.err_no_workspace")

        if (!args.executablePath.exists())
            return@ParboiledCommand fail("commands.flatpatch.patch_executable.err_workspace_doesnt_exist")

        if (args.game == null)
            return@ParboiledCommand fail("commands.flatpatch.patch_executable.err_no_game")

        when (args.game) {
            in arrayOf(DR1, DR2) -> {
                //First thing's first, we make a backup
                val backup = File(args.executablePath.absolutePath + ".backup").let { backup ->
                    return@let if (backup.exists()) {
                        File(
                            args.executablePath.absolutePath + ".sha256_${FileInputStream(args.executablePath).use(
                                InputStream::sha256Hash
                            )}.backup"
                        )
                    } else {
                        backup
                    }
                }
                Files.copy(args.executablePath.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)

                RandomAccessFile(args.executablePath, "rw").use { raf ->
                    //Next up, check for an existing map
                    val executableMapFile = File(args.executablePath.absolutePath + ".smap")
                    val executableMap: SpiralDR1ExecutableMap

                    if (!executableMapFile.exists()) {
                        val strings: MutableMap<Long, String> = HashMap()
                        val builder = StringBuilder()
                        var startingAddress: Long = -1
                        val testedRange = 32..126 //We only really care about ASCII strings

                        //Find all the strings
                        arbitraryProgressBar(
                            loadingText = locale(
                                "commands.flatpatch.patch_executable.mapping",
                                args.executablePath.name
                            ), loadedText = "commands.flatpatch.patch_executable.finished_mapping"
                        ) {
                            for (i in 0 until raf.length()) {
                                val chr = raf.read()
                                if (chr in testedRange) {
                                    if (builder.isEmpty())
                                        startingAddress = i
                                    builder.append(chr.toChar())
                                } else if (chr == 0x00) {
                                    builder.toString().takeIf(String::isNotEmpty)
                                        ?.let { str -> strings[startingAddress] = str }
                                    builder.clear()
                                } else {
                                    builder.clear()
                                }
                            }
                        }

                        executableMap = SpiralDR1ExecutableMap(
                            archiveLocations = strings.mapValues { (_, value) ->
                                value.replace(
                                    "content/",
                                    "archive:"
                                )
                            }.filterValues { value -> value.contains("archive:") }.map { (addr, str) ->
                                val startIndex = str.indexOf("archive:")
                                return@map (addr + startIndex) to str.substring(startIndex)
                            }.toMap(),
                            sfxFormatLocation = strings.entries.let { entries ->
                                entries.firstOrNull { (_, value) -> value == "wav" }
                                    ?.let { (addr, str) -> Pair(addr, str) }
                                    ?: entries.firstOrNull { (_, value) -> value == "ogg" }?.let { (addr, value) ->
                                        addr to value.replace(
                                            "ogg",
                                            "wav"
                                        )
                                    }
                            }
                        )

                        SpiralSerialisation.JSON_MAPPER.writeValue(executableMapFile, executableMap)
                    } else {
                        executableMap = SpiralSerialisation.JSON_MAPPER.readValue(executableMapFile)
                    }

                    val size = executableMap.archiveLocations.size + 1L
                    val patchWarnings: MutableList<String> = ArrayList()
                    ProgressTracker(
                        downloadingText = "commands.flatpatch.patch_executable.patching",
                        downloadedText = "commands.flatpatch.patch_executable.finished_patching"
                    ) {
                        executableMap.archiveLocations.entries.forEachIndexed { index, (addr, original) ->
                            raf.seek(addr)
                            val current = raf.readZeroString(original.length)
                            if (current == original) {
                                raf.seek(addr)
                                raf.write(current.replace("archive:", "content/").toByteArray(Charsets.US_ASCII))
                            } else {
                                patchWarnings.add(
                                    locale(
                                        "commands.flatpatch.patch_executable.warn_differing_content",
                                        "0x${addr.toString(16)}",
                                        original,
                                        current
                                    )
                                )
                            }

                            trackDownload(index.toLong(), size)
                        }

                        executableMap.sfxFormatLocation?.let { (addr, original) ->
                            raf.seek(addr)
                            val current = raf.readZeroString(original.length)
                            if (current == original) {
                                raf.seek(addr)
                                raf.write(current.replace("wav", "ogg").toByteArray(Charsets.US_ASCII))
                            } else {
                                patchWarnings.add(
                                    locale(
                                        "commands.flatpatch.patch_executable.warn_differing_content",
                                        "0x${addr.toString(16)}",
                                        original,
                                        current
                                    )
                                )
                            }
                        } ?: FlatPatchPlugin.LOGGER.warn(
                            "commands.flatpatch.patch_executable.warn_no_sfx"
                        )

                        trackDownload(size, size)
                    }

                    patchWarnings.forEach(FlatPatchPlugin.LOGGER::warn)
                }
            }
        }

        return@ParboiledCommand SUCCESS
    }
}