package hh.game.mgba_android.tracker.data

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser

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
        context: Context,
        reader: (Long, Int) -> ByteArray?,
    ): Pair<MaxFrVariant, GameAddresses>? {
        for ((variant, file) in VARIANTS) {
            try {
                val text = context.assets.open(file).bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(text).asJsonObject
                val addr = root.getAsJsonObject("Addresses")
                val gBattleMovesAddr = hex(addr, "gBattleMoves")
                // Probe move 1 (Pound) at gBattleMoves + 12:
                // struct is 12 bytes; byte offsets 1-4 = power/type/accuracy/pp
                val bytes = reader(gBattleMovesAddr + 12L, 5) ?: continue
                if (bytes[1].toInt() and 0xFF == 40  &&  // power
                    bytes[2].toInt() and 0xFF == 0   &&  // type (Normal)
                    bytes[3].toInt() and 0xFF == 100 &&  // accuracy
                    bytes[4].toInt() and 0xFF == 35) {   // pp
                    Log.d(TAG, "Detected $variant via $file (gBattleMoves=0x${gBattleMovesAddr.toString(16).uppercase()})")
                    return variant to build(context, root, addr)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading $file", e)
            }
        }
        return null
    }

    // All values in the JSON Addresses section are hex strings, with or without "0X" prefix.
    private fun hex(o: JsonObject, key: String): Long {
        val s = o.get(key)?.asString?.uppercase() ?: return 0L
        val digits = s.removePrefix("0X").trimStart('0').ifEmpty { "0" }
        return digits.toLong(16)
    }

    private fun hexInt(o: JsonObject, key: String): Int = hex(o, key).toInt()

    private fun loadLua(context: Context, root: JsonObject, fileKey: String, startIdKey: String): Map<Int, MaxExtPokemon> {
        val file    = root.get(fileKey)?.asString ?: return emptyMap()
        val startId = root.get(startIdKey)?.asInt ?: return emptyMap()
        return try {
            val lua = context.assets.open(file).bufferedReader().use { it.readText() }
            MaxExtPokemonParser.parseFirstBlock(lua, startId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $file", e)
            emptyMap()
        }
    }

    private fun build(context: Context, root: JsonObject, a: JsonObject): GameAddresses {
        val extMap = buildMap<Int, MaxExtPokemon> {
            putAll(loadLua(context, root, "gen4File", "gen4StartId"))
            putAll(loadLua(context, root, "gen5File", "gen5StartId"))
        }
        // Load abilities and moves into the global store
        MaxExtDataStore.abilityMap = loadAbilities(context, root)
        MaxExtDataStore.moveMap    = loadMoves(context, root)
        return buildAddresses(a, extMap)
    }

    private fun loadAbilities(context: Context, root: JsonObject): Map<Int, MaxExtAbility> {
        val file = root.get("abilitiesFile")?.asString ?: return emptyMap()
        return try {
            val lua = context.assets.open(file).bufferedReader().use { it.readText() }
            MaxExtAbilityParser.parse(lua)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $file", e)
            emptyMap()
        }
    }

    private fun loadMoves(context: Context, root: JsonObject): Map<Int, MaxExtMove> {
        val file = root.get("movesFile")?.asString ?: return emptyMap()
        return try {
            val lua = context.assets.open(file).bufferedReader().use { it.readText() }
            MaxExtMoveParser.parse(lua)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $file", e)
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
