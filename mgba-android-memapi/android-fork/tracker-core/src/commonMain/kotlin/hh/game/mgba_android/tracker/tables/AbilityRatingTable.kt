package hh.game.mgba_android.tracker.tables

/**
 * Ability ratings ported from GachaMonRatingSystem.json "Abilities" section.
 * Index = ability ID (1–77). Source of truth: Ironmon-Tracker lua scripts.
 */
object AbilityRatingTable {
    // Index 0 unused; indices 1–77 match ability IDs
    private val RATINGS = intArrayOf(
         0,   // 0 (none)
         1,   // 1  Stench
         1,   // 2  Drizzle
         5,   // 3  Speed Boost
        10,   // 4  Battle Armor
         1,   // 5  Sturdy
         1,   // 6  Damp
         3,   // 7  Limber
         2,   // 8  Sand Veil
         2,   // 9  Static
         5,   // 10 Volt Absorb
         5,   // 11 Water Absorb
         1,   // 12 Oblivious
         3,   // 13 Cloud Nine
         5,   // 14 Compound Eyes
         3,   // 15 Insomnia
         0,   // 16 Color Change
         3,   // 17 Immunity
         5,   // 18 Flash Fire
         8,   // 19 Shield Dust
         5,   // 20 Own Tempo
         1,   // 21 Suction Cups
         4,   // 22 Intimidate
         0,   // 23 Shadow Tag
         2,   // 24 Rough Skin
         1,   // 25 Wonder Guard
         5,   // 26 Levitate
         1,   // 27 Effect Spore
         1,   // 28 Synchronize
         5,   // 29 Clear Body
         0,   // 30 Natural Cure
         0,   // 31 Lightningrod
         4,   // 32 Serene Grace
         1,   // 33 Swift Swim
         1,   // 34 Chlorophyll
         0,   // 35 Illuminate
         3,   // 36 Trace
        15,   // 37 Huge Power
         2,   // 38 Poison Point
         3,   // 39 Inner Focus
         2,   // 40 Magma Armor
         3,   // 41 Water Veil
         0,   // 42 Magnet Pull
         3,   // 43 Soundproof
         1,   // 44 Rain Dish
         0,   // 45 Sand Stream
         2,   // 46 Pressure
         5,   // 47 Thick Fat
         2,   // 48 Early Bird
         1,   // 49 Flame Body
         2,   // 50 Run Away
         2,   // 51 Keen Eye
         2,   // 52 Hyper Cutter
         8,   // 53 Pickup
       -15,   // 54 Truant
         0,   // 55 Hustle
         1,   // 56 Cute Charm
         0,   // 57 Plus
         0,   // 58 Minus
         0,   // 59 Forecast
         1,   // 60 Sticky Hold
         5,   // 61 Shed Skin
         2,   // 62 Guts
         2,   // 63 Marvel Scale
         2,   // 64 Liquid Ooze
         1,   // 65 Overgrow
         1,   // 66 Blaze
         1,   // 67 Torrent
         1,   // 68 Swarm
         2,   // 69 Rock Head
         1,   // 70 Drought
         0,   // 71 Arena Trap
         3,   // 72 Vital Spirit
         5,   // 73 White Smoke
        15,   // 74 Pure Power
        10,   // 75 Shell Armor
         0,   // 76 Cacophony (unused)
         3,   // 77 Air Lock
    )

    fun get(abilityId: Int): Int =
        if (abilityId in RATINGS.indices) RATINGS[abilityId] else 0
}
