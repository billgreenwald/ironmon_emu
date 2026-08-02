package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.platform.AssetReader
import hh.game.mgba_android.tracker.platform.TrackerLog
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MaxFrAddressLoader {

    private const val TAG = "MaxFrAddressLoader"

    // Ordered list — detection probes each file's gBattleMoves until a match is found.
    // Adding a new MaxFR variant = drop a new JSON in maxdata/ and add an entry here.
    private val VARIANTS: List<Pair<MaxFrVariant, String>> = listOf(
        MaxFrVariant.MAX_FR_GEN5_FR to "maxdata/max-fr-gen5-fr.json",
        MaxFrVariant.MAX_FR_GEN4    to "maxdata/max-fr-gen4.json",
        MaxFrVariant.MAX_FR         to "maxdata/max-fr.json",
        MaxFrVariant.MAX_EM         to "maxdata/max-em.json",
    )

    /**
     * Detects the MaxFR/MaxEM variant by probing gBattleMoves from each JSON.
     * On match, returns the variant + fully-loaded GameAddresses. Returns null if no match.
     * Should be called once per ROM load (when game code changes), not every poll tick.
     */
    fun detectAndLoad(
        assets: AssetReader,
        reader: (Long, Int) -> ByteArray?,
    ): Pair<MaxFrVariant, GameAddresses>? {
        for ((variant, file) in VARIANTS) {
            try {
                val text = assets.readText(file) ?: continue
                val root = Json.parseToJsonElement(text).jsonObject
                val addr = root["Addresses"]!!.jsonObject
                val gBattleMovesAddr = hex(addr, "gBattleMoves")
                // Probe move 1 (Pound) at gBattleMoves + 12:
                // struct is 12 bytes; byte offsets 1-4 = power/type/accuracy/pp
                val bytes = reader(gBattleMovesAddr + 12L, 5) ?: continue
                if (bytes[1].toInt() and 0xFF == 40  &&  // power
                    bytes[2].toInt() and 0xFF == 0   &&  // type (Normal)
                    bytes[3].toInt() and 0xFF == 100 &&  // accuracy
                    bytes[4].toInt() and 0xFF == 35) {   // pp
                    TrackerLog.d(TAG, "Detected $variant via $file (gBattleMoves=0x${gBattleMovesAddr.toString(16).uppercase()})")
                    return variant to build(assets, root, addr)
                }
            } catch (e: Exception) {
                TrackerLog.e(TAG, "Error reading $file", e)
            }
        }
        return null
    }

    // All values in the JSON Addresses section are hex strings, with or without "0X" prefix.
    private fun hex(o: JsonObject, key: String): Long {
        val s = o[key]?.jsonPrimitive?.contentOrNull?.uppercase() ?: return 0L
        val digits = s.removePrefix("0X").trimStart('0').ifEmpty { "0" }
        return digits.toLong(16)
    }

    private fun hexInt(o: JsonObject, key: String): Int = hex(o, key).toInt()

    private fun str(o: JsonObject, key: String): String? = o[key]?.jsonPrimitive?.contentOrNull
    private fun int(o: JsonObject, key: String): Int? = o[key]?.jsonPrimitive?.intOrNull

    private fun loadLua(assets: AssetReader, root: JsonObject, fileKey: String, startIdKey: String, spriteStartKey: String): Map<Int, MaxExtPokemon> {
        val file        = str(root, fileKey) ?: return emptyMap()
        val startId     = int(root, startIdKey) ?: return emptyMap()
        val spriteStart = int(root, spriteStartKey) ?: startId
        return try {
            val lua = assets.readText(file) ?: return emptyMap()
            MaxExtPokemonParser.parseFirstBlock(lua, startId, spriteStart)
        } catch (e: Exception) {
            TrackerLog.e(TAG, "Failed to load $file", e)
            emptyMap()
        }
    }

    private fun loadLuaLegendaries(assets: AssetReader, root: JsonObject): Map<Int, MaxExtPokemon> {
        val startId     = int(root, "gen4LegendaryStartId") ?: return emptyMap()
        val spriteStart = int(root, "gen4LegendarySpriteStart") ?: startId
        val spriteIds   = root["gen4LegendarySpriteIds"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonPrimitive.int }.getOrNull() } ?: emptyList()
        val file        = str(root, "gen4File") ?: return emptyMap()
        return try {
            val lua = assets.readText(file) ?: return emptyMap()
            MaxExtPokemonParser.parseSecondBlock(lua, startId, spriteStart, spriteIds)
        } catch (e: Exception) {
            TrackerLog.e(TAG, "Failed to load gen4 legendaries from $file", e)
            emptyMap()
        }
    }

    private fun build(assets: AssetReader, root: JsonObject, a: JsonObject): GameAddresses {
        val extMap = buildMap<Int, MaxExtPokemon> {
            putAll(loadLua(assets, root, "gen4File", "gen4StartId", "gen4SpriteStart"))
            putAll(loadLua(assets, root, "gen5File", "gen5StartId", "gen5SpriteStart"))
            putAll(loadLuaLegendaries(assets, root))
        }
        // Load abilities and moves into the global store
        MaxExtDataStore.abilityMap = loadAbilities(assets, root)
        MaxExtDataStore.moveMap    = loadMoves(assets, root)
        return buildAddresses(a, extMap)
    }

    private fun loadAbilities(assets: AssetReader, root: JsonObject): Map<Int, MaxExtAbility> {
        val file = str(root, "abilitiesFile") ?: return emptyMap()
        return try {
            val lua = assets.readText(file) ?: return emptyMap()
            MaxExtAbilityParser.parse(lua)
        } catch (e: Exception) {
            TrackerLog.e(TAG, "Failed to load $file", e)
            emptyMap()
        }
    }

    private fun loadMoves(assets: AssetReader, root: JsonObject): Map<Int, MaxExtMove> {
        val file = str(root, "movesFile") ?: return emptyMap()
        return try {
            val lua = assets.readText(file) ?: return emptyMap()
            MaxExtMoveParser.parse(lua)
        } catch (e: Exception) {
            TrackerLog.e(TAG, "Failed to load $file", e)
            emptyMap()
        }
    }

    private fun buildAddresses(a: JsonObject, extMap: Map<Int, MaxExtPokemon>): GameAddresses = GameAddresses(
        partyCount               = hex(a, "gPlayerPartyCount"),
        partyBase                = hex(a, "pstats"),
        baseStatsTable           = hex(a, "gBaseStats"),
        levelUpLearnsets         = hex(a, "gLevelUpLearnsets"),
        experienceTables         = hex(a, "gExperienceTables"),
        enemyParty               = hex(a, "estats"),
        battleTypeFlags          = hex(a, "gBattleTypeFlags"),
        battleMons               = hex(a, "gBattleMons"),
        battlersCount            = hex(a, "gBattlersCount"),
        battleWeather            = hex(a, "gBattleWeather"),
        sideStatuses             = hex(a, "gSideStatuses"),
        sideTimers               = hex(a, "gSideTimers"),
        battleOutcome            = hex(a, "gBattleOutcome"),
        battleResults            = hex(a, "gBattleResults"),
        gMapHeader               = hex(a, "gMapHeader"),
        saveBlock1Ptr            = hex(a, "gSaveBlock1ptr"),
        saveBlock1IsPointer      = true,
        gameStatsOffset          = hexInt(a, "gameStatsOffset"),
        gameFlagsOffset          = hexInt(a, "gameFlagsOffset"),
        saveBlock2Ptr            = hex(a, "gSaveBlock2ptr"),
        encryptionKeyOffset      = hexInt(a, "EncryptionKeyOffset"),
        bagPocket_Items_offset   = hexInt(a, "bagPocket_Items_offset"),
        bagPocket_Items_size     = hexInt(a, "bagPocket_Items_Size"),
        bagPocket_Berries_offset = hexInt(a, "bagPocket_Berries_offset"),
        bagPocket_Berries_size   = hexInt(a, "bagPocket_Berries_Size"),
        trainerBattleOpponent    = hex(a, "gTrainerBattleOpponent_A"),
        gBattlerPartyIndexes     = hex(a, "gBattlerPartyIndexes"),
        sSpecialFlags            = hex(a, "sSpecialFlags"),
        gHitMarker               = hex(a, "gHitMarker"),
        gMoveResultFlags         = hex(a, "gMoveResultFlags"),
        gBattleCommunication     = hex(a, "gBattleCommunication"),
        gBattleMoves             = hex(a, "gBattleMoves"),
        hasFairy                 = true,
        extPokemonMap            = extMap,
    )
}
