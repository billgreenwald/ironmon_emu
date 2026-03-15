package hh.game.mgba_android.tracker.tables

object AbilityTable {

    data class AbilityInfo(val name: String, val desc: String)

    private val ABILITIES = arrayOf<AbilityInfo?>(
        null,                                                                       // 0 (no ability)
        AbilityInfo("Stench",         "May cause the foe to flinch."),             // 1
        AbilityInfo("Drizzle",        "Summons rain in battle."),                  // 2
        AbilityInfo("Speed Boost",    "Gradually boosts Speed each turn."),        // 3
        AbilityInfo("Battle Armor",   "Hard armor blocks critical hits."),         // 4
        AbilityInfo("Sturdy",         "Cannot be knocked out in one hit."),        // 5
        AbilityInfo("Damp",           "Prevents Selfdestruct and Explosion."),     // 6
        AbilityInfo("Limber",         "Prevents paralysis."),                      // 7
        AbilityInfo("Sand Veil",      "Boosts evasion in sandstorm."),             // 8
        AbilityInfo("Static",         "May paralyze on contact."),                 // 9
        AbilityInfo("Volt Absorb",    "Restores HP if hit by Electric moves."),    // 10
        AbilityInfo("Water Absorb",   "Restores HP if hit by Water moves."),       // 11
        AbilityInfo("Oblivious",      "Prevents attraction and Taunt."),           // 12
        AbilityInfo("Cloud Nine",     "Negates weather effects."),                 // 13
        AbilityInfo("Compound Eyes",  "Boosts accuracy by 30%."),                  // 14
        AbilityInfo("Insomnia",       "Prevents sleep."),                          // 15
        AbilityInfo("Color Change",   "Changes type to match move received."),     // 16
        AbilityInfo("Immunity",       "Prevents poisoning."),                      // 17
        AbilityInfo("Flash Fire",     "Powers up Fire moves if hit by fire."),     // 18
        AbilityInfo("Shield Dust",    "Blocks added effects of moves."),           // 19
        AbilityInfo("Own Tempo",      "Prevents confusion."),                      // 20
        AbilityInfo("Suction Cups",   "Cannot be forced to switch out."),          // 21
        AbilityInfo("Intimidate",     "Lowers foe's Attack on entry."),            // 22
        AbilityInfo("Shadow Tag",     "Prevents the foe from fleeing."),           // 23
        AbilityInfo("Rough Skin",     "Damages foe on contact."),                  // 24
        AbilityInfo("Wonder Guard",   "Only supereffective moves hit."),           // 25
        AbilityInfo("Levitate",       "Immune to Ground-type moves."),             // 26
        AbilityInfo("Effect Spore",   "May poison, paralyze, or sleep on contact."), // 27
        AbilityInfo("Synchronize",    "Passes status condition to the foe."),      // 28
        AbilityInfo("Clear Body",     "Prevents stat reduction by foes."),         // 29
        AbilityInfo("Natural Cure",   "Cures status when switched out."),          // 30
        AbilityInfo("Lightningrod",   "Draws Electric moves; raises Sp. Atk."),    // 31
        AbilityInfo("Serene Grace",   "Boosts added effect chances."),             // 32
        AbilityInfo("Swift Swim",     "Doubles Speed in rain."),                   // 33
        AbilityInfo("Chlorophyll",    "Doubles Speed in sunshine."),               // 34
        AbilityInfo("Illuminate",     "Raises wild encounter rate."),              // 35
        AbilityInfo("Trace",          "Copies the foe's ability."),                // 36
        AbilityInfo("Huge Power",     "Doubles Attack stat."),                     // 37
        AbilityInfo("Poison Point",   "May poison on contact."),                   // 38
        AbilityInfo("Inner Focus",    "Prevents flinching."),                      // 39
        AbilityInfo("Magma Armor",    "Prevents freezing."),                       // 40
        AbilityInfo("Water Veil",     "Prevents burns."),                          // 41
        AbilityInfo("Magnet Pull",    "Traps Steel-type Pokémon."),                // 42
        AbilityInfo("Soundproof",     "Immune to sound-based moves."),             // 43
        AbilityInfo("Rain Dish",      "Restores HP slowly in rain."),              // 44
        AbilityInfo("Sand Stream",    "Summons sandstorm in battle."),             // 45
        AbilityInfo("Pressure",       "Foe moves use double PP."),                 // 46
        AbilityInfo("Thick Fat",      "Reduces Fire and Ice damage."),             // 47
        AbilityInfo("Early Bird",     "Wakes from sleep quickly."),                // 48
        AbilityInfo("Flame Body",     "May burn on contact."),                     // 49
        AbilityInfo("Run Away",       "Guarantees escape from wild battles."),     // 50
        AbilityInfo("Keen Eye",       "Accuracy cannot be reduced."),              // 51
        AbilityInfo("Hyper Cutter",   "Attack stat cannot be lowered by foes."),   // 52
        AbilityInfo("Pickup",         "May pick up items after battle."),           // 53
        AbilityInfo("Truant",         "Can only attack every other turn."),         // 54
        AbilityInfo("Hustle",         "Boosts Attack but lowers accuracy."),        // 55
        AbilityInfo("Cute Charm",     "May cause attraction on contact."),          // 56
        AbilityInfo("Plus",           "Boosts Sp. Atk with a partner's Minus."),    // 57
        AbilityInfo("Minus",          "Boosts Sp. Atk with a partner's Plus."),     // 58
        AbilityInfo("Forecast",       "Changes type with the weather."),            // 59
        AbilityInfo("Sticky Hold",    "Item cannot be taken by foe."),              // 60
        AbilityInfo("Shed Skin",      "May heal status each turn."),                // 61
        AbilityInfo("Guts",           "Boosts Attack when statused."),              // 62
        AbilityInfo("Marvel Scale",   "Boosts Defense when statused."),             // 63
        AbilityInfo("Liquid Ooze",    "Damages foes using draining moves."),        // 64
        AbilityInfo("Overgrow",       "Powers up Grass moves at low HP."),          // 65
        AbilityInfo("Blaze",          "Powers up Fire moves at low HP."),           // 66
        AbilityInfo("Torrent",        "Powers up Water moves at low HP."),          // 67
        AbilityInfo("Swarm",          "Powers up Bug moves at low HP."),            // 68
        AbilityInfo("Rock Head",      "No recoil damage from moves."),              // 69
        AbilityInfo("Drought",        "Summons intense sunlight in battle."),       // 70
        AbilityInfo("Arena Trap",     "Traps non-Flying foes."),                    // 71
        AbilityInfo("Vital Spirit",   "Prevents sleep."),                           // 72
        AbilityInfo("White Smoke",    "Prevents stat reduction by foes."),          // 73
        AbilityInfo("Pure Power",     "Doubles Attack stat."),                      // 74
        AbilityInfo("Shell Armor",    "Hard shell blocks critical hits."),          // 75
        AbilityInfo("Cacophony",      "Immune to sound-based moves."),              // 76 (unused)
        AbilityInfo("Air Lock",       "Negates weather effects."),                  // 77
    )

    fun get(abilityId: Int): AbilityInfo =
        if (abilityId in 1 until ABILITIES.size) ABILITIES[abilityId] ?: AbilityInfo("???", "")
        else AbilityInfo("None", "")

    fun name(abilityId: Int): String = get(abilityId).name
}
