package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.GameVersion
import hh.game.mgba_android.tracker.models.LogBaseStats
import hh.game.mgba_android.tracker.models.LogData
import hh.game.mgba_android.tracker.models.LogEncounter
import hh.game.mgba_android.tracker.models.LogEncounterArea
import hh.game.mgba_android.tracker.models.LogLevelMove
import hh.game.mgba_android.tracker.models.LogMove
import hh.game.mgba_android.tracker.models.LogPartyMon
import hh.game.mgba_android.tracker.models.LogPokemon
import hh.game.mgba_android.tracker.models.LogRoute
import hh.game.mgba_android.tracker.models.LogTM
import hh.game.mgba_android.tracker.models.LogTrainer
import hh.game.mgba_android.tracker.tables.AbilityTable
import hh.game.mgba_android.tracker.tables.MoveNames
import hh.game.mgba_android.tracker.tables.RouteNames
import hh.game.mgba_android.tracker.tables.SpeciesNames
import hh.game.mgba_android.tracker.tables.TrainerRouteTable
import hh.game.mgba_android.tracker.tables.TypeChart

/**
 * Parses a Universal Pokémon Randomizer `.log` spoiler file into [LogData].
 *
 * Direct port of `ironmon_tracker/data/RandomizerLog.lua`. One instance per parse
 * (holds the name→id maps and the Nidoran gender toggle). The port keeps parity
 * with the Lua sector patterns, encounter-rate tables, and the FRLG route
 * Set#→mapId mapping. Route wild-encounter mapping is currently FRLG-only (RSE/
 * Emerald route Set# tables are not yet ported); all other sectors work for every
 * game since they are name/id driven, not memory driven.
 */
class RandomizerLog(
    private val game: GameVersion,
    private val isMaxFr: Boolean = false,
    private val isNatDex: Boolean = false,
) {
    private val isHoenn = game == GameVersion.RUBY || game == GameVersion.SAPPHIRE || game == GameVersion.EMERALD

    private val data = LogData()

    // name → internal id maps (built during parse; log names may be custom)
    private val pokemonNameToId = HashMap<String, Int>()
    private val moveNameToId = HashMap<String, Int>()
    private val abilityNameToId = HashMap<String, Int>()
    private val typeNameToId = HashMap<String, Int>()
    private var routeSetNumToId: Map<Int, Int> = emptyMap()

    private var currentNidoranIsF = true

    companion object {
        // Encounter areas: key → (logKey, combined slot rates). Mirrors RandomizerLog.EncounterTypes.
        private val GRASS_RATES = doubleArrayOf(0.20, 0.20, 0.10, 0.10, 0.10, 0.10, 0.05, 0.05, 0.04, 0.04, 0.01, 0.01)
        private val SURF_RATES = doubleArrayOf(0.60, 0.30, 0.05, 0.04, 0.01)
        private val ROCKSMASH_RATES = doubleArrayOf(0.60, 0.30, 0.05, 0.04, 0.01)
        private val OLDROD_RATES = doubleArrayOf(0.70, 0.30)
        private val GOODROD_RATES = doubleArrayOf(0.60, 0.20, 0.20)
        private val SUPERROD_RATES = doubleArrayOf(0.40, 0.40, 0.15, 0.04, 0.01)

        // Encounter-type detection order: key → lowercase logKey substring to search for.
        private val WILD_LOG_KEYS = listOf(
            "GrassCave" to "grass/cave",
            "Surfing" to "surfing",
            "RockSmash" to "rock smash",
            "OldRod" to "fishing", // Fishing splits into Old/Good/Super by slot index
        )
    }

    // ── Public entry point ──────────────────────────────────────────────────
    fun parse(lines: List<String>): LogData? {
        if (lines.isEmpty()) return null
        setupMappings()

        val sectors = locateSectors(lines)
        parseRandomizerSettings(lines)
        parseRandomizerGame(lines)
        sectors["BaseStatsItems"]?.let { parseBaseStatsItems(lines, it) } // must be first
        sectors["Evolutions"]?.let { parseEvolutions(lines, it) }
        sectors["Moves"]?.let { parseMoves(lines, it) }
        sectors["MoveSets"]?.let { parseMoveSets(lines, it) }
        sectors["TMMoves"]?.let { parseTMMoves(lines, it) }
        sectors["TMCompatibility"]?.let { parseTMCompatibility(lines, it) }
        sectors["Trainers"]?.let { parseTrainers(lines, it) }
        parseRoutes(lines, sectors["Routes"])
        return data
    }

    // ── Name normalization ──────────────────────────────────────────────────
    private fun formatInput(raw: String?): String {
        if (raw == null) return ""
        var s = raw.trim()
        s = s.replace("♀", " F").replace("♂", " M")
        s = s.replace("?", "")
        s = s.replace("’", "'") // curly apostrophe → straight
        s = s.replace("[PK][MN]", "PKMN")
        // Fold the few accented characters that appear in Gen III names.
        s = s.replace("é", "e").replace("É", "E")
        return s.lowercase().trim()
    }

    /** Nidoran ♀/♂ symbols are sometimes stripped; alternate F/M in file order. */
    private fun alternateNidorans(name: String): String {
        if (name.isEmpty() || name.lowercase() != "nidoran") return name
        val correct = if (currentNidoranIsF) "$name f" else "$name m"
        currentNidoranIsF = !currentNidoranIsF
        return correct
    }

    // ── Mappings ────────────────────────────────────────────────────────────
    private fun setupMappings() {
        // Move names → ids (from the app's static tables; matches the Lua approach)
        for (id in 1..600) {
            val n = MoveNames.get(id, isMaxFr)
            if (n.startsWith("Move#")) continue
            moveNameToId[formatInput(n)] = id
        }
        // Ability names → ids
        for (id in 1..170) {
            val n = AbilityTable.get(id, isMaxFr).name
            if (n == "None" || n == "???") continue
            abilityNameToId[formatInput(n)] = id
        }
        // Type names → ids
        for ((id, name) in TypeChart.TYPE_NAMES) {
            typeNameToId[name.lowercase()] = id
        }
        routeSetNumToId = when (game) {
            GameVersion.FIRE_RED, GameVersion.LEAF_GREEN -> FRLG_ROUTE_SET_NUM_TO_ID
            else -> emptyMap() // RSE/Emerald route Set# tables not yet ported
        }
    }

    // ── Sector location ─────────────────────────────────────────────────────
    /** Returns sector key → first content line index (the line AFTER the header). */
    private fun locateSectors(lines: List<String>): Map<String, Int> {
        val headers = mapOf(
            "Evolutions" to "randomized evolutions",
            "BaseStatsItems" to "pokemon base stats & types",
            "Moves" to "move data",
            "MoveSets" to "pokemon movesets",
            "TMMoves" to "tm moves",
            "TMCompatibility" to "tm compatibility",
            "Trainers" to "trainers pokemon",
            "Routes" to "wild pokemon",
        )
        val result = HashMap<String, Int>()
        for ((i, line) in lines.withIndex()) {
            val norm = line.trim().trim('-').trim(':').trim().lowercase()
            for ((key, name) in headers) {
                if (!result.containsKey(key) && norm == name) {
                    result[key] = i + 1
                    break
                }
            }
        }
        return result
    }

    // ── Settings / game ─────────────────────────────────────────────────────
    private fun parseRandomizerSettings(lines: List<String>) {
        if (lines.size < 3) return
        data.settings.version = Regex("Randomizer Version:\\s*(\\S+)").find(lines[0])?.groupValues?.get(1)
        data.settings.randomSeed = Regex("^Random Seed:\\s*(\\d+)").find(lines[1])?.groupValues?.get(1)
        data.settings.settingsString = Regex("^Settings String:\\s*(.+?)\\s*$").find(lines[2])?.groupValues?.get(1)
    }

    private fun parseRandomizerGame(lines: List<String>) {
        val re = Regex("^Randomization of\\s*(.+?)\\s+completed")
        for (line in lines) {
            val m = re.find(line) ?: continue
            data.settings.game = m.groupValues[1].trim()
            return
        }
    }

    // ── Base stats & types ──────────────────────────────────────────────────
    private fun parseBaseStatsItems(lines: List<String>, start: Int) {
        var i = start + 1 // skip the table header row
        while (i < lines.size) {
            val cols = lines[i].split("|")
            val id = cols.getOrNull(0)?.trim()?.toIntOrNull()
            // end of sector: not a data row, or missing the speed column
            if (id == null || cols.size < 9) return
            val rawName = normalizeName(cols[1])
            val key = formatInput(cols[1]).let { alternateNidorans(it) }
            if (key.isEmpty()) return

            val internalId = SpeciesNames.nationalToInternal(id)
            val mon = data.pokemon.getOrPut(internalId) { LogPokemon(internalId) }
            pokemonNameToId[key] = internalId
            mon.name = rawName

            val types = formatInput(cols[2]).split("/")
            mon.type1 = typeNameToId[types.getOrElse(0) { "" }.trim()]
            mon.type2 = typeNameToId[types.getOrElse(1) { "" }.trim()]

            mon.baseStats = LogBaseStats(
                hp = cols[3].trim().toIntOrNull() ?: 0,
                atk = cols[4].trim().toIntOrNull() ?: 0,
                def = cols[5].trim().toIntOrNull() ?: 0,
                spa = cols[6].trim().toIntOrNull() ?: 0,
                spd = cols[7].trim().toIntOrNull() ?: 0,
                spe = cols[8].trim().toIntOrNull() ?: 0,
            )

            mon.ability1 = abilityNameToId[formatInput(cols.getOrNull(9))] ?: 0
            mon.ability2 = abilityNameToId[formatInput(cols.getOrNull(10))]

            val held = cols.getOrNull(11)?.trim()
            if (!held.isNullOrEmpty() && held.any { it.isLetter() }) mon.heldItems = normalizeName(held)

            i++
        }
    }

    /** Display name: keep original casing, normalize gender symbols / PKMN / apostrophe. */
    private fun normalizeName(raw: String): String {
        return raw.trim()
            .replace("♀", " F").replace("♂", " M")
            .replace("?", "")
            .replace("’", "'")
            .replace("[PK][MN]", "PKMN")
            .trim()
    }

    // ── Evolutions ──────────────────────────────────────────────────────────
    private fun parseEvolutions(lines: List<String>, start: Int) {
        val re = Regex("^(.*?)\\s*->\\s*(.*)")
        var i = start
        while (i < lines.size) {
            val m = re.find(lines[i])
            val name = m?.let { alternateNidorans(formatInput(it.groupValues[1])) }
            val evos = m?.groupValues?.get(2)
            if (name == null || name.isEmpty() || evos == null || !pokemonNameToId.containsKey(name)) return

            val pokemonId = pokemonNameToId[name]!!
            val mon = data.pokemon[pokemonId] ?: return
            mon.evolutions.clear()
            for (evoRaw in evos.replace(" and ", ", ").split(",")) {
                val evoName = alternateNidorans(formatInput(evoRaw))
                val evoId = pokemonNameToId[evoName] ?: continue
                mon.evolutions.add(evoId)
                data.pokemon[evoId]?.preEvolutions?.add(pokemonId)
            }
            i++
        }
    }

    // ── Move data ───────────────────────────────────────────────────────────
    private fun parseMoves(lines: List<String>, start: Int) {
        var i = start + 1 // skip table header
        while (i < lines.size) {
            val cols = lines[i].split("|")
            val moveId = cols.getOrNull(0)?.trim()?.toIntOrNull()
            if (moveId == null || cols.size < 3) return
            val name = normalizeName(cols[1])
            if (name.isEmpty()) return
            data.moves[moveId] = LogMove(
                moveId = moveId,
                name = name,
                type = typeNameToId[formatInput(cols[2]).trim()],
                power = cols.getOrNull(3)?.trim()?.toIntOrNull(),
                acc = cols.getOrNull(4)?.trim()?.toIntOrNull(),
                pp = cols.getOrNull(5)?.trim()?.toIntOrNull(),
            )
            i++
        }
    }

    // ── Movesets ────────────────────────────────────────────────────────────
    private fun parseMoveSets(lines: List<String>, start: Int) {
        val nextMon = Regex("^\\d+\\s(.*?)\\s*->")
        val evoMove = Regex("^Learned upon evolution:\\s(.*)")
        val levelMove = Regex("^Level\\s(\\d+)\\s{0,2}:\\s(.*)")
        var i = start
        while (i < lines.size) {
            var name = lines[i].let { nextMon.find(it)?.groupValues?.get(1) }
                ?.let { alternateNidorans(formatInput(it)) }
            // Find the next known Pokémon name, or end of sector
            while (name == null || !pokemonNameToId.containsKey(name)) {
                if (lines[i].startsWith("--") || i + 1 >= lines.size) return
                i++
                name = lines[i].let { nextMon.find(it)?.groupValues?.get(1) }
                    ?.let { alternateNidorans(formatInput(it)) }
            }
            val mon = data.pokemon[pokemonNameToId[name]!!]
            if (mon != null) {
                mon.moveSet.clear()
                i++ // move past the name line
                // The Lua hardcodes `index += 7` to skip 6 redundant base-set preamble
                // lines; instead scan forward to the first real move line so any number
                // of preamble lines is tolerated. Stop early if the next mon / sector end
                // is reached (this mon simply has no level-up moves).
                while (i < lines.size && evoMove.find(lines[i]) == null && levelMove.find(lines[i]) == null) {
                    if (nextMon.containsMatchIn(lines[i]) || lines[i].startsWith("--")) break
                    i++
                }
                // Parse consecutive move lines (level 0 = "Learned upon evolution")
                while (i < lines.size) {
                    val evo = evoMove.find(lines[i])
                    val lm = levelMove.find(lines[i])
                    val level: Int
                    val moveName: String
                    when {
                        evo != null -> { level = 0; moveName = evo.groupValues[1] }
                        lm != null -> { level = lm.groupValues[1].toIntOrNull() ?: 0; moveName = lm.groupValues[2] }
                        else -> break
                    }
                    mon.moveSet.add(LogLevelMove(level, moveNameToId[formatInput(moveName)] ?: 0, normalizeName(moveName)))
                    i++
                }
            } else {
                i++
            }
        }
    }

    // ── TM moves ────────────────────────────────────────────────────────────
    private fun parseTMMoves(lines: List<String>, start: Int) {
        val re = Regex("^TM(\\d+)\\s(.*)")
        var i = start
        while (i < lines.size) {
            val m = re.find(lines[i])
            val tmNum = m?.groupValues?.get(1)?.toIntOrNull()
            val moveName = m?.groupValues?.get(2)
            if (tmNum == null || moveName == null || formatInput(moveName).isEmpty()) return
            data.tms[tmNum] = LogTM(tmNum, moveNameToId[formatInput(moveName)] ?: 0, normalizeName(moveName))
            i++
        }
    }

    // ── TM compatibility ────────────────────────────────────────────────────
    private fun parseTMCompatibility(lines: List<String>, start: Int) {
        val re = Regex("^\\s*\\d+\\s*(.*?)\\s*\\|(.*)")
        val tmRe = Regex("TM(\\d+)")
        var i = start
        while (i < lines.size) {
            val m = re.find(lines[i])
            val name = m?.let { alternateNidorans(formatInput(it.groupValues[1])) }
            val tms = m?.groupValues?.get(2)
            if (name == null || name.isEmpty() || tms == null || !pokemonNameToId.containsKey(name)) return
            val mon = data.pokemon[pokemonNameToId[name]!!] ?: return
            mon.tmMoves.clear()
            for (tm in tmRe.findAll(tms)) {
                val n = tm.groupValues[1].toIntOrNull() ?: continue
                if (data.tms.containsKey(n)) mon.tmMoves.add(n)
            }
            i++
        }
    }

    // ── Trainers ────────────────────────────────────────────────────────────
    private fun parseTrainers(lines: List<String>, start: Int) {
        val next = Regex("^#(\\d+)\\s\\(([^=>]+)\\s*=?>?\\s*(.*)\\)\\S*\\s-\\s(.*)")
        val partyMon = Regex("\\s*(.*?)\\sLv(\\d+)")
        var i = start
        while (i < lines.size) {
            val m = next.find(lines[i])
            val num = m?.groupValues?.get(1)?.toIntOrNull()
            val fullname = m?.let { formatInput(it.groupValues[2]) }
            val customFull = m?.let { formatInput(it.groupValues[3]) }
            val party = m?.groupValues?.get(4)
            if (num == null || fullname == null || party == null) return

            val (tClass, tName) = splitTrainerClassAndName(fullname)
            val (cClass, cName) = splitTrainerClassAndName(customFull ?: "")
            val trainer = LogTrainer(num, tName, tClass, fullname, cName, cClass)

            var avgSum = 0.0
            for (partySeg in party.split(",")) {
                val pm = partyMon.find(partySeg) ?: continue
                val monAndItem = pm.groupValues[1].split("@")
                val monName = alternateNidorans(formatInput(monAndItem.getOrElse(0) { "" }))
                var held: String? = formatInput(monAndItem.getOrNull(1))
                if (held.isNullOrEmpty()) held = null
                val level = pm.groupValues[2].toIntOrNull() ?: 0
                val pid = pokemonNameToId[monName] ?: continue

                val member = LogPartyMon(pid, held, level)
                if (level < (trainer.minLevel ?: 999)) trainer.minLevel = level
                if (level > (trainer.maxLevel ?: 0)) trainer.maxLevel = level
                avgSum += level

                // Reconstruct current moves by walking level-up set backwards from level
                val ms = data.pokemon[pid]?.moveSet ?: emptyList()
                for (j in ms.indices.reversed()) {
                    if (ms[j].level <= level) {
                        member.moveIds.add(0, ms[j].moveId)
                        if (member.moveIds.size >= 4) break
                    }
                }
                trainer.party.add(member)
            }
            if (trainer.party.isNotEmpty()) trainer.avgLevel = avgSum / trainer.party.size
            data.trainers[num] = trainer
            i++
        }
    }

    /** Returns (class, name) split from the trainer's full name. Port of splitTrainerClassAndName. */
    private fun splitTrainerClassAndName(fullname: String): Pair<String, String> {
        val f = fullname
        val pattern = when {
            f.contains("&") -> Regex("(.*?)\\s*(\\S+\\s*&\\s*\\S+)$")
            f.contains("lt. ") -> Regex("(.*?)\\s*(\\S+\\s\\S+)$")
            else -> Regex("(.*?)\\s*(\\S+)$")
        }
        val m = pattern.find(f) ?: return "" to f
        var cls = m.groupValues[1]
        var name = m.groupValues[2]
        if (name.isEmpty()) { name = cls; cls = "" }
        return cls.trim() to name.trim()
    }

    // ── Routes ──────────────────────────────────────────────────────────────
    private fun parseRoutes(lines: List<String>, start: Int?) {
        // 1. Seed routes with trainer info from the app's TrainerRouteTable.
        for ((mapId, trainerIds) in TrainerRouteTable.get(game)) {
            val route = data.routes.getOrPut(mapId) { LogRoute(mapId, RouteNames.get(mapId, isHoenn)) }
            val area = LogEncounterArea("Trainers")
            var numAdded = 0
            var avg = 0.0
            for (tid in trainerIds) {
                val t = data.trainers[tid] ?: continue
                numAdded++
                if ((t.minLevel ?: 999) < (route.minTrainerLv ?: 999)) route.minTrainerLv = t.minLevel
                if ((t.maxLevel ?: -1) > (route.maxTrainerLv ?: 0)) route.maxTrainerLv = t.maxLevel
                avg += t.avgLevel ?: 0.0
                area.trainers.add(tid)
            }
            if (numAdded > 0) {
                route.numTrainers = numAdded
                route.avgTrainerLv = avg / numAdded
                route.encounterAreas["Trainers"] = area
            }
        }

        if (start == null) return

        // Concatenated fishing rates: Old(2) + Good(3) + Super(5) = 10 slots
        val fishingRates = OLDROD_RATES + GOODROD_RATES + SUPERROD_RATES

        val routeRe = Regex("^Set #(\\d+)\\s*-\\s*(.*?)\\s*\\(.*\\)")
        val monRe = Regex("^(.*?)\\s*Lvs?\\s?(\\d+)-?(\\d*)\\s*(.*)")
        var i = start
        while (i < lines.size) {
            var rm = routeRe.find(lines[i])
            var setNum = rm?.groupValues?.get(1)?.toIntOrNull()
            var nameEnc = rm?.let { formatInput(it.groupValues[2]) }
            while (setNum == null || nameEnc == null) {
                if (lines[i].startsWith("--") || i + 1 >= lines.size) return
                i++
                rm = routeRe.find(lines[i])
                setNum = rm?.groupValues?.get(1)?.toIntOrNull()
                nameEnc = rm?.let { formatInput(it.groupValues[2]) }
            }

            // Determine encounter type from the encounter name text
            var encKey: String? = null
            var isFishing = false
            for ((key, logKey) in WILD_LOG_KEYS) {
                if (nameEnc.contains(logKey)) {
                    encKey = key
                    isFishing = (logKey == "fishing")
                    break
                }
            }

            val mapId = routeSetNumToId[setNum] ?: 0
            if (mapId != 0 && encKey != null) {
                i++
                val route = data.routes.getOrPut(mapId) { LogRoute(mapId, RouteNames.get(mapId, isHoenn)) }

                if (isFishing) {
                    route.encounterAreas["OldRod"] = LogEncounterArea("OldRod")
                    route.encounterAreas["GoodRod"] = LogEncounterArea("GoodRod")
                    route.encounterAreas["SuperRod"] = LogEncounterArea("SuperRod")
                } else {
                    route.encounterAreas[encKey] = LogEncounterArea(encKey)
                }

                val rates = if (isFishing) fishingRates else ratesFor(encKey)
                var encIndex = 1
                var lastArea: LogEncounterArea? = null

                var mm = if (i < lines.size) monRe.find(lines[i]) else null
                while (mm != null) {
                    val monName = formatInput(mm.groupValues[1])
                    val pid = pokemonNameToId[monName]
                    if (pid != null) {
                        val area = when {
                            !isFishing -> route.encounterAreas[encKey]!!
                            encIndex <= 2 -> route.encounterAreas["OldRod"]!!
                            encIndex <= 5 -> route.encounterAreas["GoodRod"]!!
                            else -> route.encounterAreas["SuperRod"]!!
                        }
                        lastArea = area
                        val minLv = mm.groupValues[2].toIntOrNull() ?: 0
                        val maxLv = mm.groupValues[3].toIntOrNull() ?: minLv
                        val enc = area.pokemon.getOrPut(pid) { LogEncounter().also { it.index = encIndex } }
                        enc.levelMin = minOf(enc.levelMin, minLv)
                        enc.levelMax = maxOf(enc.levelMax, maxLv)
                        enc.rate += rates.getOrElse(encIndex - 1) { 0.0 }
                    }
                    i++
                    encIndex++
                    if (i >= lines.size) break
                    mm = monRe.find(lines[i])
                }

                // Record wild level range once (grass/cave usually parsed first)
                if (route.minWildLv == null || route.maxWildLv == null) {
                    val area = lastArea
                    if (area != null) {
                        for (p in area.pokemon.values) {
                            route.numWilds++
                            if (p.levelMin < (route.minWildLv ?: 999)) route.minWildLv = p.levelMin
                            if (p.levelMax > (route.maxWildLv ?: 0)) route.maxWildLv = p.levelMax
                        }
                    }
                }
                if (i >= lines.size) return
            } else {
                i++
            }
        }
    }

    private fun ratesFor(key: String): DoubleArray = when (key) {
        "GrassCave" -> GRASS_RATES
        "Surfing" -> SURF_RATES
        "RockSmash" -> ROCKSMASH_RATES
        "OldRod" -> OLDROD_RATES
        "GoodRod" -> GOODROD_RATES
        "SuperRod" -> SUPERROD_RATES
        else -> DoubleArray(0)
    }
}
