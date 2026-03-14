package hh.game.mgba_android.tracker.tables

/**
 * Static level-up evolution table matching Lua tracker PokemonData.lua.
 * Only includes level-based evolutions (numeric evolution field in the Lua table).
 * Stone / trade / friendship / beauty evolutions return 0.
 *
 * Key   = speciesId (NatDex / Gen III internal ID)
 * Value = level at which this form evolves (0 = no level evolution)
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
        63  to 16,              // Abra → Kadabra (Kadabra→Alakazam by trade)
        66  to 28,              // Machop → Machoke (Machoke→Machamp by trade)
        69  to 21,              // Bellsprout → Weepinbell
        72  to 30,              // Tentacool → Tentacruel
        74  to 25,              // Geodude → Graveler (Graveler→Golem by trade)
        77  to 40,              // Ponyta → Rapidash
        79  to 37,              // Slowpoke → Slowbro
        81  to 30,              // Magnemite → Magneton
        84  to 31,              // Doduo → Dodrio
        86  to 34,              // Seel → Dewgong
        88  to 38,              // Grimer → Muk
        92  to 25,              // Gastly → Haunter (Haunter→Gengar by trade)
        96  to 26,              // Drowzee → Hypno
        98  to 28,              // Krabby → Kingler
        100 to 30,              // Voltorb → Electrode
        104 to 28,              // Cubone → Marowak
        109 to 35,              // Koffing → Weezing
        111 to 42,              // Rhyhorn → Rhydon
        116 to 32,              // Horsea → Seadra
        118 to 33,              // Goldeen → Seaking
        129 to 20,              // Magikarp → Gyarados
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
        236 to 20,              // Tyrogue → Hitmonlee/Hitmonchan/Hitmontop (personality)
        238 to 30,              // Smoochum → Jynx
        239 to 30,              // Elekid → Electabuzz
        240 to 30,              // Magby → Magmar
        246 to 30,  247 to 55,  // Larvitar → Pupitar → Tyranitar
        // ── Gen III ──────────────────────────────────────────────────────────
        252 to 16,  253 to 36,  // Treecko → Grovyle → Sceptile
        255 to 16,  256 to 36,  // Torchic → Combusken → Blaziken
        258 to 16,  259 to 36,  // Mudkip → Marshtomp → Swampert
        261 to 18,              // Poochyena → Mightyena
        263 to 20,              // Zigzagoon → Linoone
        265 to 7,               // Wurmple → Silcoon/Cascoon (personality)
        266 to 10,              // Silcoon → Beautifly
        268 to 10,              // Cascoon → Dustox
        270 to 14,              // Lotad → Lombre
        273 to 14,              // Seedot → Nuzleaf
        276 to 22,              // Taillow → Swellow
        278 to 25,              // Wingull → Pelipper
        280 to 20,  281 to 30,  // Ralts → Kirlia → Gardevoir
        283 to 22,              // Surskit → Masquerain
        285 to 23,              // Shroomish → Breloom
        287 to 18,  288 to 36,  // Slakoth → Vigoroth → Slaking
        290 to 20,              // Nincada → Ninjask
        293 to 20,  294 to 40,  // Whismur → Loudred → Exploud
        296 to 24,              // Makuhita → Hariyama
        304 to 32,  305 to 42,  // Aron → Lairon → Aggron
        307 to 37,              // Meditite → Medicham
        309 to 26,              // Electrike → Manectric
        316 to 26,              // Gulpin → Swalot
        318 to 30,              // Carvanha → Sharpedo
        320 to 40,              // Wailmer → Wailord
        322 to 33,              // Numel → Camerupt
        325 to 32,              // Spoink → Grumpig
        328 to 35,  329 to 45,  // Trapinch → Vibrava → Flygon
        331 to 32,              // Cacnea → Cacturne
        333 to 35,              // Swablu → Altaria
        339 to 30,              // Barboach → Whiscash
        341 to 30,              // Corphish → Crawdaunt
        343 to 36,              // Baltoy → Claydol
        345 to 40,              // Lileep → Cradily
        347 to 40,              // Anorith → Armaldo
        353 to 37,              // Shuppet → Banette
        355 to 37,              // Duskull → Dusclops
        360 to 15,              // Wynaut → Wobbuffet
        361 to 42,              // Snorunt → Glalie
        363 to 32,  364 to 44,  // Spheal → Sealeo → Walrein
        369 to 30,  370 to 50,  // Bagon → Shelgon → Salamence
        371 to 20,  372 to 45,  // Beldum → Metang → Metagross
    )

    /** Returns the level at which [speciesId] evolves, or 0 if no level-based evolution. */
    fun get(speciesId: Int): Int = TABLE[speciesId] ?: 0
}
