package hh.game.mgba_android.tracker.tables

/**
 * Static level-up evolution table matching Lua tracker PokemonData.lua.
 * Only includes level-based evolutions (numeric evolution field in the Lua table).
 * Stone / trade / friendship / beauty evolutions use proxy levels from the Lua tracker.
 *
 * Key   = Gen III internal species ID (== national Dex for Gen I/II; differs for Gen III)
 * Value = level at which this form evolves (0 = no level evolution)
 *
 * Gen III internal IDs sourced from PokemonData.lua idInternalToNat (inverted).
 */
object EvolutionLevel {

    private val TABLE = mapOf(
        // ── Gen I ────────────────────────────────────────────────────────────
        1   to 16,  2   to 32,  // Bulbasaur → Ivysaur → Venusaur
        4   to 16,  5   to 36,  // Charmander → Charmeleon → Charizard
        7   to 16,  8   to 36,  // Squirtle → Wartortle → Blastoise
        10  to 7,   11  to 10,  // Caterpie → Metapod → Butterfree
        13  to 7,   14  to 10,  // Weedle → Kakuna → Beedrill
        16  to 18,  17  to 36,  // Pidgey → Pidgeotto → Pidgeot
        19  to 20,              // Rattata → Raticate
        21  to 20,              // Spearow → Fearow
        23  to 22,              // Ekans → Arbok
        27  to 22,              // Sandshrew → Sandslash
        29  to 16,              // Nidoran♀ → Nidorina
        32  to 16,              // Nidoran♂ → Nidorino
        41  to 22,              // Zubat → Golbat
        43  to 21,              // Oddish → Gloom
        46  to 24,              // Paras → Parasect
        48  to 31,              // Venonat → Venomoth
        50  to 26,              // Diglett → Dugtrio
        52  to 28,              // Meowth → Persian
        54  to 33,              // Psyduck → Golduck
        56  to 28,              // Mankey → Primeape
        60  to 25,              // Poliwag → Poliwhirl
        63  to 16,              // Abra → Kadabra
        64  to 37,              // Kadabra → Alakazam (trade proxy)
        66  to 28,              // Machop → Machoke
        67  to 37,              // Machoke → Machamp (trade proxy)
        69  to 21,              // Bellsprout → Weepinbell
        72  to 30,              // Tentacool → Tentacruel
        74  to 25,              // Geodude → Graveler
        75  to 37,              // Graveler → Golem (trade proxy)
        77  to 40,              // Ponyta → Rapidash
        79  to 37,              // Slowpoke → Slowbro
        81  to 30,              // Magnemite → Magneton
        84  to 31,              // Doduo → Dodrio
        86  to 34,              // Seel → Dewgong
        88  to 38,              // Grimer → Muk
        92  to 25,              // Gastly → Haunter
        93  to 37,              // Haunter → Gengar (trade proxy)
        95  to 30,              // Onix → Steelix (trade proxy)
        96  to 26,              // Drowzee → Hypno
        98  to 28,              // Krabby → Kingler
        100 to 30,              // Voltorb → Electrode
        104 to 28,              // Cubone → Marowak
        109 to 35,              // Koffing → Weezing
        111 to 42,              // Rhyhorn → Rhydon
        116 to 32,              // Horsea → Seadra
        117 to 40,              // Seadra → Kingdra (trade proxy)
        118 to 33,              // Goldeen → Seaking
        123 to 30,              // Scyther → Scizor (trade proxy)
        129 to 20,              // Magikarp → Gyarados
        137 to 30,              // Porygon → Porygon2 (trade proxy)
        138 to 40,              // Omanyte → Omastar
        140 to 40,              // Kabuto → Kabutops
        147 to 30,  148 to 55,  // Dratini → Dragonair → Dragonite
        // ── Gen II ───────────────────────────────────────────────────────────
        152 to 16,  153 to 32,  // Chikorita → Bayleef → Meganium
        155 to 14,  156 to 36,  // Cyndaquil → Quilava → Typhlosion
        158 to 18,  159 to 30,  // Totodile → Croconaw → Feraligatr
        161 to 15,              // Sentret → Furret
        163 to 20,              // Hoothoot → Noctowl
        165 to 18,              // Ledyba → Ledian
        167 to 22,              // Spinarak → Ariados
        170 to 27,              // Chinchou → Lanturn
        177 to 25,              // Natu → Xatu
        179 to 15,  180 to 30,  // Mareep → Flaaffy → Ampharos
        183 to 18,              // Marill → Azumarill
        187 to 18,  188 to 27,  // Hoppip → Skiploom → Jumpluff
        194 to 20,              // Wooper → Quagsire
        204 to 31,              // Pineco → Forretress
        209 to 23,              // Snubbull → Granbull
        216 to 30,              // Teddiursa → Ursaring
        218 to 38,              // Slugma → Magcargo
        220 to 33,              // Swinub → Piloswine
        223 to 25,              // Remoraid → Octillery
        228 to 24,              // Houndour → Houndoom
        231 to 25,              // Phanpy → Donphan
        236 to 20,              // Tyrogue → (personality-based)
        238 to 30,              // Smoochum → Jynx
        239 to 30,              // Elekid → Electabuzz
        240 to 30,              // Magby → Magmar
        246 to 30,  247 to 55,  // Larvitar → Pupitar → Tyranitar
        // ── Gen III (keys = internal ROM species IDs, NOT national Dex) ──────
        277 to 16,  278 to 36,  // Treecko → Grovyle → Sceptile
        280 to 16,  281 to 36,  // Torchic → Combusken → Blaziken
        283 to 16,  284 to 36,  // Mudkip → Marshtomp → Swampert
        286 to 18,              // Poochyena → Mightyena
        288 to 20,              // Zigzagoon → Linoone
        290 to 7,               // Wurmple → Silcoon/Cascoon (personality)
        291 to 10,              // Silcoon → Beautifly
        293 to 10,              // Cascoon → Dustox
        295 to 14,              // Lotad → Lombre
        298 to 14,              // Seedot → Nuzleaf
        304 to 22,              // Taillow → Swellow
        309 to 25,              // Wingull → Pelipper
        392 to 16,  393 to 30,  // Ralts → Kirlia → Gardevoir
        311 to 22,              // Surskit → Masquerain
        306 to 23,              // Shroomish → Breloom
        364 to 18,  365 to 36,  // Slakoth → Vigoroth → Slaking
        301 to 20,              // Nincada → Ninjask
        370 to 20,  371 to 40,  // Whismur → Loudred → Exploud
        335 to 24,              // Makuhita → Hariyama
        382 to 32,  383 to 42,  // Aron → Lairon → Aggron
        356 to 37,              // Meditite → Medicham
        337 to 26,              // Electrike → Manectric
        367 to 26,              // Gulpin → Swalot
        330 to 30,              // Carvanha → Sharpedo
        313 to 40,              // Wailmer → Wailord
        339 to 33,              // Numel → Camerupt
        351 to 32,              // Spoink → Grumpig
        332 to 35,  333 to 45,  // Trapinch → Vibrava → Flygon
        344 to 32,              // Cacnea → Cacturne
        358 to 35,              // Swablu → Altaria
        323 to 30,              // Barboach → Whiscash
        326 to 30,              // Corphish → Crawdaunt
        318 to 36,              // Baltoy → Claydol
        388 to 40,              // Lileep → Cradily
        390 to 40,              // Anorith → Armaldo
        328 to 35,              // Feebas (beauty proxy)
        377 to 37,              // Shuppet → Banette
        361 to 37,              // Duskull → Dusclops
        360 to 15,              // Wynaut → Wobbuffet
        346 to 42,              // Snorunt → Glalie
        341 to 32,  342 to 44,  // Spheal → Sealeo → Walrein
        381 to 30,  325 to 50,  // Bagon → Shelgon → Salamence
        398 to 20,  399 to 45,  // Beldum → Metang → Metagross
    )

    /** Returns the level at which [speciesId] evolves, or 0 if no level-based evolution. */
    fun get(speciesId: Int): Int = TABLE[speciesId] ?: 0
}
