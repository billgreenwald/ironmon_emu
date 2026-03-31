package hh.game.mgba_android.tracker.data

import hh.game.mgba_android.tracker.models.EnemyData
import hh.game.mgba_android.tracker.models.PokemonData
import hh.game.mgba_android.tracker.models.Weather

/**
 * Calculates actual power values for variable-power moves during battle.
 * Formulas sourced from Ironmon-Tracker/ironmon_tracker/Utils.lua.
 */
object BattlePowerCalc {

    // Move IDs for variable-power moves (from MoveData.lua MoveData.Values)
    private const val LOW_KICK     = 67
    private const val FLAIL        = 175
    private const val REVERSAL     = 179
    private const val RETURN       = 216
    private const val FRUSTRATION  = 218
    private const val HIDDEN_POWER = 237
    private const val ERUPTION     = 284
    private const val WEATHER_BALL = 311
    private const val WATER_SPOUT  = 323

    /**
     * Returns a computed power string for variable-power moves, or null if the move
     * is not variable or data is unavailable. Only computes when [inBattle] is true.
     * Source: Utils.calculateWeightBasedDamage / calculateLowHPBasedDamage /
     *         calculateHighHPBasedDamage / calculateFriendshipBasedDamage / calculateWeatherBall
     */
    fun calculate(
        moveId: Int,
        player: PokemonData,
        enemy: EnemyData?,
        weather: Weather,
        inBattle: Boolean,
    ): String? {
        if (!inBattle) return null
        return when (moveId) {
            FLAIL, REVERSAL    -> lowHpBasedDamage(player.currentHp, player.maxHp)
            ERUPTION,
            WATER_SPOUT        -> highHpBasedDamage(player.currentHp, player.maxHp)
            LOW_KICK           -> lowKickPower(enemy?.speciesId ?: 0)
            RETURN             -> friendshipBasedDamage(player.friendship, inverse = false)
            FRUSTRATION        -> friendshipBasedDamage(player.friendship, inverse = true)
            HIDDEN_POWER       -> hiddenPowerValue(
                player.ivHp, player.ivAtk, player.ivDef,
                player.ivSpe, player.ivSpA, player.ivSpD,
            )
            WEATHER_BALL       -> weatherBallPower(weather)
            else               -> null
        }
    }

    // ── Utils.calculateLowHPBasedDamage ──────────────────────────────────────────
    // For Flail & Reversal. Game uses integer division (floor).
    private fun lowHpBasedDamage(currentHp: Int, maxHp: Int): String {
        if (maxHp <= 0) return "20"
        val frac = currentHp * 48 / maxHp
        return when {
            frac <= 1  -> "200"
            frac <= 4  -> "150"
            frac <= 9  -> "100"
            frac <= 16 -> "80"
            frac <= 32 -> "40"
            else       -> "20"
        }
    }

    // ── Utils.calculateHighHPBasedDamage ─────────────────────────────────────────
    // For Water Spout & Eruption. Base power = 150.
    private fun highHpBasedDamage(currentHp: Int, maxHp: Int): String {
        if (maxHp <= 0) return "1"
        val power = maxOf((150.0 * currentHp / maxHp + 0.5).toInt(), 1)
        return power.toString()
    }

    // ── Utils.calculateWeightBasedDamage ─────────────────────────────────────────
    // For Low Kick. Uses a static weight table (source: PokemonData.lua weight fields).
    // Returns null for unknown species or unknown weight (ghost before Silph Scope, etc.).
    private fun lowKickPower(speciesId: Int): String? {
        if (speciesId <= 0 || speciesId >= LOW_KICK_POWER.size) return null
        val p = LOW_KICK_POWER[speciesId]
        return if (p > 0) p.toString() else null
    }

    // ── Utils.calculateFriendshipBasedDamage ─────────────────────────────────────
    // For Return & Frustration. Only reveals value when computed power ≥ 100,
    // matching Lua tracker behavior to avoid exposing exact friendship.
    private fun friendshipBasedDamage(friendship: Int, inverse: Boolean): String? {
        val f = if (inverse) 255 - friendship else friendship
        val power = maxOf((f / 2.5).toInt(), 1)
        return if (power >= 100) power.toString() else null
    }

    // ── Hidden Power power value (30–70) ─────────────────────────────────────────
    // Formula from Utils.calcHiddenPowerTypeAndPower (bit 1 of each IV).
    private fun hiddenPowerValue(
        ivHp: Int, ivAtk: Int, ivDef: Int,
        ivSpe: Int, ivSpA: Int, ivSpD: Int,
    ): String {
        val powerBits = (ivHp  shr 1 and 1)      +
                        (ivAtk shr 1 and 1) * 2   +
                        (ivDef shr 1 and 1) * 4   +
                        (ivSpe shr 1 and 1) * 8   +
                        (ivSpA shr 1 and 1) * 16  +
                        (ivSpD shr 1 and 1) * 32
        return (powerBits * 40 / 63 + 30).toString()
    }

    // ── Utils.calculateWeatherBall ────────────────────────────────────────────────
    // Base 50, doubled when weather is active.
    private fun weatherBallPower(weather: Weather): String =
        if (weather == Weather.NONE) "50" else "100"

    // ── Low Kick / Grass Knot power table ─────────────────────────────────────────
    // Indexed by Gen III internal species ID (1-based).
    // Source: PokemonData.lua weight field → brackets per Utils.calculateWeightBasedDamage:
    //   weight < 10  → 20 | < 25 → 40 | < 50 → 60 | < 100 → 80 | < 200 → 100 | ≥ 200 → 120
    //   weight == 0  → 0 (unknown, e.g. unidentified ghost)
    // Internal IDs 252–276 are ROM placeholder entries ("none"); Hoenn Pokémon start at 277.
    private val LOW_KICK_POWER = intArrayOf(
        0,   // 0: unused
        // 1–10: Bulbasaur…Caterpie
        20, 40, 80, 20, 40, 80, 20, 40, 80, 20,
        // 11–20: Metapod…Raticate
        20, 60, 20, 40, 60, 20, 60, 60, 20, 40,
        // 21–30: Spearow…Nidorina
        20, 60, 20, 80, 20, 60, 40, 60, 20, 40,
        // 31–40: Nidoqueen…Wigglytuff
        80, 20, 40, 80, 20, 60, 20, 40, 20, 40,
        // 41–50: Zubat…Diglett
        20, 80, 20, 20, 40, 20, 60, 60, 40, 20,
        // 51–60: Dugtrio…Poliwag
        60, 20, 60, 40, 80, 60, 60, 40, 100, 40,
        // 61–70: Poliwhirl…Weepinbell
        40, 80, 40, 80, 60, 40, 80, 100, 20, 20,
        // 71–80: Victreebel…Slowbro
        40, 60, 80, 40, 100, 120, 60, 80, 60, 80,
        // 81–90: Magnemite…Shellder
        20, 80, 40, 60, 80, 80, 100, 60, 60, 20,
        // 91–100: Cloyster…Voltorb
        100, 20, 20, 60, 120, 60, 80, 20, 80, 40,
        // 101–110: Electrode…Koffing
        80, 20, 100, 20, 60, 60, 80, 80, 20, 20,
        // 111–120: Rhyhorn…Staryu
        100, 100, 60, 60, 80, 20, 60, 40, 60, 60,
        // 121–130: Starmie…Gyarados
        80, 80, 80, 60, 60, 60, 80, 80, 40, 120,
        // 131–140: Lapras…Kabuto
        120, 20, 20, 60, 40, 60, 60, 20, 60, 40,
        // 141–151: Kabutops…Mew
        60, 80, 120, 80, 80, 80, 20, 40, 120, 100, 20,
        // 152–160: Chikorita…Feraligatr
        20, 40, 100, 20, 40, 80, 20, 60, 80,
        // 161–170: Sentret…Chinchou
        20, 60, 40, 60, 40, 60, 20, 60, 80, 40,
        // 171–180: Lanturn…Flaaffy
        40, 20, 20, 20, 20, 20, 20, 40, 20, 40,
        // 181–190: Ampharos…Aipom
        80, 20, 20, 60, 60, 60, 20, 20, 20, 40,
        // 191–200: Sunkern…Misdreavus
        20, 20, 60, 20, 80, 60, 60, 20, 80, 20,
        // 201–210: Unown…Granbull
        20, 60, 60, 20, 100, 40, 80, 120, 20, 60,
        // 211–220: Qwilfish…Swinub
        20, 100, 40, 80, 60, 20, 100, 60, 80, 20,
        // 221–230: Piloswine…Kingdra
        80, 20, 40, 60, 40, 120, 80, 40, 60, 100,
        // 231–240: Phanpy…Magby
        60, 100, 60, 80, 80, 40, 60, 20, 40, 40,
        // 241–251: Miltank…Celebi
        80, 60, 100, 100, 100, 80, 100, 120, 120, 100, 20,
        // 252–276: internal ROM placeholders (no real Pokémon)
        0, 0, 0, 0, 0,  0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,  0, 0, 0, 0, 0,
        0, 0, 0, 0, 0,
        // 277–289: Treecko…Linoone
        20, 40, 80, 20, 40, 80, 20, 60, 80, 40, 60, 40, 60,
        // 290–300: Wurmple…Shiftry
        20, 40, 60, 40, 60, 20, 60, 80, 20, 60, 80,
        // 301–313: Nincada…Wailmer
        20, 40, 20, 20, 40, 20, 60, 20, 20, 60, 20, 20, 100,
        // 314–325: Wailord…Luvdisc
        120, 40, 60, 40, 40, 100, 80, 80, 40, 20, 40, 20,
        // 326–336: Corphish…Hariyama
        40, 60, 20, 100, 40, 80, 40, 40, 80, 80, 120,
        // 337–349: Electrike…Solrock
        40, 60, 40, 120, 60, 80, 100, 80, 80, 40, 120, 100, 100,
        // 350–362: Azurill…Dusclops
        20, 60, 80, 20, 20, 40, 40, 60, 20, 40, 40, 40, 60,
        // 363–376: Roselia…Absol
        20, 40, 60, 100, 40, 80, 80, 40, 60, 80, 80, 60, 40, 60,
        // 377–384: Shuppet…Aggron
        20, 40, 80, 60, 40, 80, 100, 120,
        // 385–398: Castform…Beldum
        20, 40, 40, 40, 80, 40, 80, 20, 40, 60, 60, 100, 100, 80,
        // 399–406: Metang…Rayquaza
        120, 120, 120, 100, 120, 120, 120, 120,
        // 407–411: Latias…Chimecho
        60, 80, 20, 80, 20,
    )
}
